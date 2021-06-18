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
package org.apache.ignite.internal.processors.cache.persistence.wal;

import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;

import static org.apache.ignite.configuration.DataStorageConfiguration.HALF_MAX_WAL_ARCHIVE_SIZE;
import static org.apache.ignite.configuration.DataStorageConfiguration.UNLIMITED_WAL_ARCHIVE;
import static org.apache.ignite.internal.processors.cache.persistence.wal.FileWriteAheadLogManager.minWalArchiveSize;

/**
 * Class for testing {@link FileWriteAheadLogManager}.
 */
public class FileWriteAheadLogManagerSelfTest extends GridCommonAbstractTest {
    /**
     * Testing of {@link FileWriteAheadLogManager#minWalArchiveSize(DataStorageConfiguration)}.
     */
    @Test
    public void testGettingMinWalArchiveSizeFromConfiguration() {
        DataStorageConfiguration cfg = new DataStorageConfiguration();

        cfg.setMaxWalArchiveSize(UNLIMITED_WAL_ARCHIVE);

        for (long i : F.asList(10L, 20L, HALF_MAX_WAL_ARCHIVE_SIZE))
            assertEquals(UNLIMITED_WAL_ARCHIVE, minWalArchiveSize(cfg.setMinWalArchiveSize(i)));

        cfg.setMaxWalArchiveSize(100);

        for (long i : F.asList(10L, 20L))
            assertEquals(i, minWalArchiveSize(cfg.setMinWalArchiveSize(i)));

        assertEquals(50, minWalArchiveSize(cfg.setMinWalArchiveSize(HALF_MAX_WAL_ARCHIVE_SIZE)));
    }
}
