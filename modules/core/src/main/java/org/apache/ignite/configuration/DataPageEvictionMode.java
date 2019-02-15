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

package org.apache.ignite.configuration;

import org.jetbrains.annotations.Nullable;

/**
 * Defines memory page eviction algorithm. A mode is set for a specific
 * {@link DataRegionConfiguration}. Only data pages, that store key-value entries, are eligible for eviction. The
 * other types of pages, like index or meta pages, are not evictable.
 */
public enum DataPageEvictionMode {
    /** Eviction is disabled. */
    DISABLED,

    /**
     * Random-LRU algorithm.
     * <ul>
     * <li>Once a memory region defined by a data region is configured, an off-heap array is allocated to track
     * last usage timestamp for every individual data page. The size of the array is calculated this way - size =
     * ({@link DataRegionConfiguration#getMaxSize()} / {@link DataStorageConfiguration#pageSize})</li>
     * <li>When a data page is accessed, its timestamp gets updated in the tracking array. The page index in the
     * tracking array is calculated this way - index = (pageAddress / {@link DataRegionConfiguration#getMaxSize()}</li>
     * <li>When it's required to evict some pages, the algorithm randomly chooses 5 indexes from the tracking array and
     * evicts a page with the latest timestamp. If some of the indexes point to non-data pages (index or system pages)
     * then the algorithm picks other pages.</li>
     * </ul>
     */
    RANDOM_LRU,

    /**
     * Random-2-LRU algorithm: scan-resistant version of Random-LRU.
     * <p>
     * This algorithm differs from Random-LRU only in a way that two latest access timestamps are stored for every
     * data page. At the eviction time, a minimum between two latest timestamps is taken for further comparison with
     * minimums of other pages that might be evicted. LRU-2 outperforms LRU by resolving "one-hit wonder" problem -
     * if a data page is accessed rarely, but accidentally accessed once, it's protected from eviction for a long time.
     */
    RANDOM_2_LRU;

    /** Enumerated values. */
    private static final DataPageEvictionMode[] VALS = values();

    /**
     * Efficiently gets enumerated value from its ordinal.
     *
     * @param ord Ordinal value.
     * @return Enumerated value or {@code null} if ordinal out of range.
     */
    @Nullable public static DataPageEvictionMode fromOrdinal(int ord) {
        return ord >= 0 && ord < VALS.length ? VALS[ord] : null;
    }
}
