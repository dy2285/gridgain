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

package org.apache.ignite.internal.processors.cache;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.CacheRebalanceMode;
import org.apache.ignite.cache.affinity.Affinity;
import org.apache.ignite.cache.query.FieldsQueryCursor;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.cache.query.annotations.QuerySqlField;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.util.typedef.G;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Test to lazy query partitions has not been released too early.
 */
@RunWith(JUnit4.class)
public class GridCacheLazyQueryPartitionsReleaseTest extends GridCommonAbstractTest {
    /** Cache name */
    private static final String PERSON_CACHE = "person";

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(igniteInstanceName);

        CacheConfiguration ccfg = defaultCacheConfiguration()
            .setCacheMode(CacheMode.PARTITIONED)
            .setAtomicityMode(CacheAtomicityMode.ATOMIC)
            .setRebalanceMode(CacheRebalanceMode.ASYNC)
            .setRebalanceBatchSize(1000)
            .setRebalanceDelay(0)
            .setName(PERSON_CACHE)
            .setIndexedTypes(Integer.class, Person.class);

        cfg.setCacheConfiguration(ccfg);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();
    }

    /**
     * Lazy query release partitions test.
     *
     * @throws Exception If failed.
     */
    @Test
    public void testLazyQueryPartitionsRelease() throws Exception {
        Ignite node1 = startGrid(0);

        IgniteCache<Integer, Person> cache = node1.cache(PERSON_CACHE);

        cache.clear();

        Affinity<Integer> aff = node1.affinity(PERSON_CACHE);

        int partsFilled = fillAllPartitions(cache, aff);

        SqlFieldsQuery qry = new SqlFieldsQuery("select name, age from person")
            .setLazy(true)
            .setPageSize(1);

        FieldsQueryCursor<List<?>> qryCursor = cache.query(qry);

        Iterator<List<?>> it = qryCursor.iterator();

        int resCntr = 0;

        if (it.hasNext()) {
            it.next();

            resCntr++;
        } else
            fail("No query results.");

        startGrid(1);

        for (Ignite ig : G.allGrids())
            ig.cache(PERSON_CACHE).rebalance().get();

        while (it.hasNext()) {
            it.next();

            resCntr++;
        }

        assertEquals("Wrong result set size", partsFilled, resCntr);
    }

    /**
     * Lazy query release partitions on cursor close test.
     *
     * @throws Exception If failed.
     */
    @Test
    public void testLazyQueryPartitionsReleaseOnClose() throws Exception {
        Ignite node1 = startGrid(0);

        IgniteCache<Integer, Person> cache = node1.cache(PERSON_CACHE);

        cache.clear();

        Affinity<Integer> aff = node1.affinity(PERSON_CACHE);

        int partsFilled = fillAllPartitions(cache, aff);

        SqlFieldsQuery qry = new SqlFieldsQuery("select name, age from person")
            .setLazy(true)
            .setPageSize(1);

        FieldsQueryCursor<List<?>> qryCursor = cache.query(qry);

        Iterator<List<?>> it = qryCursor.iterator();

        if (it.hasNext())
            it.next();
        else
            fail("No query results.");

        startGrid(1);

        // Close cursor. Partitions should be released now.
        qryCursor.close();

        for (Ignite ig : G.allGrids())
            ig.cache(PERSON_CACHE).rebalance().get();

        assertEquals("Wrong result set size", partsFilled, cache.query(qry).getAll().size());
    }

    /**
     * Fills all partitions in the cache with a single data entry.
     *
     * @param cache - Cache to fill all partition to.
     * @param aff Affinity.
     * @return Number of filled partitions
     */
    private int fillAllPartitions(IgniteCache<Integer, Person> cache, Affinity<Integer> aff) {
        int partsCnt = aff.partitions();

        Set<Integer> emptyParts = new HashSet<>(partsCnt);

        for (int i = 0; i < partsCnt; i++)
            emptyParts.add(i);

        int cntr = 0;

        while (!emptyParts.isEmpty()) {
            int part = aff.partition(cntr++);

            if (emptyParts.remove(part))
                cache.put(cntr, new Person("p_" + cntr, cntr));

            if (cntr > 100_000)
                fail("Failed to fill all partitions");
        }

        return partsCnt;
    }

    /**
     * Dummy class for testing.
     */
    public static class Person implements Serializable {
        /** Name. */
        @QuerySqlField
        private String name;

        /** Age. */
        @QuerySqlField
        private int age;

        /**
         * @param name Name.
         * @param age Age.
         */
        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        /**
         *
         */
        public String getName() {
            return name;
        }

        /**
         * @param name Name.
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         *
         */
        public int getAge() {
            return age;
        }

        /**
         * @param age Age.
         */
        public void setAge(int age) {
            this.age = age;
        }
    }
}
