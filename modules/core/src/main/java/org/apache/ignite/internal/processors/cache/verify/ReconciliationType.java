/*
 * Copyright 2021 GridGain Systems, Inc. and Contributors.
 *
 * Licensed under the GridGain Community Edition License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.gridgain.com/products/software/community-edition/gridgain-community-edition-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.processors.cache.verify;

import org.jetbrains.annotations.Nullable;

/**
 * Reconciliation types
 */
public enum ReconciliationType {
    /** Data consistency reconciliation. */
    DATA_CONSISTENCY,

    /** Cache size consistency reconciliation. */
    CACHE_SIZE_CONSISTENCY,

    /** Partition counter consistency reconciliation. */
    PARTITION_COUNTER_CONSISTENCY;

    /** Enumerated values. */
    private static final ReconciliationType[] VALS = values();

    /**
     * Efficiently gets enumerated value from its ordinal.
     *
     * @param ord Ordinal value.
     * @return Enumerated value or {@code null} if ordinal out of range.
     */
    @Nullable public static ReconciliationType fromOrdinal(int ord) {
        return ord >= 0 && ord < VALS.length ? VALS[ord] : null;
    }
}