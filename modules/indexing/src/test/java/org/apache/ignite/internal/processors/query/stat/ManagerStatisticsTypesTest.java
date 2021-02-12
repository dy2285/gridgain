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

import org.gridgain.internal.h2.value.ValueUuid;
import org.junit.Test;

import java.math.BigDecimal;

/**
 * Gather statistics on test table dtypes and check that statistics manager will return correct statistics for
 * different data types.
 */
public class ManagerStatisticsTypesTest extends StatisticsTypesAbstractTest {
    /**
     * Check collected statistics for each type.
     */
    @Test
    public void testCollectedStatistics() {
        ObjectStatisticsImpl dtypesStat = (ObjectStatisticsImpl) grid(0).context().query().getIndexing()
                .statsManager().getLocalStatistics("PUBLIC", "DTYPES");

        assertNotNull(dtypesStat);

        assertEquals(SMALL_SIZE * 1.5 - 1, dtypesStat.rowCount());

        assertEquals(TYPES.length + 3, dtypesStat.columnsStatistics().size());

        for (String type : TYPES) {
            String colName = COL_NAME_PREFIX + type;
            ColumnStatistics colStat = dtypesStat.columnStatistics(colName);

            assertNotNull(colStat);

            if (colName.equals("COL_GEOMETRY"))
                assertEquals("Unexpected nulls count for column " + colName, 100, colStat.nulls());
            else
                assertEquals("Unexpected nulls count for column " + colName, 33, colStat.nulls());

            assertEquals(dtypesStat.rowCount(), colStat.total());
            assertNotNull(colStat.raw());

            if (colName.equals("COL_GEOMETRY")) {
                assertNull(colStat.min());
                assertNull(colStat.max());
            }
            else {
                assertNotNull(colStat.min());
                assertNotNull(colStat.max());
            }
        }
    }

    /**
     * Check boolean type statistics collection.
     */
    @Test
    public void testBooleanStatistics() {
        String colName = COL_NAME_PREFIX + "BOOLEAN";
        ColumnStatistics booleanStats = getTypesStats().columnStatistics(colName);

        assertEquals(2, booleanStats.cardinality());
        assertFalse(booleanStats.min().getBoolean());
        assertTrue(booleanStats.max().getBoolean());
        assertEquals(1, booleanStats.size());
    }

    /**
     * Check boolean type statistics collection.
     */
    @Test
    public void testIntStatistics() {
        String colName = COL_NAME_PREFIX + "INT";
        ColumnStatistics intStats = getTypesStats().columnStatistics(colName);

        assertEquals(SMALL_SIZE, intStats.cardinality());
        assertEquals(1, intStats.min().getInt());
        assertEquals(SMALL_SIZE - 1, intStats.max().getInt());
        assertEquals(4, intStats.size());
    }

    /**
     * Check tinyint type statistics collection.
     */
    @Test
    public void testTinyintStatistics() {
        String colName = COL_NAME_PREFIX + "TINYINT";
        ColumnStatistics tinyintStats = getTypesStats().columnStatistics(colName);

        assertEquals(SMALL_SIZE, tinyintStats.cardinality());
        assertEquals(1, tinyintStats.min().getShort());
        assertEquals(SMALL_SIZE - 1, tinyintStats.max().getShort());
        assertEquals(1, tinyintStats.size());
    }

    /**
     * Check smallint type statistics collection.
     */
    @Test
    public void testSmallintStatistics() {
        String colName = COL_NAME_PREFIX + "SMALLINT";
        ColumnStatistics smallintStats = getTypesStats().columnStatistics(colName);

        assertEquals(SMALL_SIZE, smallintStats.cardinality());
        assertEquals(1, smallintStats.min().getShort());
        assertEquals(SMALL_SIZE - 1, smallintStats.max().getShort());
        assertEquals(2, smallintStats.size());
    }

    /**
     * Check bigint type statistics collection.
     */
    @Test
    public void testBigintStatistics() {
        String colName = COL_NAME_PREFIX + "BIGINT";
        ColumnStatistics bigintStats = getTypesStats().columnStatistics(colName);

        assertEquals(SMALL_SIZE, bigintStats.cardinality());
        assertEquals(1, bigintStats.min().getBigDecimal().intValue());
        assertEquals(SMALL_SIZE - 1, bigintStats.max().getBigDecimal().intValue());
        assertEquals(8, bigintStats.size());
    }

    /**
     * Check decimal type statistics collection.
     */
    @Test
    public void testDecimalStatistics() {
        String colName = COL_NAME_PREFIX + "DECIMAL";
        ColumnStatistics decimalStats = getTypesStats().columnStatistics(colName);

        assertEquals(SMALL_SIZE, decimalStats.cardinality());
        assertEquals(new BigDecimal("0.01"), decimalStats.min().getBigDecimal());
        assertEquals(new BigDecimal("" + ((double)SMALL_SIZE - 1) / 100), decimalStats.max().getBigDecimal());
        assertEquals(2, decimalStats.size());
    }

