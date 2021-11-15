/*
 * Copyright 2019 GridGain Systems, Inc. and Contributors.
 *
 * Licensed under the GridGain Community Edition License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.gridgain.com/products/software/community-edition/gridgain-community-edition-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ignite.internal.processors.cache.persistence.pagemem;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import org.apache.ignite.IgniteLogger;
import org.apache.ignite.internal.processors.cache.persistence.CheckpointLockStateChecker;
import org.apache.ignite.internal.processors.cache.persistence.checkpoint.CheckpointProgress;
import org.apache.ignite.internal.util.GridConcurrentHashSet;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.lang.IgniteOutClosure;

/**
 * Throttles threads that generate dirty pages during ongoing checkpoint.
 * Designed to avoid zero dropdowns that can happen if checkpoint buffer is overflowed.
 * When a page is in checkpoint and Checkpoint Buffer is filled over 2/3, uses exponentially
 * growing sleep time to throttle.
 * Otherwise, uses average checkpoint write speed and moment speed of marking pages as dirty.<br>
 *
 * See also: <a href="https://github.com/apache/ignite/tree/master/modules/core/src/main/java/org/apache/ignite/internal/processors/cache/persistence/pagemem#speed-based-throttling">Speed-based throttling description</a>.
 */
public class PagesWriteSpeedBasedThrottle implements PagesWriteThrottlePolicy {
    /**
     * Throttling 'duration' used to signal that no trottling is needed, and no certain side-effects are allowed
     * (like stats collection).
     */
    static final long NO_THROTTLING_MARKER = Long.MIN_VALUE;

    /** Page memory. */
    private final PageMemoryImpl pageMemory;

    /** Checkpoint progress provider. */
    private final IgniteOutClosure<CheckpointProgress> cpProgress;

    /** Threads set. Contains threads which are currently parked because of throttling. */
    private final GridConcurrentHashSet<Thread> parkedThreads = new GridConcurrentHashSet<>();

    /**
     * Used for calculating speed of marking pages dirty.
     * Value from past 750-1000 millis only.
     * {@link IntervalBasedMeasurement#getSpeedOpsPerSec(long)} returns pages marked/second.
     * {@link IntervalBasedMeasurement#getAverage()} returns average throttle time.
     * */
    private final IntervalBasedMeasurement speedMarkAndAvgParkTime = new IntervalBasedMeasurement(250, 3);

    /** Checkpoint lock state provider. */
    private final CheckpointLockStateChecker cpLockStateChecker;

    /** Logger. */
    private final IgniteLogger log;

    /** Previous warning time, nanos. */
    private final AtomicLong prevWarnTime = new AtomicLong();

    /** Warning min delay nanoseconds. */
    private static final long WARN_MIN_DELAY_NS = TimeUnit.SECONDS.toNanos(10);

    /** Warning threshold: minimal level of pressure that causes warning messages to log. */
    static final double WARN_THRESHOLD = 0.2;

    /** Checkpoint buffer protection logic. */
    private final CheckpointBufferProtectionThrottle cpBufferProtector = new CheckpointBufferProtectionThrottle();

    /** Clean pages protection logic. */
    private final SpeedBasedCleanPagesProtectionThrottle cleanPagesProtector;

    /**
     * @param pageMemory Page memory.
     * @param cpProgress Database manager.
     * @param stateChecker Checkpoint lock state provider.
     * @param log Logger.
     */
    public PagesWriteSpeedBasedThrottle(
            PageMemoryImpl pageMemory,
            IgniteOutClosure<CheckpointProgress> cpProgress,
            CheckpointLockStateChecker stateChecker,
            IgniteLogger log
    ) {
        this.pageMemory = pageMemory;
        this.cpProgress = cpProgress;
        cpLockStateChecker = stateChecker;
        this.log = log;

        cleanPagesProtector = new SpeedBasedCleanPagesProtectionThrottle(pageMemory, cpProgress, speedMarkAndAvgParkTime);
    }

