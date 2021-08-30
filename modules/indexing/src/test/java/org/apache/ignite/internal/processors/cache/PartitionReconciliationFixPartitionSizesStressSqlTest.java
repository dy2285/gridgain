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

package org.apache.ignite.internal.processors.cache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.internal.IgniteInternalFuture;
import org.apache.ignite.internal.processors.cache.checker.processor.PartitionReconciliationFixPartitionSizesStressAbstractTest;
import org.apache.ignite.internal.processors.cache.verify.ReconciliationType;
import org.apache.ignite.internal.visor.checker.VisorPartitionReconciliationTaskArg;
import org.apache.ignite.testframework.GridTestUtils;
import org.junit.Test;

import static org.apache.ignite.cache.CacheMode.REPLICATED;
import static org.apache.ignite.internal.processors.cache.verify.ReconciliationType.CACHE_SIZE_CONSISTENCY;
import static org.apache.ignite.internal.processors.cache.verify.ReconciliationType.DATA_CONSISTENCY;

/**
 * Tests partition reconciliation of sizes with various cache configurations and sql.
 */
public class PartitionReconciliationFixPartitionSizesStressSqlTest extends PartitionReconciliationFixPartitionSizesStressAbstractTest {
    /**
     * <ul>
     * <li>Start nodes.</li>
     * <li>Create sql cache.</li>
     * <li>Load some data.</li>
     * <li>Break cache size.</li>
     * <li>Start load thread.</li>
     * <li>Do size reconciliation.</li>
     * <li>Stop load thread.</li>
     * <li>Check size of primary/backup partitions in cluster.</li>
     * </ul>
     */
    @Test
    public void test() throws Exception {
        Set<ReconciliationType> reconciliationTypes = new HashSet<>();

        reconciliationTypes.add(CACHE_SIZE_CONSISTENCY);

        if (rnd.nextBoolean())
            reconciliationTypes.add(DATA_CONSISTENCY);

        log.info(">>> Reconciliation types: " + reconciliationTypes);

        ig = startGrids(nodesCnt);

        client = startClientGrid(nodesCnt);

        ig.cluster().active(true);

        String cacheName = DEFAULT_CACHE_NAME + 0;

        IgniteCache<Object, Object> cache = client.createCache(
            getCacheConfig(cacheName, cacheAtomicityMode, cacheMode, backupCnt, partCnt, cacheGrp)
        );

        String tblName = "testtable";
        String sqlCacheName = "SQL_PUBLIC_TESTTABLE";

        cache.query(
            new SqlFieldsQuery("create table " + tblName + " (id integer primary key, p integer) with \"backups=" +
                backupCnt + ", template=" + cacheMode.name() + ", atomicity=" + cacheAtomicityMode + "\"")
        ).getAll();

        List<Long> keysInTable = new ArrayList<>(endKey);

        for (long i = startKey; i < endKey; i++) {
            i += 1;

            if (i < endKey) {
                if (!keysInTable.contains(i)) {
                    cache.query(new SqlFieldsQuery("insert into " + tblName + "(id, p) values (" + i + ", " + i + ")")).getAll();

                    keysInTable.add(i);
                }
            }
        }

        IgniteCache<Object, Object> sqlCache = client.cache(sqlCacheName);

        int sqlStartSize = sqlCache.size();

        List<IgniteEx> grids = new ArrayList<>();

        for (int i = 0; i < nodesCnt; i++)
            grids.add(grid(i));

        breakCacheSizes(grids, new HashSet<>(Collections.singletonList(sqlCacheName)));

        assertFalse(sqlCache.size() == sqlStartSize);

        VisorPartitionReconciliationTaskArg.Builder builder = new VisorPartitionReconciliationTaskArg.Builder();
        builder.repair(true);
        builder.parallelism(reconParallelism);
        builder.caches(new HashSet<>(Arrays.asList(sqlCacheName)));
        builder.batchSize(reconBatchSize);
        builder.reconTypes(new HashSet(reconciliationTypes));

        reconResult = new AtomicReference<>();

        IgniteInternalFuture loadFut = startAsyncSqlLoad(reconResult, client, sqlCache, tblName, keysInTable, startKey, endKey);

        GridTestUtils.runMultiThreadedAsync(() -> {
            reconResult.set(partitionReconciliation(client, builder));
        }, 1, "reconciliation");

        GridTestUtils.waitForCondition(() -> reconResult.get() != null, 120_000);

        List<String> errors = reconResult.get().errors();

        assertTrue(errors.isEmpty());

        loadFut.get();

        for (long i = startKey; i < endKey; i++) {
            if (!keysInTable.contains(i))
                cache.query(new SqlFieldsQuery("insert into " + tblName + "(id, p) values (" + i + ", " + i + ")")).getAll();
        }

        awaitPartitionMapExchange();

        long allKeysCountForCacheGroup;
        long allKeysCountForCache;

        allKeysCountForCacheGroup = 0;
        allKeysCountForCache = 0;

        for (int i = 0; i < nodesCnt; i++) {
            long i0 = getFullPartitionsSizeForCacheGroup(grid(i), sqlCacheName);
            allKeysCountForCacheGroup += i0;

            long i1 = getPartitionsSizeForCache(grid(i), sqlCacheName);
            allKeysCountForCache += i1;
        }

        assertEquals(endKey, client.cache(sqlCacheName).size());

        if (cacheMode == REPLICATED) {
            assertEquals((long)endKey * nodesCnt, allKeysCountForCacheGroup);
            assertEquals((long)endKey * nodesCnt, allKeysCountForCache);
        }
        else {
            assertEquals((long)endKey * (1 + backupCnt), allKeysCountForCacheGroup);
            assertEquals((long)endKey * (1 + backupCnt), allKeysCountForCache);
        }
    }
}