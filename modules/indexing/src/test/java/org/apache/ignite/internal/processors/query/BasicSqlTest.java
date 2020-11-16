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

package org.apache.ignite.internal.processors.query;

import java.util.ArrayList;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.ignite.cache.query.FieldsQueryCursor;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.internal.processors.cache.index.AbstractIndexingCommonTest;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Basic simple tests for SQL.
 */
@RunWith(Parameterized.class)
public class BasicSqlTest extends AbstractIndexingCommonTest {
    /** */
    private static IgniteEx cli;

    /** Repair algorithm. */
    @Parameterized.Parameter(0)
    public boolean client;

    /**
     *
     */
    @Parameterized.Parameters(name = "client = {0}")
    public static List<Object[]> parameters() {
        ArrayList<Object[]> params = new ArrayList<>();

        params.add(new Object[] {true});
        params.add(new Object[] {false});

        return params;
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        stopAllGrids();

        super.afterTestsStopped();
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        super.beforeTestsStarted();

        startGrids(2);
        cli = startClientGrid(2);
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        super.afterTest();

        for (String cacheName : grid(0).cacheNames())
            grid(0).cache(cacheName).destroy();
    }

    /**
     * Check correct split sort expression that contains CAST function.

     * Steps:
     * - Creates test table and fill test data.
     * - Executes query that contains sort expression with CAST function.
     * - The query must be executed successfully.
     * - Checks query results.
     */
    @Test
    public void testSplitCastFunctionInSortExpression() {
        sql("CREATE TABLE Person (ID INTEGER PRIMARY KEY, NAME VARCHAR(100))");
        sql("INSERT INTO Person (ID, NAME) VALUES (3, 'Emma'), (2, 'Ann'), (1, 'Ed')");

        List<List<?>> res = sql("SELECT NAME, ID FROM Person ORDER BY CAST(ID AS LONG)").getAll();

        assertEquals(3, res.size());
        assertEquals("Ed", res.get(0).get(0));
        assertEquals("Ann", res.get(1).get(0));
        assertEquals("Emma", res.get(2).get(0));
    }

    /**
     */
    @Test
    public void testChooseCorrectIndexByOrderByExpression() {
        sql("CREATE TABLE TEST (ID INT PRIMARY KEY, VAL0 INT, VAL1 INT, VAL2 VARCHAR)");
        sql("CREATE INDEX IDX_VAL0 ON TEST(VAL0)");
        sql("CREATE INDEX IDX_VAL0_VAL1 ON TEST(VAL0, VAL1)");

        String plan = null;
        plan = (String)executeOnSrv(new SqlFieldsQuery(
            "EXPLAIN SELECT VAL0, VAL1, VAL2 FROM TEST " +
            "WHERE VAL0 = 0 " +
            "ORDER BY VAL0, VAL1").setLocal(true)
        ).getAll().get(0).get(0);

        assertTrue("Unexpected plan: " + plan, plan.contains("IDX_VAL0_VAL1"));

        plan = (String)executeOnSrv(new SqlFieldsQuery(
            "EXPLAIN SELECT VAL0, VAL1, VAL2 FROM TEST " +
            "WHERE VAL0 = 0 " +
            "ORDER BY 1, 2").setLocal(true)
        ).getAll().get(0).get(0);

        assertTrue("Unexpected plan: " + plan, plan.contains("IDX_VAL0_VAL1"));

        plan = (String)executeOnSrv(new SqlFieldsQuery(
            "EXPLAIN SELECT VAL0, VAL1, VAL2 FROM TEST " +
                "WHERE VAL0 = 0 " +
                "ORDER BY VAL0, VAL1")
        ).getAll().get(0).get(0);

        assertTrue("Unexpected plan: " + plan, plan.contains("IDX_VAL0_VAL1"));
    }

