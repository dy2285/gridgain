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

package org.apache.ignite.internal.processors.query.schema;

import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.internal.IgniteInterruptedCheckedException;
import org.apache.ignite.internal.processors.cache.GridCacheContext;
import org.apache.ignite.internal.processors.cache.GridCacheEntryEx;
import org.apache.ignite.internal.processors.cache.GridCacheEntryRemovedException;
import org.apache.ignite.internal.processors.cache.KeyCacheObject;
import org.apache.ignite.internal.processors.cache.distributed.dht.topology.GridDhtInvalidPartitionException;
import org.apache.ignite.internal.processors.cache.distributed.dht.topology.GridDhtLocalPartition;
import org.apache.ignite.internal.processors.cache.distributed.dht.topology.GridDhtPartitionState;
import org.apache.ignite.internal.processors.cache.persistence.CacheDataRow;
import org.apache.ignite.internal.processors.query.QueryTypeDescriptorImpl;
import org.apache.ignite.internal.util.future.GridFutureAdapter;
import org.apache.ignite.internal.util.lang.GridCursor;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.internal.util.worker.GridWorker;
import org.jetbrains.annotations.Nullable;

import static java.util.Objects.nonNull;
import static org.apache.ignite.IgniteSystemProperties.IGNITE_ENABLE_EXTRA_INDEX_REBUILD_LOGGING;
import static org.apache.ignite.IgniteSystemProperties.getBoolean;
import static org.apache.ignite.internal.processors.cache.distributed.dht.topology.GridDhtPartitionState.EVICTED;
import static org.apache.ignite.internal.processors.cache.distributed.dht.topology.GridDhtPartitionState.LOST;
import static org.apache.ignite.internal.processors.cache.distributed.dht.topology.GridDhtPartitionState.MOVING;
import static org.apache.ignite.internal.processors.cache.distributed.dht.topology.GridDhtPartitionState.OWNING;
import static org.apache.ignite.internal.processors.cache.distributed.dht.topology.GridDhtPartitionState.RENTING;
import static org.apache.ignite.internal.processors.cache.persistence.CacheDataRowAdapter.RowData.KEY_ONLY;

/**
 * Worker for creating/rebuilding indexes for cache per partition.
 */
public class SchemaIndexCachePartitionWorker extends GridWorker {
    /** Count of rows, being processed within a single checkpoint lock. */
    private static final int BATCH_SIZE = 1000;

    /** Cache context. */
    private final GridCacheContext cctx;

    /** Stop flag between all workers for one cache. */
    private static volatile boolean stop;

    /** Index closure. */
    private final SchemaIndexCacheVisitorClosureWrapper wrappedClo;

    /** Partition. */
    private final GridDhtLocalPartition locPart;

    /** Worker future. */
    private final GridFutureAdapter<SchemaIndexCacheStat> fut;

    /**
     * Constructor.
     *
     * @param cctx Cache context.
     * @param locPart Partition.
     * @param clo Index closure.
     * @param fut Worker future.
     */
    public SchemaIndexCachePartitionWorker(
        GridCacheContext cctx,
        GridDhtLocalPartition locPart,
        SchemaIndexCacheVisitorClosure clo,
        GridFutureAdapter<SchemaIndexCacheStat> fut
    ) {
        super(
            cctx.igniteInstanceName(),
            "parallel-idx-worker-" + cctx.cache().name() + "-part-" + locPart.id(),
            cctx.logger(SchemaIndexCachePartitionWorker.class)
        );

        this.cctx = cctx;
        this.locPart = locPart;

        assert nonNull(clo);
        assert nonNull(fut);

        wrappedClo = new SchemaIndexCacheVisitorClosureWrapper(clo);
        this.fut = fut;
    }

    /** {@inheritDoc} */
    @Override protected void body() throws InterruptedException, IgniteInterruptedCheckedException {
        Throwable err = null;

        try {
            processPartition();
        }
        catch (Throwable e) {
            err = e;

            U.error(log, "Error during create/rebuild index for partition: " + locPart.id(), e);

            stop = true;

            cctx.group().metrics().setIndexBuildCountPartitionsLeft(0);
        }
        finally {
            fut.onDone(wrappedClo.indexCacheStat, err);
        }
    }