    /** {@inheritDoc} */
    @Override public void onMarkDirty(boolean isPageInCheckpoint) {
        assert cpLockStateChecker.checkpointLockIsHeldByThread();

        final long curNanoTime = System.nanoTime();
        final long throttleParkTimeNs = computeThrottlingParkTime(isPageInCheckpoint, curNanoTime);

        if (throttleParkTimeNs == NO_THROTTLING_MARKER)
            return;
        else if (throttleParkTimeNs > 0) {
            recurrentLogIfNeeded();
            doPark(throttleParkTimeNs);
        }

        pageMemory.metrics().addThrottlingTime(U.nanosToMillis(System.nanoTime() - curNanoTime));
        speedMarkAndAvgParkTime.addMeasurementForAverageCalculation(throttleParkTimeNs);
    }

    /***/
    private long computeThrottlingParkTime(boolean isPageInCheckpoint, long curNanoTime) {
        if (shouldThrottleToProtectCPBuffer(isPageInCheckpoint))
            return computeCPBufferProtectionParkTime();
        else {
            if (isPageInCheckpoint) {
                cpBufferProtector.resetExponentialBackoffCounter();
            }
            return computeCleanPagesProtectionParkTime(curNanoTime);
        }
    }

    /***/
    private boolean shouldThrottleToProtectCPBuffer(boolean isPageInCheckpoint) {
        return isPageInCheckpoint && shouldThrottle();
    }

    /***/
    private long computeCPBufferProtectionParkTime() {
        return cpBufferProtector.computeProtectionParkTime();
    }

    /***/
    private long computeCleanPagesProtectionParkTime(long curNanoTime) {
        return cleanPagesProtector.computeProtectionParkTime(curNanoTime);
    }

    /**
     * Disables the current thread for thread scheduling purposes. May be overriden by subclasses for tests
     *
     * @param throttleParkTimeNs the maximum number of nanoseconds to wait
     */
    protected void doPark(long throttleParkTimeNs) {
        if (throttleParkTimeNs > LOGGING_THRESHOLD) {
            U.warn(log, "Parking thread=" + Thread.currentThread().getName()
                + " for timeout(ms)=" + (throttleParkTimeNs / 1_000_000));
        }

        parkedThreads.add(Thread.currentThread());

        try {
            LockSupport.parkNanos(throttleParkTimeNs);
        }
        finally {
            parkedThreads.remove(Thread.currentThread());
        }
    }

    /**
     * @return number of written pages.
     */
    private int cpWrittenPages() {
        AtomicInteger writtenPagesCntr = cpProgress.apply().writtenPagesCounter();

        return writtenPagesCntr == null ? 0 : writtenPagesCntr.get();
    }

    /**
     * Prints warning to log if throttling is occurred and requires markable amount of time.
     */
    private void recurrentLogIfNeeded() {
        long prevWarningNs = prevWarnTime.get();
        long curNs = System.nanoTime();

        if (prevWarningNs != 0 && (curNs - prevWarningNs) <= WARN_MIN_DELAY_NS)
            return;

        double weight = throttleWeight();
        if (weight <= WARN_THRESHOLD)
            return;

        if (prevWarnTime.compareAndSet(prevWarningNs, curNs) && log.isInfoEnabled()) {
            String msg = String.format("Throttling is applied to page modifications " +
                    "[percentOfPartTime=%.2f, markDirty=%d pages/sec, checkpointWrite=%d pages/sec, " +
                    "estIdealMarkDirty=%d pages/sec, curDirty=%.2f, maxDirty=%.2f, avgParkTime=%d ns, " +
                    "pages: (total=%d, evicted=%d, written=%d, synced=%d, cpBufUsed=%d, cpBufTotal=%d)]",
                weight, getMarkDirtySpeed(), getCpWriteSpeed(),
                getLastEstimatedSpeedForMarkAll(), getCurrDirtyRatio(), getTargetDirtyRatio(), throttleParkTime(),
                cleanPagesProtector.cpTotalPages(), cleanPagesProtector.cpEvictedPages(), cpWrittenPages(),
                cleanPagesProtector.cpSyncedPages(),
                pageMemory.checkpointBufferPagesCount(), pageMemory.checkpointBufferPagesSize());

            log.info(msg);
        }
    }

