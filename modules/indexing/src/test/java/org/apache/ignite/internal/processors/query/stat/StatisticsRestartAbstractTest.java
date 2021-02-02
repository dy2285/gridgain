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

import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;

/**
 * Test about statistics reload after restart.
 */
public class StatisticsRestartAbstractTest extends StatisticsAbstractTest {
    /** Target for test table SMALL. */
    protected StatisticsTarget SMALL_TARGET = new StatisticsTarget(SCHEMA, "SMALL");

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(igniteInstanceName);

        cfg.setConsistentId(igniteInstanceName);

        DataStorageConfiguration memCfg = new DataStorageConfiguration()
            .setDefaultDataRegionConfiguration(new DataRegionConfiguration().setPersistenceEnabled(true));

        cfg.setDataStorageConfiguration(memCfg);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        stopAllGrids();

        cleanPersistenceDir();

        startGridsMultiThreaded(nodes());

        grid(0).getOrCreateCache(DEFAULT_CACHE_NAME);

        createStatisticTarget(null);
    }

    /**
     * Create SQL table with the given index.
     *
     * @param idx Table idx, if {@code null} - name "SMALL" without index will be used.
     * @return Target to created table.
     */
    protected StatisticsTarget createStatisticTarget(Integer idx) {
        String strIdx = (idx == null) ? "" : String.valueOf(idx);

        createSmallTable(strIdx);

        return new StatisticsTarget(SCHEMA, "SMALL" + strIdx);
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws IgniteCheckedException {
        grid(0).context().query().getIndexing().statsManager().updateStatistics(SMALL_TARGET);
    }

    /**
     * @return Number of nodes to start.
     */
    public int nodes() {
        return 1;
    }
}
