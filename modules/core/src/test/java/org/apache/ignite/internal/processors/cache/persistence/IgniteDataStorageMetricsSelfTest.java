/*
 * Copyright 2021 GridGain Systems, Inc. and Contributors.
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

package org.apache.ignite.internal.processors.cache.persistence;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.ignite.DataRegionMetrics;
import org.apache.ignite.DataStorageMetrics;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.affinity.rendezvous.RendezvousAffinityFunction;
import org.apache.ignite.cache.query.annotations.QuerySqlField;
import org.apache.ignite.cluster.ClusterState;
import org.apache.ignite.configuration.BinaryConfiguration;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.WALMode;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.internal.pagemem.wal.record.DataRecord;
import org.apache.ignite.internal.processors.cache.WalStateManager.WALDisableContext;
import org.apache.ignite.internal.processors.cache.persistence.wal.FileDescriptor;
import org.apache.ignite.internal.processors.cache.persistence.wal.FileWALPointer;
import org.apache.ignite.internal.processors.metric.MetricRegistry;
import org.apache.ignite.internal.processors.metric.impl.AtomicLongMetric;
import org.apache.ignite.internal.processors.metric.impl.LongAdderMetric;
import org.apache.ignite.internal.processors.metric.impl.LongGauge;
import org.apache.ignite.internal.util.tostring.GridToStringInclude;
import org.apache.ignite.internal.util.typedef.PAX;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.mxbean.DataStorageMetricsMXBean;
import org.apache.ignite.spi.metric.HistogramMetric;
import org.apache.ignite.testframework.ListeningTestLogger;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;

import static java.util.Collections.emptyList;
import static org.apache.ignite.cache.CacheAtomicityMode.ATOMIC;
import static org.apache.ignite.cache.CacheMode.PARTITIONED;
import static org.apache.ignite.cache.CacheWriteSynchronizationMode.FULL_SYNC;
import static org.apache.ignite.cluster.ClusterState.ACTIVE;
import static org.apache.ignite.internal.processors.cache.persistence.DataStorageMetricsImpl.DATASTORAGE_METRIC_PREFIX;
import static org.apache.ignite.internal.processors.cache.persistence.wal.serializer.RecordV1Serializer.HEADER_RECORD_SIZE;
import static org.apache.ignite.testframework.GridTestUtils.setFieldValue;
import static org.apache.ignite.testframework.GridTestUtils.waitForCondition;

/**
 *
 */
public class IgniteDataStorageMetricsSelfTest extends GridCommonAbstractTest {
    /** */
    private static final String GROUP1 = "grp1";

    /** */
    private static final String NO_PERSISTENCE = "no-persistence";

    /** */
    private final ListeningTestLogger listeningLog = new ListeningTestLogger(log);

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        cleanPersistenceDir();
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        super.beforeTest();

