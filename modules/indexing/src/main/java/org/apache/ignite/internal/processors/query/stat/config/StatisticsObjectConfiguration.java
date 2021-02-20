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
package org.apache.ignite.internal.processors.query.stat.config;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.ignite.internal.processors.query.stat.StatisticsKey;
import org.apache.ignite.internal.util.tostring.GridToStringInclude;
import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.internal.util.typedef.internal.S;

/**
 * Describe configuration of the statistic for a database object (e.g. TABLE).
 */
public class StatisticsObjectConfiguration implements Serializable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Database object key (schema and name). */
    @GridToStringInclude
    private final StatisticsKey key;

    /** Map of the statistic configurations for columns (column_name -> configuration). */
    @GridToStringInclude
    private final Map<String, StatisticsColumnConfiguration> cols;

    /** */
    public StatisticsObjectConfiguration(
        StatisticsKey key,
        Collection<StatisticsColumnConfiguration> cols
    ) {
        this.key = key;
        this.cols = cols.stream()
            .collect(
                Collectors.toMap(StatisticsColumnConfiguration::name, Function.identity())
            );
    }

    /**
     * Merge configuration changes with existing configuration.
     *
     * @param oldCfg Previous configuration. May be {@code null} when new configuration is created.
     * @param newCfg Contains target configuration changes.
     * @return merged configuration.
     */
    public static StatisticsObjectConfiguration merge(
        StatisticsObjectConfiguration oldCfg,
        StatisticsObjectConfiguration newCfg
    ) {
        assert oldCfg.key.equals(newCfg.key) : "Invalid stat config to merge: [oldKey=" + oldCfg.key
            + ", newKey=" + newCfg.key + ']';

        Map<String, StatisticsColumnConfiguration> cols = new HashMap<>(oldCfg.cols);

        for (StatisticsColumnConfiguration c : newCfg.cols.values())
            cols.put(c.name(), StatisticsColumnConfiguration.merge(cols.get(c.name()), c));

        return new StatisticsObjectConfiguration(newCfg.key, cols.values());
    }

    /**
     * Creates new configuration object for drop specified columns from current configuration.
     * Marks dropped columns as tombstone.
     *
     * @param dropColNames Set of dropped columns.
     * @return Result configuration object.
     */
    public StatisticsObjectConfiguration dropColumns(Set<String> dropColNames) {
        if (F.isEmpty(dropColNames))
            dropColNames = cols.keySet();

        Map<String, StatisticsColumnConfiguration> newCols = new HashMap<>(cols);

        for (String dropCol : dropColNames) {
            StatisticsColumnConfiguration c = cols.get(dropCol);

            if (c != null)
                newCols.put(c.name(), c.createTombstone());
        }

        return new StatisticsObjectConfiguration(key, newCols.values());
    }

    /**
     * Calculate diff between two configuration.
     *
     * @param oldCfg Current configuration.
     * @param newCfg Target configuration.
     * @return Configurations differences.
     */
    public static Diff diff(
        StatisticsObjectConfiguration oldCfg,
        StatisticsObjectConfiguration newCfg
    ) {
        if (oldCfg == null)
            return new Diff(Collections.emptySet(), newCfg.cols);

        Set<String> dropCols = new HashSet<>();
        Map<String, StatisticsColumnConfiguration> updateCols = new HashMap<>();

        for (StatisticsColumnConfiguration colNew : newCfg.cols.values()) {
            StatisticsColumnConfiguration colOld = oldCfg.cols.get(colNew.name());

            if (colOld == null || (colNew.version() > colOld.version() && !colNew.tombstone()))
                updateCols.put(colNew.name(), colNew);
            else if (colNew.tombstone() && !colOld.tombstone())
                dropCols.add(colNew.name());
        }

        return new Diff(dropCols, updateCols);
    }

    /**
     * Get database object key (schema and name).
     * @return statistic key.
     */
    public StatisticsKey key() {
        return key;
    }

    /**
     * Get configurations of all statistic columns includes tombstone configuration objects (dropped columns).
     *
     * @return statistic key.
     */
    public Map<String, StatisticsColumnConfiguration> columnsAll() {
        return cols;
    }

    /**
     * Get configurations of statistic columns.
     *
     * @return statistic key.
     */
    public Map<String, StatisticsColumnConfiguration> columns() {
        return cols.entrySet().stream()
            .filter(e -> !e.getValue().tombstone())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        StatisticsObjectConfiguration that = (StatisticsObjectConfiguration)o;

        return Objects.equals(key, that.key)
            && Objects.equals(cols, that.cols);
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        return Objects.hash(key, cols);
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(StatisticsObjectConfiguration.class, this);
    }

    /**
     * Difference between current and target configuration.
     */
    public static class Diff {
        /**
         * Statistic columns to drop.
         */
        private final Set<String> dropCols;

        /**
         * Statistic columns to update.
         */
        private final Map<String, StatisticsColumnConfiguration> updateCols;

        /** */
        public Diff(
            Set<String> dropCols,
            Map<String, StatisticsColumnConfiguration> updateCols
        ) {
            this.dropCols = dropCols;
            this.updateCols = updateCols;
        }

        /**
         * Statistics columns to drop.
         *
         * @return Set of the columns' names.
         */
        public Set<String> dropCols() {
            return dropCols;
        }

        /**
         * Statistics columns to update.
         *
         * @return Map of the statistic configuration for columns (column_name -> column_configuration).
         */
        public Map<String, StatisticsColumnConfiguration> updateCols() {
            return updateCols;
        }
    }
}