    /**
     * @param dirtyPagesRatio actual percent of dirty pages.
     * @param fullyCompletedPages written & fsynced pages count.
     * @param cpTotalPages total checkpoint scope.
     * @param nThreads number of threads providing data during current checkpoint.
     * @param markDirtySpeed registered mark dirty speed, pages/sec.
     * @param curCpWriteSpeed average checkpoint write speed, pages/sec.
     * @return time in nanoseconds to part or 0 if throttling is not required.
     */
    long getCleanPagesProtectionParkTime(
            double dirtyPagesRatio,
            long fullyCompletedPages,
            int cpTotalPages,
            int nThreads,
            long markDirtySpeed,
            long curCpWriteSpeed) {
        return cleanPagesProtector.getParkTime(dirtyPagesRatio, fullyCompletedPages, cpTotalPages, nThreads,
                markDirtySpeed, curCpWriteSpeed);
    }

    /** {@inheritDoc} */
    @Override public void onBeginCheckpoint() {
        cleanPagesProtector.reset();
    }

    /** {@inheritDoc} */
    @Override public void onFinishCheckpoint() {
        cpBufferProtector.resetExponentialBackoffCounter();

        cleanPagesProtector.close();
        speedMarkAndAvgParkTime.finishInterval();
        unparkParkedThreads();
    }

    /***/
    private void unparkParkedThreads() {
        parkedThreads.forEach(LockSupport::unpark);
    }

    /**
     * @return Exponential backoff counter.
     */
    public long throttleParkTime() {
        return speedMarkAndAvgParkTime.getAverage();
    }

    /**
     * @return Target (maximum) dirty pages ratio, after which throttling will start.
     */
    public double getTargetDirtyRatio() {
        return cleanPagesProtector.getTargetDirtyRatio();
    }

    /**
     * @return Current dirty pages ratio.
     */
    public double getCurrDirtyRatio() {
        return cleanPagesProtector.getCurrDirtyRatio();
    }

    /**
     * @return  Speed of marking pages dirty. Value from past 750-1000 millis only. Pages/second.
     */
    public long getMarkDirtySpeed() {
        return speedMarkAndAvgParkTime.getSpeedOpsPerSec(System.nanoTime());
    }

    /**
     * @return Speed average checkpoint write speed. Current and 3 past checkpoints used. Pages/second.
     */
    public long getCpWriteSpeed() {
        return cleanPagesProtector.getCpWriteSpeed();
    }

    /**
     * @return last estimated speed for marking all clear pages as dirty till the end of checkpoint.
     */
    public long getLastEstimatedSpeedForMarkAll() {
        return cleanPagesProtector.getLastEstimatedSpeedForMarkAll();
    }

    /**
     * Measurement shows how much throttling time is involved into average marking time.
     *
     * @return metric started from 0.0 and showing how much throttling is involved into current marking process.
     */
    public double throttleWeight() {
        long speed = speedMarkAndAvgParkTime.getSpeedOpsPerSec(System.nanoTime());

        if (speed <= 0)
            return 0;

        long timeForOnePage = cleanPagesProtector.calcDelayTime(speed, cleanPagesProtector.threadIdsCount(), 1);

        if (timeForOnePage == 0)
            return 0;

        return 1.0 * throttleParkTime() / timeForOnePage;
    }

    /** {@inheritDoc} */
    @Override public void tryWakeupThrottledThreads() {
        if (!shouldThrottle()) {
            cpBufferProtector.resetExponentialBackoffCounter();

            unparkParkedThreads();
        }
    }

    /** {@inheritDoc} */
    @Override public boolean shouldThrottle() {
        int checkpointBufLimit = (int)(pageMemory.checkpointBufferPagesSize() * CP_BUF_FILL_THRESHOLD);

        return pageMemory.checkpointBufferPagesCount() > checkpointBufLimit;
    }
}
