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

package org.apache.ignite.internal.processors.cache.persistence.db.wal;

import java.io.File;
import java.nio.channels.Channel;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.WALMode;
import org.apache.ignite.internal.GridKernalContext;
import org.apache.ignite.internal.IgniteInternalFuture;
import org.apache.ignite.internal.managers.eventstorage.GridEventStorageManager;
import org.apache.ignite.internal.pagemem.wal.IgniteWriteAheadLogManager;
import org.apache.ignite.internal.pagemem.wal.WALIterator;
import org.apache.ignite.internal.pagemem.wal.WALPointer;
import org.apache.ignite.internal.pagemem.wal.record.MetastoreDataRecord;
import org.apache.ignite.internal.pagemem.wal.record.SwitchSegmentRecord;
import org.apache.ignite.internal.pagemem.wal.record.WALRecord;
import org.apache.ignite.internal.processors.cache.GridCacheIoManager;
import org.apache.ignite.internal.processors.cache.GridCacheSharedContext;
import org.apache.ignite.internal.processors.cache.WalStateManager;
import org.apache.ignite.internal.processors.cache.binary.CacheObjectBinaryProcessorImpl;
import org.apache.ignite.internal.processors.cache.persistence.GridCacheDatabaseSharedManager;
import org.apache.ignite.internal.processors.cache.persistence.IgniteCacheDatabaseSharedManager;
import org.apache.ignite.internal.processors.cache.persistence.file.FileIO;
import org.apache.ignite.internal.processors.cache.persistence.file.FileIOFactory;
import org.apache.ignite.internal.processors.cache.persistence.file.RandomAccessFileIOFactory;
import org.apache.ignite.internal.processors.cache.persistence.wal.FileWriteAheadLogManager;
import org.apache.ignite.internal.processors.cache.persistence.wal.aware.SegmentAware;
import org.apache.ignite.internal.processors.cache.persistence.wal.io.FileInput;
import org.apache.ignite.internal.processors.cache.persistence.wal.reader.StandaloneGridKernalContext;
import org.apache.ignite.internal.processors.cache.persistence.wal.serializer.RecordSerializer;
import org.apache.ignite.internal.processors.cache.persistence.wal.serializer.RecordSerializerFactoryImpl;
import org.apache.ignite.internal.processors.cacheobject.IgniteCacheObjectProcessor;
import org.apache.ignite.internal.processors.subscription.GridInternalSubscriptionProcessor;
import org.apache.ignite.internal.util.typedef.T2;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.lang.IgniteBiTuple;
import org.apache.ignite.spi.eventstorage.NoopEventStorageSpi;
import org.apache.ignite.testframework.GridTestUtils;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Assert;
import org.junit.Test;

import static org.apache.ignite.internal.pagemem.wal.record.WALRecord.RecordType.METASTORE_DATA_RECORD;
import static org.apache.ignite.internal.processors.cache.persistence.wal.serializer.RecordV1Serializer.HEADER_RECORD_SIZE;
import static org.apache.ignite.testframework.GridTestUtils.getFieldValueHierarchy;
import static org.apache.ignite.testframework.GridTestUtils.waitForCondition;

/***
 * Test check correct switch segment if in the tail of segment have garbage.
 */
public class IgniteWalIteratorSwitchSegmentTest extends GridCommonAbstractTest {
    /** Segment file size. */
    private static final int SEGMENT_SIZE = 1024 * 1024;

    /** WAL segment file sub directory. */
    private static final String WORK_SUB_DIR = "/NODE/wal";

    /** WAL archive segment file sub directory. */
    private static final String ARCHIVE_SUB_DIR = "/NODE/walArchive";

    /** Serializer versions for check. */
    private int[] checkSerializerVers = new int[] {
        1,
        2
    };

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        super.beforeTest();

        cleanPersistenceDir();
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        super.afterTest();

