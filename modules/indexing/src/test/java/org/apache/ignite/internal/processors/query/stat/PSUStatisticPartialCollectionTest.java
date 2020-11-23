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
package org.apache.ignite.internal.processors.query.stat;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCheckedException;
import org.junit.Test;

/**
 * Planner statistics usage test: partial statistics collection (by set of columns) tests.
 */
public class PSUStatisticPartialCollectionTest extends StatisticsAbstractTest {
    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        Ignite node = startGridsMultiThreaded(1);

        node.getOrCreateCache(DEFAULT_CACHE_NAME);
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        runSql("DROP TABLE IF EXISTS TBL_SELECT");

        runSql("CREATE TABLE TBL_SELECT (ID INT PRIMARY KEY, lo_select int, med_select int, hi_select int)");

        runSql("CREATE INDEX TBL_SELECT_LO_IDX ON TBL_SELECT(lo_select)");
        runSql("CREATE INDEX TBL_SELECT_MED_IDX ON TBL_SELECT(med_select)");
        runSql("CREATE INDEX TBL_SELECT_HI_IDX ON TBL_SELECT(hi_select)");

        for (int i = 0; i < 1000; i++)
            runSql(String.format("insert into tbl_select(id, lo_select, med_select, hi_select) values(%d, %d, %d, %d)",
                    i, i % 10, i % 100, i % 1000));

        updateStatistics("tbl_select");
    }

    /**
     * Test that partial statistics collection work properly:
     *
     * Prepare:
     * 1) create table with three columns with lo, med and hi selectivity
     * 2) collect statistics
     *
     * Test:
     * 1) select with equals clauses by lo and med selectivity columns and test that med_idx used
     * 2) update data in lo selectivity so it will have hi selectivity
     * 3) collect statistics by hi selectivity column
     * 4) test that select still use med idx because of outdated statistics
     * 5) collect statistics by lo selectivity column
     * 6) test that select start to use lo_idx because now it has better selectivity than med one
     */
    @Test
    public void compareSelectWithIntConditions() throws IgniteCheckedException {
        String[][] noHints = new String[1][];
        String lo_med_select = "select * from TBL_SELECT i1 where lo_select = %d and med_select = %d";
        checkOptimalPlanChosenForDifferentIndexes(grid(0), new String[]{"TBL_SELECT_MED_IDX"},
                String.format(lo_med_select, 5, 5), noHints);

        runSql("UPDATE TBL_SELECT SET lo_select = hi_select");

        checkOptimalPlanChosenForDifferentIndexes(grid(0), new String[]{"TBL_SELECT_MED_IDX"},
                String.format(lo_med_select, 6, 6), noHints);

        IgniteStatisticsManager statsManager = grid(0).context().query().getIndexing().statsManager();
        statsManager.collectObjectStatistics("PUBLIC", "TBL_SELECT", "HI_SELECT");

        checkOptimalPlanChosenForDifferentIndexes(grid(0), new String[]{"TBL_SELECT_MED_IDX"},
                String.format(lo_med_select, 7, 7), noHints);

        statsManager.collectObjectStatistics("PUBLIC", "TBL_SELECT", "LO_SELECT");

        checkOptimalPlanChosenForDifferentIndexes(grid(0), new String[]{"TBL_SELECT_LO_IDX"},
                String.format(lo_med_select, 8, 8), noHints);
    }
}