    /**
     */
    @Test
    public void testIntervalOperation() {
        sql("CREATE TABLE TEST (ID INT PRIMARY KEY, VAL_INT INT, VAL_TS INT)");
        sql("CREATE INDEX IDX_VAL_TS ON TEST(VAL_TS)");

        int rows = 10;
        for (int i = 0; i < rows; ++i) {
            sql("INSERT INTO TEST (ID, VAL_INT, VAL_TS) VALUES " +
                    "(?, ?, TRUNCATE(TIMESTAMP '2015-12-31 23:59:59') - CAST(TRUNC(?) AS TIMESTAMP))",
                i, i, new Date(2015 - 1900, 11, 31 - i, 12, i, i));
        }

        List<List<?>> res = sql("SELECT ID, VAL_TS FROM TEST").getAll();

        assertEquals(rows, res.size());

        for (List<?> r : res)
            assertEquals(r.get(0), r.get(1));
    }

    /**
     */
    @Test
    public void testDbg() throws Exception {
        sql("CREATE TABLE TEST (ID0 INT, ID1 INT, VAL0 INT, VAL1 INT, PRIMARY KEY(ID0, ID1))");

        sql("CREATE INDEX IDX_ID0 ON TEST(ID0)");
        sql("CREATE INDEX IDX_ID1 ON TEST(ID1)");

        for (int i = 0; i < 100000; ++i)
            sql ("INSERT INTO TEST VALUES (?, ?, ?, ?)", 0, i, i, i);

        U.sleep(1000);
        log.info("+++ SELECT");
        List<List<?>> res = sql("SELECT * FROM TEST WHERE VAL0 = 10").getAll();
        log.info("+++ " + sql("EXPLAIN SELECT * FROM TEST WHERE VAL0 = 10").getAll());

        assertEquals(1, res.size());

        sql("DROP INDEX IDX_ID0");
        res = sql("SELECT * FROM TEST WHERE VAL0 = 10").getAll();

        assertEquals(1, res.size());

        sql("DROP INDEX IDX_ID1");
        res = sql("SELECT * FROM TEST WHERE VAL0 = 10").getAll();

        assertEquals(1, res.size());

    }

    /**
     */
    @Test
    public void testSysdate() {
        sql("CREATE TABLE TEST (ID INT PRIMARY KEY, VAL_INT INT, VAL_TS TIMESTAMP)");

        int rows = 100;
        for (int i = 0; i < rows; ++i) {
            sql("INSERT INTO TEST VALUES " +
                            "(?, ?, ?)",
                    i, i, new Timestamp(ThreadLocalRandom.current().nextLong()));
        }

        List<List<?>> res0 = sql("SELECT ID, SYSDATE, SYSDATE() FROM TEST").getAll();

        assertEquals(rows, res0.size());

        List<List<?>> res1 = sql("SELECT VAL_TS - SYSDATE() FROM TEST").getAll();

        assertEquals(rows, res1.size());

        res1.forEach(r -> assertTrue("Invalid result type: " +
                r.get(0) + ",\n at results: " + res1, r.get(0) instanceof Long));

        List<List<?>> res2 = execute(new SqlFieldsQuery("SELECT VAL_TS - SYSDATE() FROM TEST").setLocal(true)).getAll();

        assertTrue(!res2.isEmpty());

        res2.forEach(r -> assertTrue("Invalid result type: " +
                r.get(0) + ",\n at results: " + res2, r.get(0) instanceof Long));
    }


    /**
     * @param sql SQL query.
     * @param args Query parameters.
     * @return Results cursor.
     */
    private FieldsQueryCursor<List<?>> sql(String sql, Object... args) {
        IgniteEx ign = client ? cli : grid(0);

        return ign.context().query().querySqlFields(new SqlFieldsQuery(sql)
            .setArgs(args), false);
    }

    /**
     * @param qry Query.
     * @return Results cursor.
     */
    private FieldsQueryCursor<List<?>> executeOnSrv(SqlFieldsQuery qry) {
        return grid(0).context().query().querySqlFields(qry, false);
    }
}