        cleanPersistenceDir();
    }

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String gridName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(gridName);

        cfg.setConsistentId(gridName);

        long maxRegionSize = 20L * 1024 * 1024;

        DataStorageConfiguration memCfg = new DataStorageConfiguration()
            .setDefaultDataRegionConfiguration(new DataRegionConfiguration()
                .setMaxSize(maxRegionSize)
                .setPersistenceEnabled(true)
                .setMetricsEnabled(true)
                .setName("dflt-plc"))
            .setDataRegionConfigurations(new DataRegionConfiguration()
                .setMaxSize(maxRegionSize)
                .setPersistenceEnabled(false)
                .setMetricsEnabled(true)
                .setName(NO_PERSISTENCE))
            .setWalMode(WALMode.LOG_ONLY)
            .setMetricsEnabled(true);

        cfg.setDataStorageConfiguration(memCfg);

        cfg.setBinaryConfiguration(new BinaryConfiguration().setCompactFooter(false));

        cfg.setCacheConfiguration(
            cacheConfiguration(GROUP1, "cache", PARTITIONED, ATOMIC, 1, null),
            cacheConfiguration(null, "cache-np", PARTITIONED, ATOMIC, 1, NO_PERSISTENCE));

        cfg.setGridLogger(listeningLog);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();

        cleanPersistenceDir();

        super.afterTest();
    }

    /**
     * @param grpName Cache group name.
     * @param name Cache name.
     * @param cacheMode Cache mode.
     * @param atomicityMode Atomicity mode.
     * @param backups Backups number.
     * @return Cache configuration.
     */
    private CacheConfiguration cacheConfiguration(
        String grpName,
        String name,
        CacheMode cacheMode,
        CacheAtomicityMode atomicityMode,
        int backups,
        String dataRegName
    ) {
        CacheConfiguration ccfg = new CacheConfiguration();

        ccfg.setName(name);
        ccfg.setGroupName(grpName);
        ccfg.setAtomicityMode(atomicityMode);
        ccfg.setBackups(backups);
        ccfg.setCacheMode(cacheMode);
        ccfg.setWriteSynchronizationMode(FULL_SYNC);
        ccfg.setDataRegionName(dataRegName);
        ccfg.setAffinity(new RendezvousAffinityFunction(false, 32));

        return ccfg;
    }

    /**
     * @throws Exception if failed.
     */
    @Test
    public void testPersistenceMetrics() throws Exception {
        final IgniteEx ig = startGrid(0);

        ig.active(true);

        try {
            IgniteCache<Object, Object> cache = ig.cache("cache");

            for (int i = 0; i < 10; i++)
                cache.put(i, new Person("first-" + i, "last-" + i));

            IgniteCache<Object, Object> cacheNp = ig.cache("cache-np");

            for (int i = 0; i < 10; i++)
                cacheNp.put(i, new Person("first-" + i, "last-" + i));

            DataRegionMetrics memMetrics = ig.dataRegionMetrics("dflt-plc");

            assertNotNull(memMetrics);
            assertTrue(memMetrics.getDirtyPages() > 0);
            assertTrue(memMetrics.getPagesFillFactor() > 0);

            memMetrics = ig.dataRegionMetrics("no-persistence");

            assertNotNull(memMetrics);
            assertTrue(memMetrics.getTotalAllocatedPages() > 0);
            assertTrue(memMetrics.getPagesFillFactor() > 0);

            ig.context().cache().context().database().waitForCheckpoint("test");

            assertTrue(waitForCondition(new PAX() {
                @Override public boolean applyx() {
                    DataStorageMetrics pMetrics = ig.dataStorageMetrics();

                    assertNotNull(pMetrics);

                    return pMetrics.getLastCheckpointTotalPagesNumber() != 0 &&
                        pMetrics.getLastCheckpointDataPagesNumber() != 0;
                }
            }, 10_000));
        }
        finally {
            stopAllGrids();
        }
    }

    /** @throws Exception if failed. */
    @Test
    public void testCheckpointMetrics() throws Exception {
        Pattern cpPtrn = Pattern.compile("^Checkpoint started .*" +
            "checkpointBeforeLockTime=(\\d+)ms, " +
            "checkpointLockWait=(\\d+)ms, " +
            "checkpointListenersExecuteTime=(\\d+)ms, " +
            "checkpointLockHoldTime=(\\d+)ms, " +
            "walCpRecordFsyncDuration=(\\d+)ms, " +
            "writeCheckpointEntryDuration=(\\d+)ms, " +
            "splitAndSortCpPagesDuration=(\\d+)ms");

        AtomicLong expLastCpBeforeLockDuration = new AtomicLong();
        AtomicLong expLastCpLockWaitDuration = new AtomicLong();
        AtomicLong expLastCpListenersExecuteDuration = new AtomicLong();
        AtomicLong expLastCpLockHoldDuration = new AtomicLong();
        AtomicLong expLastCpWalRecordFsyncDuration = new AtomicLong();
        AtomicLong expLastCpWriteEntryDuration = new AtomicLong();
        AtomicLong expLastCpSplitAndSortPagesDuration = new AtomicLong();
        AtomicInteger cpCnt = new AtomicInteger();

        listeningLog.registerListener(s -> {
            Matcher matcher = cpPtrn.matcher(s);

            if (!matcher.find())
                return;

            expLastCpBeforeLockDuration.set(Long.parseLong(matcher.group(1)));
            expLastCpLockWaitDuration.set(Long.parseLong(matcher.group(2)));
            expLastCpListenersExecuteDuration.set(Long.parseLong(matcher.group(3)));
            expLastCpLockHoldDuration.set(Long.parseLong(matcher.group(4)));
            expLastCpWalRecordFsyncDuration.set(Long.parseLong(matcher.group(5)));
            expLastCpWriteEntryDuration.set(Long.parseLong(matcher.group(6)));
            expLastCpSplitAndSortPagesDuration.set(Long.parseLong(matcher.group(7)));
            cpCnt.incrementAndGet();
        });

        IgniteEx node = startGrid(0);

        node.cluster().state(ACTIVE);

        GridCacheDatabaseSharedManager db = (GridCacheDatabaseSharedManager)node.context().cache().context().database();

        db.checkpointReadLock();

        try {
            waitForCondition(() -> cpCnt.get() > 0, getTestTimeout());

            MetricRegistry mreg = node.context().metric().registry(DATASTORAGE_METRIC_PREFIX);

            AtomicLongMetric lastCpBeforeLockDuration = mreg.findMetric("LastCheckpointBeforeLockDuration");
            AtomicLongMetric lastCpLockWaitDuration = mreg.findMetric("LastCheckpointLockWaitDuration");
            AtomicLongMetric lastCpListenersExecuteDuration = mreg.findMetric("LastCheckpointListenersExecuteDuration");
            AtomicLongMetric lastCpLockHoldDuration = mreg.findMetric("LastCheckpointLockHoldDuration");
            AtomicLongMetric lastCpWalRecordFsyncDuration = mreg.findMetric("LastCheckpointWalRecordFsyncDuration");
            AtomicLongMetric lastCpWriteEntryDuration = mreg.findMetric("LastCheckpointWriteEntryDuration");
            AtomicLongMetric lastCpSplitAndSortPagesDuration =
                mreg.findMetric("LastCheckpointSplitAndSortPagesDuration");

            HistogramMetric cpBeforeLockHistogram = mreg.findMetric("CheckpointBeforeLockHistogram");
            HistogramMetric cpLockWaitHistogram = mreg.findMetric("CheckpointLockWaitHistogram");
            HistogramMetric cpListenersExecuteHistogram = mreg.findMetric("CheckpointListenersExecuteHistogram");
            HistogramMetric cpMarkHistogram = mreg.findMetric("CheckpointMarkHistogram");
            HistogramMetric cpLockHoldHistogram = mreg.findMetric("CheckpointLockHoldHistogram");
            HistogramMetric cpPagesWriteHistogram = mreg.findMetric("CheckpointPagesWriteHistogram");
            HistogramMetric cpFsyncHistogram = mreg.findMetric("CheckpointFsyncHistogram");
            HistogramMetric cpWalRecordFsyncHistogram = mreg.findMetric("CheckpointWalRecordFsyncHistogram");
            HistogramMetric cpWriteEntryHistogram = mreg.findMetric("CheckpointWriteEntryHistogram");
            HistogramMetric cpSplitAndSortPagesHistogram = mreg.findMetric("CheckpointSplitAndSortPagesHistogram");
            HistogramMetric cpHistogram = mreg.findMetric("CheckpointHistogram");

            waitForCondition(() -> cpCnt.get() == Arrays.stream(cpHistogram.value()).sum(), getTestTimeout());

            assertEquals(cpCnt.get(), Arrays.stream(cpBeforeLockHistogram.value()).sum());
            assertEquals(cpCnt.get(), Arrays.stream(cpLockWaitHistogram.value()).sum());
            assertEquals(cpCnt.get(), Arrays.stream(cpListenersExecuteHistogram.value()).sum());
            assertEquals(cpCnt.get(), Arrays.stream(cpMarkHistogram.value()).sum());
            assertEquals(cpCnt.get(), Arrays.stream(cpLockHoldHistogram.value()).sum());
            assertEquals(cpCnt.get(), Arrays.stream(cpPagesWriteHistogram.value()).sum());
            assertEquals(cpCnt.get(), Arrays.stream(cpFsyncHistogram.value()).sum());
            assertEquals(cpCnt.get(), Arrays.stream(cpWalRecordFsyncHistogram.value()).sum());
            assertEquals(cpCnt.get(), Arrays.stream(cpWriteEntryHistogram.value()).sum());
            assertEquals(cpCnt.get(), Arrays.stream(cpSplitAndSortPagesHistogram.value()).sum());

            assertEquals(expLastCpBeforeLockDuration.get(), lastCpBeforeLockDuration.value());
            assertEquals(expLastCpLockWaitDuration.get(), lastCpLockWaitDuration.value());
            assertEquals(expLastCpListenersExecuteDuration.get(), lastCpListenersExecuteDuration.value());
            assertEquals(expLastCpLockHoldDuration.get(), lastCpLockHoldDuration.value());
            assertEquals(expLastCpWalRecordFsyncDuration.get(), lastCpWalRecordFsyncDuration.value());
            assertEquals(expLastCpWriteEntryDuration.get(), lastCpWriteEntryDuration.value());
            assertEquals(expLastCpSplitAndSortPagesDuration.get(), lastCpSplitAndSortPagesDuration.value());
        }
        finally {
            db.checkpointReadUnlock();
        }
    }

    /**
     * Test for metric lastCheckpointStarted
     *
     * @throws Exception if failed.
     * */
    @Test
    public void testLastCheckpointStarted() throws Exception {
        IgniteEx ex = startGrid(0);
        ex.cluster().state(ClusterState.ACTIVE);

        try {
            long prevLastStart = 0;

            for (int i = 0; i < 5; i++) {
                IgniteCache<Object, Object> cache = ex.cache("cache");

                for (int j = 0; j < 10_000; j++)
                    cache.put(j, "VALUE_" + i + "_" + j);

                ex.context().cache().context().database().waitForCheckpoint("test");

                try {
                    Thread.sleep(100);
                }
                catch (InterruptedException e) { /* no op */ }

                GridCacheDatabaseSharedManager dbMgr =
                        (GridCacheDatabaseSharedManager)ex.context().cache().context().database();
                DataStorageMetrics dsm = dbMgr.persistentStoreMetrics();

                long lastStart = dsm.getLastCheckpointStarted();

                assertTrue(lastStart > 0);
                assertTrue(lastStart - prevLastStart > 0);

                prevLastStart = lastStart;
            }
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     * Checking that the metrics of the total logged bytes are working correctly.
     *
     * @throws Exception If failed.
     */
    @Test
    public void testWalWrittenBytes() throws Exception {
        IgniteEx n = startGrid(0, (Consumer<IgniteConfiguration>)cfg ->
            cfg.getDataStorageConfiguration().setWalSegmentSize((int)(2 * U.MB)));

        n.cluster().state(ACTIVE);
        awaitPartitionMapExchange();

        for (int i = 0; i < 10; i++)
            n.cache("cache").put(ThreadLocalRandom.current().nextLong(), new byte[(int)(32 * U.KB)]);

        WALDisableContext walDisableCtx = n.context().cache().context().walState().walDisableContext();
        assertNotNull(walDisableCtx);

        setFieldValue(walDisableCtx, "disableWal", true);

        assertTrue(walDisableCtx.check());
        assertNull(walMgr(n).log(new DataRecord(emptyList())));

        assertEquals(-1, walMgr(n).lastArchivedSegment());

        long exp = ((FileWALPointer)walMgr(n).lastWritePointer()).fileOffset() - HEADER_RECORD_SIZE;

        assertEquals(exp, dbMgr(n).persistentStoreMetrics().getWalWrittenBytes());
        assertEquals(exp, dsMetricsMXBean(n).getWalWrittenBytes());
        assertEquals(exp, ((LongAdderMetric)dsMetricRegistry(n).findMetric("WalWrittenBytes")).value());
    }

    /**
     * Check whether Wal is reporting correct usage.
     *
     * @throws Exception
     */
    @Test
    public void testWalTotalSize() throws Exception {
        IgniteEx n = startGrid(0, (Consumer<IgniteConfiguration>)cfg ->
            cfg.getDataStorageConfiguration().setWalSegmentSize((int)(2 * U.MB)));

        n.cluster().state(ACTIVE);
        awaitPartitionMapExchange();

        for (int i = 0; i < 10; i++)
            n.cache("cache").put(ThreadLocalRandom.current().nextLong(), new byte[(int)(32 * U.KB)]);

        long exp = 10 * 2048 * 1024;

        assertEquals(exp, dbMgr(n).persistentStoreMetrics().getWalTotalSize());
        assertEquals(exp, dsMetricsMXBean(n).getWalTotalSize());
        assertEquals(exp, ((LongGauge)dsMetricRegistry(n).findMetric("WalTotalSize")).value());
    }

    /**
     * Check whether Wal is reporting correct usage when WAL Archive is turned off.
     *
     * @throws Exception
     */
    @Test
    public void testWalTotalSizeWithArchive() throws Exception {

        IgniteEx n = startGrid(0, (Consumer<IgniteConfiguration>)cfg ->
            cfg.getDataStorageConfiguration().setWalSegmentSize((int)(2 * U.MB)));

        n.cluster().state(ACTIVE);
        awaitPartitionMapExchange();

        for (int i = 0; i < 10; i++)
            n.cache("cache").put(ThreadLocalRandom.current().nextLong(), new byte[(int)(32 * U.KB)]);

        while (walMgr(n).lastArchivedSegment() < 3)
            n.cache("cache").put(ThreadLocalRandom.current().nextLong(), new byte[(int)(32 * U.KB)]);

        long exp = 14 * 2048 * 1024;

        //the wal archive segements are not exactly 2K bytes.
        assertTrue(exp >= dbMgr(n).persistentStoreMetrics().getWalTotalSize());
        assertTrue(exp >= dsMetricsMXBean(n).getWalTotalSize());
        assertTrue(exp >= ((LongGauge)dsMetricRegistry(n).findMetric("WalTotalSize")).value());
    }

    /**
     * Check whether Wal is reporting correct usage when WAL Archive is turned off.
     *
     * @throws Exception
     */
    @Test
    public void testWalTotalSizeWithArchiveTurnedOff() throws Exception {

        IgniteEx n = startGrid(0, (Consumer<IgniteConfiguration>)cfg ->
            cfg.getDataStorageConfiguration().setWalArchivePath(cfg.getDataStorageConfiguration().getWalPath()).setWalSegmentSize((int)(2 * U.MB)));

        n.cluster().state(ACTIVE);
        awaitPartitionMapExchange();

        for (int i = 0; i < 10; i++)
            n.cache("cache").put(ThreadLocalRandom.current().nextLong(), new byte[(int)(32 * U.KB)]);

        long exp = 2048 * 1024;

        assertEquals(exp, dbMgr(n).persistentStoreMetrics().getWalTotalSize());
        assertEquals(exp, dsMetricsMXBean(n).getWalTotalSize());
        assertEquals(exp, ((LongGauge)dsMetricRegistry(n).findMetric("WalTotalSize")).value());
    }

    /**
     * Checking that the metrics of the total size compressed segment are working correctly.
     *
     * @throws Exception If failed.
     */
    @Test
    public void testWalCompressedBytes() throws Exception {
        IgniteEx n0 = startGrid(0, (Consumer<IgniteConfiguration>)cfg ->
            cfg.getDataStorageConfiguration().setWalCompactionEnabled(true).setWalSegmentSize((int)(2 * U.MB)));

        n0.cluster().state(ACTIVE);
        awaitPartitionMapExchange();

        while (walMgr(n0).lastArchivedSegment() < 3)
            n0.cache("cache").put(ThreadLocalRandom.current().nextLong(), new byte[(int)(32 * U.KB)]);

        waitForCondition(
            () -> walMgr(n0).lastArchivedSegment() == walMgr(n0).lastCompactedSegment(),
            getTestTimeout()
        );

        assertCorrectWalCompressedBytesMetrics(n0);

        stopAllGrids();

        IgniteEx n1 = startGrid(0, (Consumer<IgniteConfiguration>)cfg ->
            cfg.getDataStorageConfiguration().setWalCompactionEnabled(true));

        n1.cluster().state(ACTIVE);
        awaitPartitionMapExchange();

        assertCorrectWalCompressedBytesMetrics(n1);
    }

    /**
     *
     */
    static class Person implements Serializable {
        /** */
        @GridToStringInclude
        @QuerySqlField(index = true, groups = "full_name")
        private String fName;

        /** */
        @GridToStringInclude
        @QuerySqlField(index = true, groups = "full_name")
        private String lName;

        /**
         * @param fName First name.
         * @param lName Last name.
         */
        public Person(String fName, String lName) {
            this.fName = fName;
            this.lName = lName;
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(Person.class, this);
        }

        /** {@inheritDoc} */
        @Override public boolean equals(Object o) {
            if (this == o)
                return true;

            if (o == null || getClass() != o.getClass())
                return false;

            Person person = (Person)o;

            return Objects.equals(fName, person.fName) &&
                Objects.equals(lName, person.lName);
        }

        /** {@inheritDoc} */
        @Override public int hashCode() {
            return Objects.hash(fName, lName);
        }
    }

    /**
     * Getting DATASTORAGE_METRIC_PREFIX metric registry.
     *
     * @param n Node.
     * @return Group of metrics.
     */
    private MetricRegistry dsMetricRegistry(IgniteEx n) {
        return n.context().metric().registry(DATASTORAGE_METRIC_PREFIX);
    }

    /**
     * Getting data storage MXBean.
     *
     * @param n Node.
     * @return MXBean.
     */
    private DataStorageMetricsMXBean dsMetricsMXBean(IgniteEx n) {
        return getMxBean(n.name(), "Persistent Store", "DataStorageMetrics", DataStorageMetricsMXBean.class);
    }

    /**
     * Check that the metric of the total size compressed segment is working correctly.
     *
     * @param n Node.
     */
    private void assertCorrectWalCompressedBytesMetrics(IgniteEx n) {
        long exp = Arrays.stream(walMgr(n).walArchiveFiles()).filter(FileDescriptor::isCompressed)
            .mapToLong(fd -> fd.file().length()).sum();

        assertEquals(exp, dbMgr(n).persistentStoreMetrics().getWalCompressedBytes());
        assertEquals(exp, dsMetricsMXBean(n).getWalCompressedBytes());
        assertEquals(exp, ((LongAdderMetric)dsMetricRegistry(n).findMetric("WalCompressedBytes")).value());
    }
}
