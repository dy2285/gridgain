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

package org.apache.ignite.internal.processors.cache.datastructures;

import java.util.Collection;
import org.apache.ignite.IgniteSet;
import org.apache.ignite.cluster.ClusterState;
import org.apache.ignite.configuration.CollectionConfiguration;
import org.junit.Test;

/**
 * Tests specific methods of {@link IgniteSet} behaviour if cluster in a {@link ClusterState#ACTIVE_READ_ONLY} state.
 */
public class IgniteSetClusterReadOnlyTest extends IgniteCollectionsClusterReadOnlyAbstractTest {
    /** {@inheritDoc} */
    @Override String name(Collection col) {
        assertTrue(col + "", col instanceof IgniteSet);

        return ((IgniteSet)col).name();
    }

    /** {@inheritDoc} */
    @Override Collection createCollection(String name, CollectionConfiguration cfg) {
        return grid(0).set(name, cfg);
    }

    /** */
    @Test
    public void testCloseDenied() {
        performAction(c -> cast(c).close());
    }

    /** */
    @Test
    @Override public void testRemoveDenied() {
        super.testRemoveDenied();

        igniteCollections.forEach(c -> assertFalse(name(c), c.contains(UNKNOWN_ELEM)));

        performAction(c -> assertFalse(name(c), c.remove(UNKNOWN_ELEM)));
    }

    /** */
    private IgniteSet cast(Collection c) {
        return (IgniteSet)c;
    }
}
