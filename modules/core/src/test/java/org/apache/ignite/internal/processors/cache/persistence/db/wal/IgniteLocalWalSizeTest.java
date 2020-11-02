/*
 * Copyright 2020 GridGain Systems, Inc. and Contributors.
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cluster.ClusterState;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.internal.processors.cache.persistence.wal.FileDescriptor;
import org.apache.ignite.internal.processors.cache.persistence.wal.FileWriteAheadLogManager;
import org.apache.ignite.internal.processors.cache.persistence.wal.filehandle.FileWriteHandle;
import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import static org.apache.ignite.internal.processors.cache.persistence.file.FilePageStoreManager.ZIP_SUFFIX;
import static org.apache.ignite.internal.processors.cache.persistence.wal.FileDescriptor.fileName;
import static org.apache.ignite.internal.processors.cache.persistence.wal.FileWriteAheadLogManager.WAL_SEGMENT_COMPACTED_OR_RAW_FILE_FILTER;
import static org.apache.ignite.internal.processors.cache.persistence.wal.FileWriteAheadLogManager.isSegmentFileName;
import static org.apache.ignite.testframework.GridTestUtils.getFieldValue;

/**
 * Class for testing local size of WAL.
 */
public class IgniteLocalWalSizeTest extends GridCommonAbstractTest {
    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        super.beforeTest();

        stopAllGrids();
        cleanPersistenceDir();
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        super.afterTest();

        stopAllGrids();
        cleanPersistenceDir();
    }

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String gridName) throws Exception {
        return super.getConfiguration(gridName)
            .setCacheConfiguration(new CacheConfiguration<>(DEFAULT_CACHE_NAME))
            .setDataStorageConfiguration(
                new DataStorageConfiguration()
                    .setWalSegments(5)
                    .setWalSegmentSize((int)U.MB)
                    .setDefaultDataRegionConfiguration(new DataRegionConfiguration().setPersistenceEnabled(true))
            );
    }

    /**
     * Checking correctness of working with local segment sizes for case: archiving only.
     *
     * @throws Exception If failed.
     */
    @Test
    public void testLocalSegmentSizesArchiveOnly() throws Exception {
        checkLocalSegmentSizesForOneNode(null);
    }

    /**
     * Checking correctness of working with local segment sizes for case: archiving and compression.
     *
     * @throws Exception If failed.
     */
    @Test
    public void testLocalSegmentSizesArchiveAndCompression() throws Exception {
        checkLocalSegmentSizesForOneNode(cfg -> cfg.getDataStorageConfiguration().setWalCompactionEnabled(true));
    }

    /**
     * Checking correctness of working with local segment sizes for case: without archiving.
     *
     * @throws Exception If failed.
     */
    @Test
    public void testLocalSegmentSizesWithoutArchive() throws Exception {
        checkLocalSegmentSizesForOneNode(cfg -> {
            DataStorageConfiguration dsCfg = cfg.getDataStorageConfiguration();
            dsCfg.setWalArchivePath(dsCfg.getWalPath());
        });
    }

    /**
     * Checking correctness of working with local segment sizes for case: without archiving and with compression.
     *
     * @throws Exception If failed.
     */
    @Test
    public void testLocalSegmentSizesWithoutArchiveWithCompression() throws Exception {
        checkLocalSegmentSizesForOneNode(cfg -> {
            DataStorageConfiguration dsCfg = cfg.getDataStorageConfiguration();
            dsCfg.setWalArchivePath(dsCfg.getWalPath()).setWalCompactionEnabled(true);
        });
    }

    /**
     * Checking whether segment file name is checked correctly.
     *
     * @throws Exception If failed.
     */
    @Test
    public void testSegmentFileName() throws Exception {
        Arrays.asList(null, "", "1", "wal", fileName(0) + "1", fileName(1).replace(".wal", ".wa"))
            .forEach(s -> assertFalse(s, isSegmentFileName(s)));

        IntStream.range(0, 10)
            .mapToObj(FileDescriptor::fileName)
            .forEach(fn -> assertTrue(fn, isSegmentFileName(fn) && isSegmentFileName(fn + ZIP_SUFFIX)));
    }

    /**
     * Checks whether local segment sizes are working correctly for a single node after loading and restarting.
     *
     * @param cfgUpdater Configuration updater.
     * @throws Exception If failed.
     */
    private void checkLocalSegmentSizesForOneNode(
        @Nullable Consumer<IgniteConfiguration> cfgUpdater
    ) throws Exception {
        IgniteConfiguration cfg = getConfiguration(getTestIgniteInstanceName(0));

        if (cfgUpdater != null)
            cfgUpdater.accept(cfg);

        IgniteEx n = startGrid(cfg);
        n.cluster().state(ClusterState.ACTIVE);

        awaitPartitionMapExchange();

        IgniteCache<Object, Object> c = n.getOrCreateCache(DEFAULT_CACHE_NAME);
        IntStream.range(0, 10_000).forEach(i -> c.put(i, i));

        forceCheckpoint();
        checkLocalSegmentSizes(n);

        stopGrid(cfg.getIgniteInstanceName());
        awaitPartitionMapExchange();

        cfg = getConfiguration(cfg.getIgniteInstanceName());

        if (cfgUpdater != null)
            cfgUpdater.accept(cfg);

        // To avoid a race between compressor and getting the segment sizes.
        if (cfg.getDataStorageConfiguration().isWalCompactionEnabled())
            cfg.getDataStorageConfiguration().setWalCompactionEnabled(false);

        n = startGrid(cfg);
        awaitPartitionMapExchange();

        checkLocalSegmentSizes(n);
    }

    /**
     * Check that local segment sizes in the memory and actual match.
     *
     * @param n Node.
     */
    private void checkLocalSegmentSizes(IgniteEx n) {
        FileWriteAheadLogManager wal = (FileWriteAheadLogManager)n.context().cache().context().wal();

        File walWorkDir = getFieldValue(wal, "walWorkDir");
        File walArchiveDir = getFieldValue(wal, "walArchiveDir");

        Map<Long, Long> expSegmentSize = new HashMap<>();

        F.asList(walArchiveDir.listFiles(WAL_SEGMENT_COMPACTED_OR_RAW_FILE_FILTER))
            .stream()
            .map(FileDescriptor::new)
            .forEach(fd -> {
                if (fd.isCompressed())
                    expSegmentSize.put(fd.idx(), fd.file().length());
                else
                    expSegmentSize.putIfAbsent(fd.idx(), fd.file().length());
            });

        FileWriteHandle currHnd = getFieldValue(wal, "currHnd");

        if (!walArchiveDir.equals(walWorkDir)) {
            long absIdx = currHnd.getSegmentId();
            int segments = n.configuration().getDataStorageConfiguration().getWalSegments();

            for (long i = absIdx - (absIdx % segments); i <= absIdx; i++)
                expSegmentSize.putIfAbsent(i, new File(walWorkDir, fileName(i % segments)).length());
        }

        assertEquals(currHnd.getSegmentId() + 1, expSegmentSize.size());

        Map<Long, Long> segmentSize = getFieldValue(wal, "segmentSize");
        assertEquals(expSegmentSize.size(), segmentSize.size());

        expSegmentSize.forEach((idx, size) -> {
            assertEquals(idx.toString(), size, segmentSize.get(idx));
            assertEquals(idx.toString(), size.longValue(), wal.segmentSize(idx));
        });

        assertEquals(0, wal.segmentSize(currHnd.getSegmentId() + 1));
    }
}