        cleanPersistenceDir();
    }

    /** {@inheritDoc} */
    @Override protected void cleanPersistenceDir() throws Exception {
        super.cleanPersistenceDir();

        U.resolveWorkDirectory(U.defaultWorkDirectory(), WORK_SUB_DIR, true);
        U.resolveWorkDirectory(U.defaultWorkDirectory(), ARCHIVE_SUB_DIR, true);
    }

    /**
     * Test for check invariant, size of SWITCH_SEGMENT_RECORD should be 1 byte.
     *
     * @throws Exception If some thing failed.
     */
    @Test
    public void testCheckSerializer() throws Exception {
        for (int serVer : checkSerializerVers) {
            checkInvariantSwitchSegmentSize(serVer);
        }
    }

    /**
     * @param serVer WAL serializer version.
     * @throws Exception If some thing failed.
     */
    private void checkInvariantSwitchSegmentSize(int serVer) throws Exception {
        GridKernalContext kctx = new StandaloneGridKernalContext(
            log, null, null) {
            @Override public IgniteCacheObjectProcessor cacheObjects() {
                return new CacheObjectBinaryProcessorImpl(this);
            }
        };

        RecordSerializer serializer = new RecordSerializerFactoryImpl(
            new GridCacheSharedContext<>(
                kctx,
                null,
                null,
                null,
                null,
                null,
                null,
                new IgniteCacheDatabaseSharedManager() {
                    @Override public int pageSize() {
                        return DataStorageConfiguration.DFLT_PAGE_SIZE;
                    }
                },
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null)
        ).createSerializer(serVer);

        SwitchSegmentRecord switchSegmentRecord = new SwitchSegmentRecord();

        int recordSize = serializer.size(switchSegmentRecord);

        Assert.assertEquals(1, recordSize);
    }

    /**
     * Test for check invariant, size of SWITCH_SEGMENT_RECORD should be 1 byte.
     *
     * @throws Exception If some thing failed.
     */
    @Test
    public void testInvariantSwitchSegment() throws Exception {
        for (int serVer : checkSerializerVers) {
            try {
                checkInvariantSwitchSegment(serVer);
            }
            finally {
                cleanPersistenceDir();
            }
        }
    }

    /**
     * Test for check switch segment from work dir to archive dir during iteration.
     *
     * @throws Exception If some thing failed.
     */
    @Test
    public void testSwitchReadingSegmentFromWorkToArchive() throws Exception {
        for (int serVer : checkSerializerVers) {
            try {
                checkSwitchReadingSegmentDuringIteration(serVer);
            }
            finally {
                cleanPersistenceDir();
            }
        }
    }

    /**
     * @param serVer WAL serializer version.
     * @throws Exception If some thing failed.
     */
    private void checkInvariantSwitchSegment(int serVer) throws Exception {
        String workDir = U.defaultWorkDirectory();

        T2<IgniteWriteAheadLogManager, RecordSerializer> initTup = initiate(serVer, workDir);

        IgniteWriteAheadLogManager walMgr = initTup.get1();

        RecordSerializer recordSerializer = initTup.get2();

        int switchSegmentRecordSize = recordSerializer.size(new SwitchSegmentRecord());

        log.info("switchSegmentRecordSize:" + switchSegmentRecordSize);

        int tailSize = 0;

        /* Initial record payload size. */
        int payloadSize = 1024;

        int recSize = 0;

        MetastoreDataRecord rec = null;

        /* Record size. */
        int recordTypeSize = 1;

        /* Record pointer. */
        int recordPointerSize = 8 + 4 + 4;

        int lowBound = recordTypeSize + recordPointerSize;
        int highBound = lowBound + /*CRC*/4;

        int attempt = 1000;

        // Try find how many record need for specific tail size.
        while (true) {
            if (attempt < 0)
                throw new IgniteCheckedException("Can not find any payload size for test, " +
                    "lowBound=" + lowBound + ", highBound=" + highBound);

            if (tailSize >= lowBound && tailSize < highBound)
                break;

            payloadSize++;

            byte[] payload = new byte[payloadSize];

            // Fake record for payload.
            rec = new MetastoreDataRecord("0", payload);

            recSize = recordSerializer.size(rec);

            tailSize = (SEGMENT_SIZE - HEADER_RECORD_SIZE) % recSize;

            attempt--;
        }

        Assert.assertNotNull(rec);

        int recordsToWrite = SEGMENT_SIZE / recSize;

        log.info("records to write " + recordsToWrite + " tail size " +
            (SEGMENT_SIZE - HEADER_RECORD_SIZE) % recSize);

        // Add more record for rollover to the next segment.
        recordsToWrite += 100;

        for (int i = 0; i < recordsToWrite; i++)
            walMgr.log(new MetastoreDataRecord(rec.key(), rec.value()));

        walMgr.flush(null, true);

        // Await archiver move segment to WAL archive.
        Thread.sleep(5000);

        // If switchSegmentRecordSize more that 1, it mean that invariant is broke.
        // Filling tail some garbage. Simulate tail garbage on rotate segment in WAL work directory.
        if (switchSegmentRecordSize > 1) {
            File seg = new File(workDir + ARCHIVE_SUB_DIR + "/0000000000000000.wal");

            FileIOFactory ioFactory = new RandomAccessFileIOFactory();

            FileIO seg0 = ioFactory.create(seg);

            byte[] bytes = new byte[tailSize];

            Random rnd = new Random();

            rnd.nextBytes(bytes);

            // Some record type.
            bytes[0] = (byte)(METASTORE_DATA_RECORD.ordinal() + 1);

            seg0.position((int)(seg0.size() - tailSize));

            seg0.write(bytes, 0, tailSize);

            seg0.force(true);

            seg0.close();
        }

        int expectedRecords = recordsToWrite;
        int actualRecords = 0;

        // Check that switch segment works as expected and all record is reachable.
        try (WALIterator it = walMgr.replay(null)) {
            while (it.hasNext()) {
                IgniteBiTuple<WALPointer, WALRecord> tup = it.next();

                WALRecord rec0 = tup.get2();

                if (rec0.type() == METASTORE_DATA_RECORD)
                    actualRecords++;
            }
        }

        Assert.assertEquals("Not all records read during iteration.", expectedRecords, actualRecords);
    }

    /**
     * @param serVer WAL serializer version.
     * @throws Exception If some thing failed.
     */
    private void checkSwitchReadingSegmentDuringIteration(int serVer) throws Exception {
        String workDir = U.defaultWorkDirectory();

        T2<IgniteWriteAheadLogManager, RecordSerializer> initTup = initiate(serVer, workDir);

        IgniteWriteAheadLogManager walMgr = initTup.get1();

        RecordSerializer recordSerializer = initTup.get2();

        MetastoreDataRecord rec = new MetastoreDataRecord("0", new byte[100]);

        int recSize = recordSerializer.size(rec);

        // Add more record for rollover to the next segment.
        int recordsToWrite = SEGMENT_SIZE / recSize + 100;

        SegmentAware segmentAware = GridTestUtils.getFieldValue(walMgr, "segmentAware");

        //guard from archivation before iterator would be created.
        segmentAware.checkCanReadArchiveOrReserveWorkSegment(0);

        for (int i = 0; i < recordsToWrite; i++)
            walMgr.log(new MetastoreDataRecord(rec.key(), rec.value()));

        walMgr.flush(null, true);

        int expectedRecords = recordsToWrite;
        AtomicInteger actualRecords = new AtomicInteger(0);

        AtomicReference<String> startedSegmentPath = new AtomicReference<>();
        AtomicReference<String> finishedSegmentPath = new AtomicReference<>();

        CountDownLatch startedIteratorLatch = new CountDownLatch(1);
        CountDownLatch finishedArchivedLatch = new CountDownLatch(1);

        IgniteInternalFuture<Object> future = GridTestUtils.runAsync(
            () -> {
                // Check that switch segment works as expected and all record is reachable.
                try (WALIterator it = walMgr.replay(null)) {
                    Object handle = getFieldValueHierarchy(it, "currWalSegment");
                    FileInput in = getFieldValueHierarchy(handle, "in");
                    Object delegate = getFieldValueHierarchy(in.io(), "delegate");
                    Channel ch = getFieldValueHierarchy(delegate, "ch");
                    String path = getFieldValueHierarchy(ch, "path");

                    startedSegmentPath.set(path);

                    startedIteratorLatch.countDown();

                    while (it.hasNext()) {
                        IgniteBiTuple<WALPointer, WALRecord> tup = it.next();

                        WALRecord rec0 = tup.get2();

                        if (rec0.type() == METASTORE_DATA_RECORD)
                            actualRecords.incrementAndGet();

                        finishedArchivedLatch.await();
                    }

                    in = getFieldValueHierarchy(handle, "in");
                    delegate = getFieldValueHierarchy(in.io(), "delegate");
                    ch = getFieldValueHierarchy(delegate, "ch");
                    path = getFieldValueHierarchy(ch, "path");

                    finishedSegmentPath.set(path);
                }

                return null;
            }
        );

        startedIteratorLatch.await();

        segmentAware.releaseWorkSegment(0);

        waitForCondition(() -> segmentAware.lastArchivedAbsoluteIndex() == 0, 5000);

        finishedArchivedLatch.countDown();

        future.get();

        // TODO: 10.11.2020 del
        if (log.isInfoEnabled())
            log.info(String.format("kirill w1=%s, w2=%s", workDir, U.defaultWorkDirectory()));

        //should started iteration from work directory but finish from archive directory.
        File workSeg0 = U.resolveWorkDirectory(workDir, WORK_SUB_DIR + "/0000000000000000.wal", false);
        File archiveSeg0 = U.resolveWorkDirectory(workDir, ARCHIVE_SUB_DIR + "/0000000000000000.wal", false);

        assertEquals(workSeg0.getAbsolutePath(), new File(startedSegmentPath.get()).getAbsolutePath());
        assertEquals(archiveSeg0.getAbsolutePath(), new File(finishedSegmentPath.get()).getAbsolutePath());

        Assert.assertEquals("Not all records read during iteration.", expectedRecords, actualRecords.get());
    }

    /***
     * Initiate WAL manager.
     *
     * @param serVer WAL serializer version.
     * @param workDir Work directory path.
     * @return Tuple of WAL manager and WAL record serializer.
     * @throws IgniteCheckedException If some think failed.
     */
    private T2<IgniteWriteAheadLogManager, RecordSerializer> initiate(
        int serVer,
        String workDir
    ) throws IgniteCheckedException {

        GridKernalContext kctx = new StandaloneGridKernalContext(
            log, null, null
        ) {
            @Override protected IgniteConfiguration prepareIgniteConfiguration() {
                IgniteConfiguration cfg = super.prepareIgniteConfiguration();

                cfg.setDataStorageConfiguration(
                    new DataStorageConfiguration()
                        .setWalSegmentSize(SEGMENT_SIZE)
                        .setWalRecordIteratorBufferSize(SEGMENT_SIZE / 2)
                        .setWalMode(WALMode.FSYNC)
                        .setWalPath(workDir + WORK_SUB_DIR)
                        .setWalArchivePath(workDir + ARCHIVE_SUB_DIR)
                        .setFileIOFactory(new RandomAccessFileIOFactory())
                        .setMaxWalArchiveSize(DataStorageConfiguration.DFLT_WAL_ARCHIVE_MAX_SIZE * 10)
                );

                cfg.setEventStorageSpi(new NoopEventStorageSpi());

                // TODO: 10.11.2020 del
                if (log.isInfoEnabled()) {
                    try {
                        log.info("kirill=" + cfg + ", a=" + pdsFolderResolver().resolveFolders().folderName());
                    }
                    catch (IgniteCheckedException e) {
                        throw new RuntimeException(e);
                    }
                }

                return cfg;
            }

            @Override public GridInternalSubscriptionProcessor internalSubscriptionProcessor() {
                return new GridInternalSubscriptionProcessor(this);
            }

            @Override public GridEventStorageManager event() {
                return new GridEventStorageManager(this);
            }
        };

        IgniteWriteAheadLogManager walMgr = new FileWriteAheadLogManager(kctx);

        GridTestUtils.setFieldValue(walMgr, "serializerVer", serVer);

        GridCacheSharedContext<?, ?> ctx = new GridCacheSharedContext<>(
            kctx,
            null,
            null,
            null,
            null,
            walMgr,
            new WalStateManager(kctx),
            new GridCacheDatabaseSharedManager(kctx),
            null,
            null,
            null,
            null,
            new GridCacheIoManager(),
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        walMgr.start(ctx);

        walMgr.onActivate(kctx);

        walMgr.resumeLogging(null);

        RecordSerializer recordSerializer = new RecordSerializerFactoryImpl(ctx)
            .createSerializer(walMgr.serializerVersion());

        return new T2<>(walMgr, recordSerializer);
    }
}
