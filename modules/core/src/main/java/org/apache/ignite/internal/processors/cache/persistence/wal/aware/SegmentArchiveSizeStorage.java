/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.processors.cache.persistence.wal.aware;

import java.util.Map;
import java.util.TreeMap;
import org.apache.ignite.IgniteLogger;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.internal.IgniteInterruptedCheckedException;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.jetbrains.annotations.Nullable;

import static org.apache.ignite.configuration.DataStorageConfiguration.UNLIMITED_WAL_ARCHIVE;

/**
 * Storage WAL archive size.
 * Allows to track the exceeding of the maximum archive size.
 */
class SegmentArchiveSizeStorage {
    /** Logger. */
    private final IgniteLogger log;

    /** Current WAL archive size in bytes. Guarded by {@code this}. */
    private long walArchiveSize;

    /** Flag of interrupt waiting on this object. Guarded by {@code this}. */
    private boolean interrupted;

    /** Minimum size of the WAL archive in bytes. */
    private final long minWalArchiveSize;

    /** Maximum size of the WAL archive in bytes. */
    private final long maxWalArchiveSize;

    /** WAL archive size unlimited. */
    private final boolean walArchiveUnlimited;

    /**
     * Segment sizes. Mapping: segment idx -> size in bytes. Guarded by {@code this}.
     * {@code null} if {@link #walArchiveUnlimited} == {@code true}.
     */
    @Nullable private final Map<Long, Long> segmentSizes;

    /**
     * Segment reservations storage.
     * {@code null} if {@link #walArchiveUnlimited} == {@code true}.
     */
    @Nullable private final SegmentReservationStorage reservationStorage;

    /**
     * Constructor.
     *
     * @param minWalArchiveSize Minimum size of the WAL archive in bytes.
     * @param maxWalArchiveSize Maximum size of the WAL archive in bytes
     *      or {@link DataStorageConfiguration#UNLIMITED_WAL_ARCHIVE}.
     * @param reservationStorage Segment reservations storage.
     */
    public SegmentArchiveSizeStorage(
        IgniteLogger log,
        long minWalArchiveSize,
        long maxWalArchiveSize,
        SegmentReservationStorage reservationStorage
    ) {
        this.log = log;

        this.minWalArchiveSize = minWalArchiveSize;
        this.maxWalArchiveSize = maxWalArchiveSize;

        if (maxWalArchiveSize != UNLIMITED_WAL_ARCHIVE) {
            walArchiveUnlimited = false;

            segmentSizes = new TreeMap<>();
            this.reservationStorage = reservationStorage;
        }
        else {
            walArchiveUnlimited = true;

            segmentSizes = null;
            this.reservationStorage = null;
        }
    }

    /**
     * Adding the WAL segment size in the archive.
     *
     * @param idx Absolut segment index.
     * @param sizeChange Segment size in bytes.
     */
    void addSize(long idx, long sizeChange) {
        long releaseIdx = -1;
        int releaseCnt = 0;

        synchronized (this) {
            walArchiveSize += sizeChange;

            if (!walArchiveUnlimited) {
                segmentSizes.compute(idx, (i, size) -> {
                    long res = (size == null ? 0 : size) + sizeChange;

                    return res == 0 ? null : res;
                });
            }

            if (sizeChange > 0) {
                if (!walArchiveUnlimited && walArchiveSize >= maxWalArchiveSize) {
                    long size = 0;

                    for (Map.Entry<Long, Long> e : segmentSizes.entrySet()) {
                        releaseIdx = e.getKey();
                        releaseCnt++;

                        if (walArchiveSize - (size += e.getValue()) < minWalArchiveSize)
                            break;
                    }
                }

                notifyAll();
            }
        }

        if (releaseIdx != -1) {
            if (log.isInfoEnabled()) {
                log.info("Maximum size of the WAL archive exceeded, the segments will be forcibly released [" +
                    "maxWalArchiveSize=" + U.humanReadableByteCount(maxWalArchiveSize) + ", releasedSegmentCnt=" +
                    releaseCnt + ", lastReleasedSegmentIdx=" + releaseIdx + ']');
            }

            reservationStorage.forceRelease(releaseIdx);
        }
    }

    /**
     * Reset the current and reserved WAL archive sizes.
     */
    synchronized void resetSizes() {
        walArchiveSize = 0;

        if (!walArchiveUnlimited)
            segmentSizes.clear();
    }

    /**
     * Waiting for exceeding the maximum WAL archive size.
     * To track size of WAL archive, need to use {@link #addSize}.
     *
     * @param max Maximum WAL archive size in bytes.
     * @throws IgniteInterruptedCheckedException If it was interrupted.
     */
    synchronized void awaitExceedMaxSize(long max) throws IgniteInterruptedCheckedException {
        try {
            while (max - walArchiveSize > 0 && !interrupted)
                wait();
        }
        catch (InterruptedException e) {
            throw new IgniteInterruptedCheckedException(e);
        }

        if (interrupted)
            throw new IgniteInterruptedCheckedException("Interrupt waiting of exceed max archive size");
    }

    /**
     * Interrupt waiting on this object.
     */
    synchronized void interrupt() {
        interrupted = true;

        notifyAll();
    }

    /**
     * Reset interrupted flag.
     */
    synchronized void reset() {
        interrupted = false;
    }

    /**
     * Getting current WAL archive size in bytes.
     *
     * @return Size in bytes.
     */
    synchronized long currentSize() {
        return walArchiveSize;
    }

    /**
     * Getting the size of the WAL segment of the archive in bytes.
     *
     * @return Size in bytes or {@code null} if the segment is absent or the archive is unlimited.
     */
    @Nullable Long segmentSize(long idx) {
        if (walArchiveUnlimited)
            return null;
        else {
            synchronized (this) {
                return segmentSizes.get(idx);
            }
        }
    }
}
