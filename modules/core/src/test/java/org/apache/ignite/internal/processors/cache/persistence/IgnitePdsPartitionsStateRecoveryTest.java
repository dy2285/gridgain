/*
 *                   GridGain Community Edition Licensing
 *                   Copyright 2019 GridGain Systems, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License") modified with Commons Clause
 * Restriction; you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * Commons Clause Restriction
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including without
 * limitation fees for hosting or consulting/ support services related to the Software), a product or
 * service whose value derives, entirely or substantially, from the functionality of the Software.
 * Any license notice or attribution required by the License must also include this Commons Clause
 * License Condition notice.
 *
 * For purposes of the clause above, the “Licensor” is Copyright 2019 GridGain Systems, Inc.,
 * the “License” is the Apache License, Version 2.0, and the Software is the GridGain Community
 * Edition software provided with this notice.
 */

package org.apache.ignite.internal.processors.cache.persistence;

import java.util.Arrays;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheRebalanceMode;
import org.apache.ignite.cache.CacheWriteSynchronizationMode;
import org.apache.ignite.cache.affinity.rendezvous.RendezvousAffinityFunction;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.WALMode;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.internal.processors.cache.distributed.dht.topology.GridDhtPartitionTopology;
import org.apache.ignite.testframework.MvccFeatureChecker;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 *
 */
@RunWith(JUnit4.class)
public class IgnitePdsPartitionsStateRecoveryTest extends GridCommonAbstractTest {
    /** Partitions count. */
    private static final int PARTS_CNT = 32;

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(igniteInstanceName);

        cfg.setConsistentId(igniteInstanceName);

        DataStorageConfiguration dsCfg = new DataStorageConfiguration()
            .setWalMode(WALMode.LOG_ONLY)
            .setWalSegmentSize(16 * 1024 * 1024)
            .setCheckpointFrequency(20 * 60 * 1000)
            .setDefaultDataRegionConfiguration(
                new DataRegionConfiguration()
                    .setMaxSize(512 * 1024 * 1024)
                    .setPersistenceEnabled(true)
            );

        cfg.setDataStorageConfiguration(dsCfg);

        CacheConfiguration ccfg = defaultCacheConfiguration()
            .setBackups(0)
            .setRebalanceMode(CacheRebalanceMode.NONE) // Disable rebalance to prevent owning MOVING partitions.
            .setWriteSynchronizationMode(CacheWriteSynchronizationMode.FULL_SYNC)
            .setAffinity(new RendezvousAffinityFunction(false, PARTS_CNT));

        cfg.setCacheConfiguration(ccfg);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        stopAllGrids();

        cleanPersistenceDir();

        System.setProperty(GridCacheDatabaseSharedManager.IGNITE_PDS_SKIP_CHECKPOINT_ON_NODE_STOP, "true");
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();

        cleanPersistenceDir();

        System.clearProperty(GridCacheDatabaseSharedManager.IGNITE_PDS_SKIP_CHECKPOINT_ON_NODE_STOP);
    }

    /**
     * Test checks that partition state is recovered properly if last checkpoint was skipped and there are logical updates to apply.
     *
     * @throws Exception If failed.
     */
    @Test
    public void testPartitionsStateConsistencyAfterRecovery() throws Exception {
        IgniteEx ignite = startGrid(0);

        ignite.cluster().active(true);

        IgniteCache<Object, Object> cache = ignite.cache(DEFAULT_CACHE_NAME);

        for (int key = 0; key < 4096; key++)
            cache.put(key, key);

        forceCheckpoint();

        for (int key = 0; key < 4096; key++) {
            int[] payload = new int[4096];
            Arrays.fill(payload, key);

            cache.put(key, payload);
        }

        GridDhtPartitionTopology topology = ignite.cachex(DEFAULT_CACHE_NAME).context().topology();

        Assert.assertFalse(topology.hasMovingPartitions());

        log.info("Stopping grid...");

        stopGrid(0);

        ignite = startGrid(0);

        awaitPartitionMapExchange();

        topology = ignite.cachex(DEFAULT_CACHE_NAME).context().topology();

        Assert.assertFalse("Node restored moving partitions after join to topology.", topology.hasMovingPartitions());
    }

    /**
     * Test checks that partition state is recovered properly if only logical updates exist.
     *
     * @throws Exception If failed.
     */
    @Test
    public void testPartitionsStateConsistencyAfterRecoveryNoCheckpoints() throws Exception {
        if (MvccFeatureChecker.forcedMvcc())
            fail("https://issues.apache.org/jira/browse/IGNITE-10603");

        IgniteEx ignite = startGrid(0);

        ignite.cluster().active(true);

        IgniteCache<Object, Object> cache = ignite.cache(DEFAULT_CACHE_NAME);

        forceCheckpoint();

        for (int key = 0; key < 4096; key++) {
            int[] payload = new int[4096];
            Arrays.fill(payload, key);

            cache.put(key, payload);
        }

        GridDhtPartitionTopology topology = ignite.cachex(DEFAULT_CACHE_NAME).context().topology();

        Assert.assertFalse(topology.hasMovingPartitions());

        log.info("Stopping grid...");

        stopGrid(0);

        ignite = startGrid(0);

        awaitPartitionMapExchange();

        topology = ignite.cachex(DEFAULT_CACHE_NAME).context().topology();

        Assert.assertFalse("Node restored moving partitions after join to topology.", topology.hasMovingPartitions());
    }
}