    /**
     * Process partition.
     *
     * @throws IgniteCheckedException If failed.
     */
    private void processPartition() throws IgniteCheckedException {
        if (stop || stopNode() || Thread.interrupted())
            return;

        boolean reserved = false;

        GridDhtPartitionState partState = locPart.state();
        if (partState != EVICTED)
            reserved = (partState == OWNING || partState == MOVING || partState == LOST) && locPart.reserve();

        if (!reserved)
            return;

        GridCursor<? extends CacheDataRow> cursor = null;

        try {
            cursor = locPart.dataStore().cursor(cctx.cacheId(), null, null, KEY_ONLY);

            boolean locked = false;

            try {
                int cntr = 0;

                while (cursor.next() && !stop && !stopNode()) {
                    KeyCacheObject key = cursor.get().key();

                    if (!locked) {
                        cctx.shared().database().checkpointReadLock();

                        locked = true;
                    }

                    if (!processKey(key))
                        return;

                    if (++cntr % BATCH_SIZE == 0) {
                        cctx.shared().database().checkpointReadUnlock();

                        locked = false;
                    }

                    if (locPart.state() == RENTING)
                        break;
                }

                wrappedClo.addNumberProcessedKeys(cntr);
            }
            finally {
                if (locked)
                    cctx.shared().database().checkpointReadUnlock();
            }
        }
        finally {
            if (cursor != null) {
                try {
                    cursor.close();
                }
                catch (Exception e) {
                    throw new IgniteCheckedException(e);
                }
            }

            locPart.release();

            cctx.group().metrics().decrementIndexBuildCountPartitionsLeft();
        }
    }

    /**
     * Process single key.
     *
     * @param key Key.
     * @return {@code True} if no {@link Thread#interrupted()} was checked.
     * @throws IgniteCheckedException If failed.
     */
    private boolean processKey(KeyCacheObject key) throws IgniteCheckedException {
        assert nonNull(key);

        while (true) {
            try {
                if (Thread.interrupted())
                    return false;

                GridCacheEntryEx entry = cctx.cache().entryEx(key);

                try {
                    entry.updateIndex(wrappedClo);
                }
                finally {
                    entry.touch();
                }

                break;
            }
            catch (GridDhtInvalidPartitionException ignore) {
                break;
            }
            catch (GridCacheEntryRemovedException ignored) {
                // No-op.
            }
        }

        return true;
    }


    /**
     * Returns node in the process of stopping or not.
     *
     * @return {@code True} if node is in the process of stopping.
     */
    private boolean stopNode() {
        return cctx.kernalContext().isStopping();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(SchemaIndexCachePartitionWorker.class, this);
    }

    /**
     * Wrapper class for given closure.
     */
    private class SchemaIndexCacheVisitorClosureWrapper implements SchemaIndexCacheVisitorClosure {
        /** Closure. */
        private final SchemaIndexCacheVisitorClosure clo;

        /** Object for collecting statistics about index update. */
        @Nullable private final SchemaIndexCacheStat indexCacheStat;

        /** */
        private SchemaIndexCacheVisitorClosureWrapper(SchemaIndexCacheVisitorClosure clo) {
            this.clo = clo;
            indexCacheStat = getBoolean(IGNITE_ENABLE_EXTRA_INDEX_REBUILD_LOGGING, false) ? new SchemaIndexCacheStat() : null;
        }

        /** {@inheritDoc} */
        @Override public void apply(CacheDataRow row) throws IgniteCheckedException {
            if (row != null) {
                clo.apply(row);

                if (indexCacheStat != null) {
                    QueryTypeDescriptorImpl type = cctx.kernalContext().query().typeByValue(
                        cctx.cache().name(),
                        cctx.cacheObjectContext(),
                        row.key(),
                        row.value(),
                        true
                    );

                    if (type != null)
                        indexCacheStat.addType(type);
                }
            }
        }

        /** */
        private void addNumberProcessedKeys(int cnt) {
            if (nonNull(indexCacheStat))
                indexCacheStat.add(cnt);
        }
    }
}