    /**
     * Check double type statistics collection.
     */
    @Test
    public void testDoubleStatistics() {
        String colName = COL_NAME_PREFIX + "DOUBLE";
        ColumnStatistics doubleStats = getTypesStats().columnStatistics(colName);

        assertEquals(SMALL_SIZE, doubleStats.cardinality());
        assertEquals(0.01, doubleStats.min().getDouble());
        assertEquals(((double)SMALL_SIZE - 1) / 100, doubleStats.max().getDouble());
        assertEquals(2, doubleStats.size());
    }

    /**
     * Check real type statistics collection.
     */
    @Test
    public void testRealStatistics() {
        String colName = COL_NAME_PREFIX + "REAL";
        ColumnStatistics realStats = getTypesStats().columnStatistics(colName);

        assertEquals(SMALL_SIZE, realStats.cardinality());
        assertEquals(new BigDecimal("0.01"), realStats.min().getBigDecimal());
        assertEquals(new BigDecimal("" + ((double)SMALL_SIZE - 1) / 100), realStats.max().getBigDecimal());
        assertEquals(2, realStats.size());
    }

    /**
     * Check time type statistics collection.
     */
    @Test
    public void testTimeStatistics() {
        String colName = COL_NAME_PREFIX + "TIME";
        ColumnStatistics timeStats = getTypesStats().columnStatistics(colName);

        assertEquals(SMALL_SIZE, timeStats.cardinality());
        assertEquals("12:00:01", timeStats.min().getTime().toString());
        assertEquals("12:01:39", timeStats.max().getTime().toString());
        assertEquals(4, timeStats.size());
    }

    /**
     * Check date type statistics collection.
     */
    @Test
    public void testDateStatistics() {
        String colName = COL_NAME_PREFIX + "DATE";
        ColumnStatistics dateStats = getTypesStats().columnStatistics(colName);

        assertEquals(SMALL_SIZE, dateStats.cardinality());
        assertEquals("1970-01-02", dateStats.min().getDate().toString());
        assertEquals("1970-04-10", dateStats.max().getDate().toString());
        assertEquals(4, dateStats.size());
    }

    /**
     * Check timestamp type statistics collection.
     */
    @Test
    public void testTimestampStatistics() {
        String colName = COL_NAME_PREFIX + "TIMESTAMP";
        ColumnStatistics timestampStats = getTypesStats().columnStatistics(colName);

        assertEquals(SMALL_SIZE, timestampStats.cardinality());
        assertEquals("1970-01-01 12:00:01.0", timestampStats.min().getTimestamp().toString());
        assertEquals("1970-01-01 12:01:39.0", timestampStats.max().getTimestamp().toString());
        assertEquals(4, timestampStats.size());
    }

    /**
     * Check varchar type statistics collection.
     */
    @Test
    public void testVarcharStatistics() {
        String colName = COL_NAME_PREFIX + "VARCHAR";
        ColumnStatistics varcharStats = getTypesStats().columnStatistics(colName);

        assertEquals(SMALL_SIZE, varcharStats.cardinality());
        assertEquals("varchar" + 1, varcharStats.min().getString());
        assertEquals("varchar" + (SMALL_SIZE - 1), varcharStats.max().getString());
        assertEquals(8, varcharStats.size());
    }

    /**
     * Check char type statistics collection.
     */
    @Test
    public void testCharStatistics() {
        String colName = COL_NAME_PREFIX + "CHAR";
        ColumnStatistics charStats = getTypesStats().columnStatistics(colName);

        assertEquals(26, charStats.cardinality());
        assertEquals('A', charStats.min().getString().charAt(0));
        assertEquals('Z', charStats.max().getString().charAt(0));
        assertEquals(1, charStats.size());
    }

    /**
     * Check UUID type statistics collection.
     */
    @Test
    public void testUUIDStatistics() {
        String colName = COL_NAME_PREFIX + "UUID";
        ColumnStatistics decimalStats = getTypesStats().columnStatistics(colName);

        assertEquals(SMALL_SIZE, decimalStats.cardinality());
        assertEquals(1L, ((ValueUuid)decimalStats.min()).getLow());
        assertEquals(SMALL_SIZE - 1L, ((ValueUuid)decimalStats.max()).getLow());
        assertEquals(16, decimalStats.size());
    }

    /**
     * Check binary type statistics collection.
     */
    @Test
    public void testBinaryStatistics() {
        String colName = COL_NAME_PREFIX + "BINARY";
        ColumnStatistics binaryStats = getTypesStats().columnStatistics(colName);

        assertEquals(SMALL_SIZE, binaryStats.cardinality());
        assertEquals((byte)1, binaryStats.min().getBytes()[3]);
        assertEquals((byte)99, binaryStats.max().getBytes()[3]);
        assertEquals(4, binaryStats.size());
    }

    /**
     * Get local statistics for dtypes table.
     *
     * @return Local object statistics for dtypes table.
     */
    private ObjectStatisticsImpl getTypesStats() {
        return (ObjectStatisticsImpl) grid(0).context().query().getIndexing().statsManager()
                .getLocalStatistics(SCHEMA, "DTYPES");
    }
}
