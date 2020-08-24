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

package org.apache.ignite.internal.processors.cache;

import java.net.URL;
import java.util.Random;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheInterceptor;
import org.apache.ignite.cache.affinity.rendezvous.RendezvousAffinityFunction;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.internal.util.typedef.G;
import org.apache.ignite.testframework.GridTestExternalClassLoader;
import org.apache.ignite.testframework.config.GridTestProperties;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;

import static java.lang.Math.abs;

/**
 * Checks that exception does not happen when a cache interceptor absences in a client side.
 */
public class NoPresentCacheInterceptorOnClientTest extends GridCommonAbstractTest {
    /** Name of transactional cache. */
    private static final String TX_DEFAULT_CACHE_NAME = "tx_" + DEFAULT_CACHE_NAME;

    /** Quantity of entries which will load. */
    private static final int ENTRIES_TO_LOAD = 100;

    /** Cache interceptor class name. */
    private static final String INTERCEPTOR_CLASS = "org.apache.ignite.tests.p2p.cache.OddEvenCacheInterceptor";

    /** Test class loader. */
    private ClassLoader testClassLoader;

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(igniteInstanceName);

        if (getTestIgniteInstanceName(0).equals(igniteInstanceName))
            cfg.setClassLoader(testClassLoader);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        super.beforeTest();

        stopAllGrids();

        testClassLoader = new GridTestExternalClassLoader(new URL[] {
            new URL(GridTestProperties.getProperty("p2p.uri.cls"))});
    }

    /**
     * Test starts two caches and checks (both with interceptor configured) them that they work on a client side.
     *
     * @throws Exception If failed.
     */
    @Test
    public void test() throws Exception {
        IgniteEx ignite0 = startGrid(0);

        IgniteCache<Integer, Integer> cache = ignite0.getOrCreateCache(generateCacheConfiguration(
            DEFAULT_CACHE_NAME,
            CacheAtomicityMode.ATOMIC
        ));

        IgniteCache<Integer, Integer> txCache = ignite0.getOrCreateCache(generateCacheConfiguration(
            TX_DEFAULT_CACHE_NAME,
            CacheAtomicityMode.TRANSACTIONAL
        ));

        checkCache(cache);
        checkCache(txCache);

        IgniteEx client = startClientGrid(1);

        IgniteCache<Integer, Integer> cacheOnClient = client.cache(DEFAULT_CACHE_NAME);
        IgniteCache<Integer, Integer> txCacheOnClient = client.cache(TX_DEFAULT_CACHE_NAME);

        checkCache(cacheOnClient);
        checkCache(txCacheOnClient);
    }

    /**
     * Generates a cache configuration with an interceptor.
     *
     * @param name Cache name.
     * @param mode Atomic mode.
     * @return Cache configuration.
     * @throws Exception If failed.
     */
    private CacheConfiguration<Integer, Integer> generateCacheConfiguration(String name, CacheAtomicityMode mode) throws Exception {
        return new CacheConfiguration<Integer, Integer>(name)
            .setAffinity(new RendezvousAffinityFunction(false, 64))
            .setAtomicityMode(mode)
            .setInterceptor((CacheInterceptor<Integer, Integer>)testClassLoader.loadClass(INTERCEPTOR_CLASS).newInstance());
    }

    /**
     * Checks a work of caches by loading entries and checking values ​​from cache.
     *
     * @param cache Ignite cache.
     * @throws Exception If failed.
     */
    private void checkCache(IgniteCache<Integer, Integer> cache) throws Exception {
        CacheAtomicityMode atomicityMode = cache.getConfiguration(CacheConfiguration.class).getAtomicityMode();

        if (atomicityMode == CacheAtomicityMode.TRANSACTIONAL) {
            Ignite ignite = G.ignite(cacheFromCtx(cache).context().igniteInstanceName());

            doInTransaction(ignite, () -> {
                loadCache(cache);

                return null;
            });
        }
        else
            loadCache(cache);

        for (int i = 0; i < ENTRIES_TO_LOAD; i++)
            assertTrue(abs(i % 2) == abs(cache.get(i) % 2));
    }

    /**
     * Loads {@see ENTRIES_TO_LOAD} rows to the cache specified.
     *
     * @param cache Ignite cache.
     */
    private void loadCache(IgniteCache<Integer, Integer> cache) {
        Random rand = new Random();

        for (int i = 0; i < ENTRIES_TO_LOAD; i++)
            cache.put(i, rand.nextInt());
    }
}
