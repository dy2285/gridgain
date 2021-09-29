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

package org.apache.ignite.internal.processors.cache.transactions;

import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.internal.IgniteInternalFuture;
import org.apache.ignite.internal.TestRecordingCommunicationSpi;
import org.apache.ignite.internal.processors.cache.distributed.dht.preloader.GridDhtPartitionsSingleMessage;
import org.apache.ignite.internal.processors.cache.distributed.near.GridNearTxPrepareRequest;
import org.apache.ignite.testframework.GridTestUtils;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;

/**
 * The test starts an implicit transaction during the cache is stopping.
 * The transaction has to be completed, and the cache is stopped.
 */
public class StartImplicitlyTxOnStopCacheTest extends GridCommonAbstractTest {
    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(igniteInstanceName)
            .setConsistentId(igniteInstanceName)
            .setCommunicationSpi(new TestRecordingCommunicationSpi())
            .setCacheConfiguration(new CacheConfiguration(DEFAULT_CACHE_NAME)
                .setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL));

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        super.beforeTest();
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
    public void test() throws Exception {
        startGrid(0);

        IgniteEx client = startClientGrid("client");

        IgniteCache<Object, Object> cache = client.cache(DEFAULT_CACHE_NAME);

        for (int i = 0; i < 100; i++)
            cache.put(i, i);

        TestRecordingCommunicationSpi commSpiClient1 = TestRecordingCommunicationSpi.spi(client);

        commSpiClient1.blockMessages(GridNearTxPrepareRequest.class, getTestIgniteInstanceName(0));

        commSpiClient1.record((node, msg) -> msg instanceof GridDhtPartitionsSingleMessage);

        IgniteInternalFuture runTxFut = GridTestUtils.runAsync(() -> cache.put(100, 100));

        IgniteInternalFuture destroyCacheFut = GridTestUtils.runAsync(() ->
            client.destroyCache(DEFAULT_CACHE_NAME));

        commSpiClient1.waitForBlocked();

        commSpiClient1.waitForRecorded();

        commSpiClient1.stopBlock();

        assertTrue(GridTestUtils.waitForCondition(destroyCacheFut::isDone, 10_000));

        assertTrue(GridTestUtils.waitForCondition(runTxFut::isDone, 10_000));

        assertNull(client.cache(DEFAULT_CACHE_NAME));
    }
}
