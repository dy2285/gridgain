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

package org.apache.ignite.internal.processors.cache.persistence.db.file;

import com.google.common.base.Strings;
import java.util.concurrent.TimeUnit;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cluster.ClusterState;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.apache.ignite.configuration.DataStorageConfiguration.DFLT_CHECKPOINT_THREADS;

/**
 * Puts data into grid, waits for checkpoint to start and then verifies data
 */
public class IgnitePdsCheckpointSimpleTest extends GridCommonAbstractTest {
    /** Checkpoint threads. */
    public int cpThreads = DFLT_CHECKPOINT_THREADS;

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(igniteInstanceName)
            .setDataStorageConfiguration(new DataStorageConfiguration()
                .setPageSize(4 * 1024)
                .setDefaultDataRegionConfiguration(new DataRegionConfiguration()
                    .setPersistenceEnabled(true))
                .setCheckpointFrequency(TimeUnit.SECONDS.toMillis(10)));

        if (cpThreads != DFLT_CHECKPOINT_THREADS) {
            cfg.getDataStorageConfiguration()
                .setCheckpointThreads(cpThreads);
        }

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        super.beforeTestsStarted();

        cleanPersistenceDir();
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        super.afterTest();

        stopAllGrids();
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testStartNodeWithDefaultCpThreads() throws Exception {
        checkCheckpointThreads();
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testStartNodeWithNonDefaultCpThreads() throws Exception {
        cpThreads = 10;

        checkCheckpointThreads();
    }

    /**
     * Checks that all checkpoint threads are present in JVM.
     *
     * @throws Exception If failed.
     */
    public void checkCheckpointThreads() throws Exception {
        IgniteEx ignite = startGrid(0);

        ignite.cluster().state(ClusterState.ACTIVE);

        IgniteCache<Object, Object> cache = ignite.getOrCreateCache("cache");

        cache.put(1, 1);

        forceCheckpoint();

        int dbCpThread = 0, ioCpRunner = 0, cpuCpRunner = 0;

        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.getName().contains("db-checkpoint-thread"))
                dbCpThread++;

            else if (t.getName().contains("checkpoint-runner-IO"))
                ioCpRunner++;

            else if (t.getName().contains("checkpoint-runner-cpu"))
                cpuCpRunner++;
        }

        assertEquals(1, dbCpThread);
        assertEquals(cpThreads, ioCpRunner);
        assertEquals(cpThreads, cpuCpRunner);
    }

    /**
     * Checks if same data can be loaded after checkpoint.
     *
     * @throws Exception if failed.
     */
    @Test
    public void testRecoveryAfterCpEnd() throws Exception {
        IgniteEx ignite = startGrid(0);

        ignite.cluster().state(ClusterState.ACTIVE);

        IgniteCache<Object, Object> cache = ignite.getOrCreateCache("cache");

        for (int i = 0; i < 10000; i++)
            cache.put(i, valueWithRedundancyForKey(i));

        ignite.context().cache().context().database().waitForCheckpoint("test");

        stopAllGrids();

        IgniteEx igniteRestart = startGrid(0);

        igniteRestart.cluster().state(ClusterState.ACTIVE);

        IgniteCache<Object, Object> cacheRestart = igniteRestart.getOrCreateCache("cache");

        for (int i = 0; i < 10000; i++)
            assertEquals(valueWithRedundancyForKey(i), cacheRestart.get(i));

        stopAllGrids();
    }

    /**
     * @param i key.
     * @return value with extra data, which allows to verify
     */
    private @NotNull String valueWithRedundancyForKey(int i) {
        return Strings.repeat(Integer.toString(i), 10);
    }
}
