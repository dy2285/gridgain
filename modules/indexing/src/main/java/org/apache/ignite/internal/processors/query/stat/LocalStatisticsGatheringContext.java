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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.ignite.internal.processors.query.h2.opt.GridH2Table;
import org.apache.ignite.internal.processors.query.stat.config.StatisticsObjectConfiguration;
import org.apache.ignite.internal.util.future.GridFutureAdapter;
import org.apache.ignite.internal.util.typedef.internal.S;

/**
 * Statistics gathering context.
 */
public class LocalStatisticsGatheringContext {
    /** Amount of remaining partitions */
    private int remainingParts;

    /** Done future adapter. */
    private final GridFutureAdapter<Void> fut;

    /** */
    public LocalStatisticsGatheringContext(int remainingParts) {
        this.remainingParts = remainingParts;
        this.fut = new GridFutureAdapter<>();
    }

    /**
     * Decrement remaining.
     */
    public synchronized void decrement() {
        remainingParts--;
    }

    /**
     * @return Collection control future.
     */
    public GridFutureAdapter<Void> future() {
        return fut;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(LocalStatisticsGatheringContext.class, this);
    }
}
