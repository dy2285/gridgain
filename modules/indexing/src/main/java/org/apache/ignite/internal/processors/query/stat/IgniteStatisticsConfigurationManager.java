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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteLogger;
import org.apache.ignite.events.DiscoveryEvent;
import org.apache.ignite.internal.NodeStoppingException;
import org.apache.ignite.internal.events.DiscoveryCustomEvent;
import org.apache.ignite.internal.managers.discovery.DiscoveryCustomMessage;
import org.apache.ignite.internal.processors.affinity.AffinityTopologyVersion;
import org.apache.ignite.internal.processors.cache.DynamicCacheChangeBatch;
import org.apache.ignite.internal.processors.cache.GridCacheContext;
import org.apache.ignite.internal.processors.cache.GridCachePartitionExchangeManager;
import org.apache.ignite.internal.processors.cache.distributed.dht.preloader.ExchangeType;
import org.apache.ignite.internal.processors.cache.distributed.dht.preloader.GridDhtPartitionsExchangeFuture;
import org.apache.ignite.internal.processors.cache.distributed.dht.preloader.PartitionsExchangeAware;
import org.apache.ignite.internal.processors.cache.query.IgniteQueryErrorCode;
import org.apache.ignite.internal.processors.metastorage.DistributedMetaStorage;
import org.apache.ignite.internal.processors.metastorage.DistributedMetastorageLifecycleListener;
import org.apache.ignite.internal.processors.metastorage.ReadableDistributedMetaStorage;
import org.apache.ignite.internal.processors.query.IgniteSQLException;
import org.apache.ignite.internal.processors.query.h2.SchemaManager;
import org.apache.ignite.internal.processors.query.h2.opt.GridH2Table;
import org.apache.ignite.internal.processors.query.stat.config.StatisticsColumnConfiguration;
import org.apache.ignite.internal.processors.query.stat.config.StatisticsObjectConfiguration;
import org.apache.ignite.internal.processors.subscription.GridInternalSubscriptionProcessor;
import org.apache.ignite.internal.util.IgniteUtils;
import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.internal.util.typedef.X;
import org.apache.ignite.thread.IgniteThreadPoolExecutor;
import org.gridgain.internal.h2.table.Column;

/**
 * Holds statistic configuration objects at the distributed metastore
 * and match local statistics with target statistic configuration.
 */
public class IgniteStatisticsConfigurationManager {
    /** */
    private static final String STAT_OBJ_PREFIX = "sql.statobj.";

    /** Schema manager. */
    private final SchemaManager schemaMgr;

    /** Distributed metastore. */
    private volatile DistributedMetaStorage distrMetaStorage;

    /** Statistics repository.*/
    private final IgniteStatisticsRepository repo;

    /** Statistic gatherer. */
    private final StatisticsGatherer gatherer;

    /** */
    private final IgniteThreadPoolExecutor mgmtPool;

    /** Logger. */
    private final IgniteLogger log;

    /** Started flag (used to skip updates of the distributed metastorage on start). */
    private volatile boolean started;

    /** Monitor to synchronize changes repository: aggregate after collects and drop statistics. */
    private final Object mux = new Object();

    /** */
    public IgniteStatisticsConfigurationManager(
        SchemaManager schemaMgr,
        GridInternalSubscriptionProcessor subscriptionProcessor,
        GridCachePartitionExchangeManager exchange,
        IgniteStatisticsRepository repo,
        StatisticsGatherer gatherer,
        IgniteThreadPoolExecutor mgmtPool,
        Function<Class<?>, IgniteLogger> logSupplier
    ) {
        this.schemaMgr = schemaMgr;
        log = logSupplier.apply(IgniteStatisticsConfigurationManager.class);
        this.repo = repo;
        this.mgmtPool = mgmtPool;
        this.gatherer = gatherer;

        subscriptionProcessor.registerDistributedMetastorageListener(new DistributedMetastorageLifecycleListener() {
            @Override public void onReadyForRead(ReadableDistributedMetaStorage metastorage) {
                distrMetaStorage = (DistributedMetaStorage)metastorage;

                distrMetaStorage.listen(
                    (metaKey) -> metaKey.startsWith(STAT_OBJ_PREFIX),
                    (k, oldV, newV) -> {
                        // Skip invoke on start node (see 'ReadableDistributedMetaStorage#listen' the second case)
                        // The update statistics on start node is handled by 'scanAndCheckLocalStatistic' method
                        // called on exchange done.
                        if (!started)
                            return;

                        mgmtPool.submit(() -> {
                            try {
                                onChangeStatisticConfiguration(
                                    (StatisticsObjectConfiguration)oldV,
                                    (StatisticsObjectConfiguration)newV
                                );
                            }
                            catch (Throwable e) {
                                log.warning("Unexpected exception on change statistic configuration [old="
                                    + oldV + ", new=" + newV + ']', e);
                            }
                        });
                    }
                );
            }
        });

        exchange.registerExchangeAwareComponent(
            new PartitionsExchangeAware() {
                @Override public void onDoneAfterTopologyUnlock(GridDhtPartitionsExchangeFuture fut) {
                    started = true;

                    // Skip join/left client nodes.
                    if (fut.exchangeType() != ExchangeType.ALL)
                        return;

                    DiscoveryEvent evt = fut.firstEvent();

                    // Skip create/destroy caches.
                    if (evt.type() == DiscoveryCustomEvent.EVT_DISCOVERY_CUSTOM_EVT) {
                        DiscoveryCustomMessage msg = ((DiscoveryCustomEvent)evt).customMessage();

                        if (msg instanceof DynamicCacheChangeBatch)
                            return;
                    }

                    scanAndCheckLocalStatistics(fut.topologyVersion());
                }
            }
        );

        schemaMgr.registerDropColumnsListener(this::onDropColumns);
        schemaMgr.registerDropTableListener(this::onDropTable);
    }

    /** */
    private void scanAndCheckLocalStatistics(AffinityTopologyVersion topVer) {
        mgmtPool.submit(() -> {
            try {
                distrMetaStorage.iterate(STAT_OBJ_PREFIX, (k, v) ->
                    checkLocalStatistics((StatisticsObjectConfiguration)v, topVer));
            }
            catch (IgniteCheckedException e) {
                log.warning("Unexpected exception on check local statistic on start", e);
            }
        });
    }

    /**
     * Update local statistic for specified database objects on the cluster.
     * Each node will scan local primary partitions to collect and update local statistic.
     *
     * @param targets DB objects to statistics update.
     */
    public void updateStatistics(List<StatisticsTarget> targets) {
        if (log.isDebugEnabled())
            log.debug("Update statistics [targets=" + targets + ']');

        for (StatisticsTarget target : targets) {
            GridH2Table tbl = schemaMgr.dataTable(target.schema(), target.obj());

            validate(target, tbl);

            Column[] cols = IgniteStatisticsHelper.filterColumns(
                tbl.getColumns(),
                target.columns() != null ? Arrays.asList(target.columns()) : Collections.emptyList());

            List<StatisticsColumnConfiguration> colCfgs = Arrays.stream(cols)
                .map(c -> new StatisticsColumnConfiguration(c.getName()))
                .collect(Collectors.toList());

            StatisticsObjectConfiguration newCfg = new StatisticsObjectConfiguration(target.key(), colCfgs);

            try {
                while (true) {
                    String key = key2String(target.key());

                    StatisticsObjectConfiguration oldCfg = distrMetaStorage.read(key);

                    if (oldCfg != null)
                        newCfg = StatisticsObjectConfiguration.merge(oldCfg, newCfg);

                    if (distrMetaStorage.compareAndSet(key, oldCfg, newCfg))
                        break;
                }
            }
            catch (IgniteCheckedException ex) {
                throw new IgniteSQLException("Error on get or update statistic schema", IgniteQueryErrorCode.UNKNOWN, ex);
            }
        }
    }

    /**
     * Drop local statistic for specified database objects on the cluster.
     * Remove local aggregated and partitioned statistics that are stored at the local metastorage.
     *
     * @param targets DB objects to statistics update.
     */
    public void dropStatistics(List<StatisticsTarget> targets, boolean validate) {
        if (log.isDebugEnabled())
            log.debug("Drop statistics [targets=" + targets + ']');

        for (StatisticsTarget target : targets) {
            String key = key2String(target.key());

            try {
                while (true) {
                    StatisticsObjectConfiguration oldCfg = distrMetaStorage.read(key);

                    if (validate)
                        validateDropRefresh(target, oldCfg);

                    StatisticsObjectConfiguration newCfg = oldCfg.dropColumns(
                        target.columns() != null ?
                            Arrays.stream(target.columns()).collect(Collectors.toSet()) :
                            Collections.emptySet()
                    );

                    if (distrMetaStorage.compareAndSet(key, oldCfg, newCfg))
                        break;
                }
            }
            catch (IgniteCheckedException ex) {
                throw new IgniteSQLException(
                    "Error on get or update statistic schema", IgniteQueryErrorCode.UNKNOWN, ex);
            }
        }
    }

    /**
     * Drop all local statistics on the cluster.
     */
    public void dropAll() {
        try {
            final List<StatisticsTarget> targetsToRemove = new ArrayList<>();

            distrMetaStorage.iterate(STAT_OBJ_PREFIX, (k, v) -> {
                    StatisticsKey statKey = ((StatisticsObjectConfiguration)v).key();

                    StatisticsObjectConfiguration cfg = (StatisticsObjectConfiguration)v;

                    if (!F.isEmpty(cfg.columns()))
                        targetsToRemove.add(new StatisticsTarget(statKey, null));
                }
            );

            dropStatistics(targetsToRemove, false);
        }
        catch (IgniteCheckedException e) {
            throw new IgniteSQLException(
                "Unexpected exception drop all statistics", IgniteQueryErrorCode.UNKNOWN, e);
        }
    }

    /**
     * Refresh local statistic for specified database objects on the cluster.
     *
     * @param targets DB objects to statistics update.
     */
    public void refreshStatistics(List<StatisticsTarget> targets) {
        if (log.isDebugEnabled())
            log.debug("Drop statistics [targets=" + targets + ']');

        for (StatisticsTarget target : targets) {
            String key = key2String(target.key());

            try {
                while (true) {
                    StatisticsObjectConfiguration oldCfg = distrMetaStorage.read(key);

                    validateDropRefresh(target, oldCfg);

                    StatisticsObjectConfiguration newCfg = oldCfg.refresh(
                        target.columns() != null ?
                            Arrays.stream(target.columns()).collect(Collectors.toSet()) :
                            Collections.emptySet());

                    if (distrMetaStorage.compareAndSet(key, oldCfg, newCfg))
                        break;
                }
            }
            catch (IgniteCheckedException ex) {
                throw new IgniteSQLException(
                    "Error on get or update statistic schema", IgniteQueryErrorCode.UNKNOWN, ex);
            }
        }
    }

    /** */
    private void validate(StatisticsTarget target, GridH2Table tbl) {
        if (tbl == null) {
            throw new IgniteSQLException(
                "Table doesn't exist [schema=" + target.schema() + ", table=" + target.obj() + ']',
                IgniteQueryErrorCode.TABLE_NOT_FOUND);
        }

        if (!F.isEmpty(target.columns())) {
            for (String col : target.columns()) {
                if (!tbl.doesColumnExist(col)) {
                    throw new IgniteSQLException(
                        "Column doesn't exist [schema=" + target.schema() +
                            ", table=" + target.obj() +
                            ", column=" + col + ']',
                        IgniteQueryErrorCode.COLUMN_NOT_FOUND);
                }
            }
        }
    }

    /** */
    private void validateDropRefresh(StatisticsTarget target, StatisticsObjectConfiguration cfg) {
        if (cfg == null || F.isEmpty(cfg.columns())) {
            throw new IgniteSQLException(
                "Statistic doesn't exist for [schema=" + target.schema() + ", obj=" + target.obj() + ']',
                IgniteQueryErrorCode.TABLE_NOT_FOUND
            );
        }

        if (!F.isEmpty(target.columns())) {
            for (String col : target.columns()) {
                if (!cfg.columns().containsKey(col)) {
                    throw new IgniteSQLException(
                        "Statistic doesn't exist for [" +
                            "schema=" + cfg.key().schema() +
                            ", obj=" + cfg.key().obj() +
                            ", col=" + col + ']',
                        IgniteQueryErrorCode.COLUMN_NOT_FOUND
                    );
                }
            }
        }
   }

    /**
     * Scan local statistic saved at the local metastorage, compare ones to statistic configuration.
     * The local statistics must be matched with configuration:
     * - re-collect old statistics;
     * - drop local statistics if ones dropped on cluster;
     * - collect new statistics if it possible.
     *
     * The method is called on change affinity assignment (end of PME).
     * @param cfg expected statistic configuration.
     * @param topVer topology version.
     */
    private void checkLocalStatistics(StatisticsObjectConfiguration cfg, final AffinityTopologyVersion topVer) {
        try {
            GridH2Table tbl = schemaMgr.dataTable(cfg.key().schema(), cfg.key().obj());

            if (tbl == null) {
                // Drop tables handle by onDropTable
                return;
            }

            GridCacheContext cctx = tbl.cacheContext();

            AffinityTopologyVersion topVer0 = cctx.affinity().affinityReadyFuture(topVer).get();

            final Set<Integer> parts = cctx.affinity().primaryPartitions(cctx.localNodeId(), topVer0);

            if (F.isEmpty(parts)) {
                // There is no data on the node for specified cache.
                // Remove oll data
                dropColumnsOnLocalStatistics(cfg, cfg.columns().keySet());

                return;
            }

            final Set<Integer> partsOwn = new HashSet<>(
                cctx.affinity().backupPartitions(cctx.localNodeId(), topVer0)
            );

            partsOwn.addAll(parts);

            if (log.isDebugEnabled())
                log.debug("Check local statistics [key=" + cfg + ", parts=" + parts + ']');

            Collection<ObjectPartitionStatisticsImpl> partStats = repo.getLocalPartitionsStatistics(cfg.key());

            Set<Integer> partsToRmv = new HashSet<>();
            Set<Integer> partsToCollect = new HashSet<>(parts);
            Map<String, StatisticsColumnConfiguration> colsToCollect = new HashMap<>();
            Set<String> colsToRmv = new HashSet<>();

            if (!F.isEmpty(partStats)) {
                for (ObjectPartitionStatisticsImpl pstat : partStats) {
                    if (!partsOwn.contains(pstat.partId()))
                        partsToRmv.add(pstat.partId());

                    boolean partExists = true;

                    for (StatisticsColumnConfiguration colCfg : cfg.columnsAll().values()) {
                        ColumnStatistics colStat = pstat.columnStatistics(colCfg.name());

                        if (colCfg.tombstone()) {
                            if (colStat != null)
                                colsToRmv.add(colCfg.name());
                        }
                        else {
                            if (colStat == null || colStat.version() < colCfg.version()) {
                                colsToCollect.put(colCfg.name(), colCfg);

                                partsToCollect.add(pstat.partId());

                                partExists = false;
                            }
                        }
                    }

                    if (partExists)
                        partsToCollect.remove(pstat.partId());
                }
            }

            if (!F.isEmpty(partsToRmv)) {
                if (log.isDebugEnabled()) {
                    log.debug("Remove local partitioned statistics [key=" + cfg.key() +
                        ", part=" + partsToRmv + ']');
                }

                partsToRmv.forEach(p -> {
                    assert !partsToCollect.contains(p);

                    repo.clearLocalPartitionStatistics(cfg.key(), p);
                });
            }

            if (!F.isEmpty(colsToRmv))
                dropColumnsOnLocalStatistics(cfg, colsToRmv);

            if (!F.isEmpty(partsToCollect))
                gatherLocalStatistics(cfg, tbl, parts, partsToCollect, colsToCollect);
            else {
                // All local statistics by partition are available.
                // Only refresh aggregated local statistics.
                gatherer.aggregateStatisticsAsync(cfg.key(), () -> aggregateLocalGathering(cfg.key(), parts));
            }
        }
        catch (Throwable ex) {
            log.error("Unexpected error on check local statistics", ex);
        }
    }

    /**
     * Match local statistic with changes of statistic configuration:
     * - update statistics;
     * - drop columns;
     * - add new columns to collect statistics.
     *
     * The method is called on change statistic configuration object at the distributed metastorage.
     */
    private void onChangeStatisticConfiguration(
        StatisticsObjectConfiguration oldCfg,
        final StatisticsObjectConfiguration newCfg
    ) {
        synchronized (mux) {
            if (log.isDebugEnabled())
                log.debug("Statistic configuration changed [old=" + oldCfg + ", new=" + newCfg + ']');

            StatisticsObjectConfiguration.Diff diff = StatisticsObjectConfiguration.diff(oldCfg, newCfg);

            if (!F.isEmpty(diff.dropCols()))
                dropColumnsOnLocalStatistics(newCfg, diff.dropCols());

            if (!F.isEmpty(diff.updateCols())) {
                GridH2Table tbl = schemaMgr.dataTable(newCfg.key().schema(), newCfg.key().obj());

                GridCacheContext cctx = tbl.cacheContext();

                Set<Integer> parts = cctx.affinity().primaryPartitions(
                    cctx.localNodeId(), cctx.affinity().affinityTopologyVersion());

                gatherLocalStatistics(
                    newCfg,
                    tbl,
                    parts,
                    parts,
                    diff.updateCols()
                );
            }
        }
    }

    /** */
    private void gatherLocalStatistics(
        StatisticsObjectConfiguration cfg,
        GridH2Table tbl,
        Set<Integer> partsToAggregate,
        Set<Integer> partsToCollect,
        Map<String, StatisticsColumnConfiguration> colsToCollect
    ) {
        if (F.isEmpty(colsToCollect))
            colsToCollect = cfg.columns();

        Column[] cols = IgniteStatisticsHelper.filterColumns(tbl.getColumns(), colsToCollect.keySet());

        gatherer.gatherLocalObjectsStatisticsAsync(tbl, cols, colsToCollect, partsToCollect);

        gatherer.aggregateStatisticsAsync(cfg.key(), () -> aggregateLocalGathering(cfg.key(), partsToAggregate));
    }

    /** */
    private void dropColumnsOnLocalStatistics(StatisticsObjectConfiguration cfg, Set<String> cols) {
        if (log.isDebugEnabled()) {
            log.debug("Remove local statistics [key=" + cfg.key() +
                ", columns=" + cols + ']');
        }

        LocalStatisticsGatheringContext gCtx = gatherer.gatheringInProgress(cfg.key());

        if (gCtx != null) {
            gCtx.futureAggregate().thenAccept((v) -> {
                repo.clearLocalStatistics(cfg.key(), cols);
                repo.clearLocalPartitionsStatistics(cfg.key(), cols);
            });
        }
        else {
            repo.clearLocalStatistics(cfg.key(), cols);
            repo.clearLocalPartitionsStatistics(cfg.key(), cols);
        }
    }

    /** */
    private void onDropColumns(GridH2Table tbl, List<String> cols) {
        assert !F.isEmpty(cols);

        dropStatistics(
            Collections.singletonList(
                new StatisticsTarget(
                    tbl.identifier().schema(),
                    tbl.getName(),
                    cols.toArray(IgniteUtils.EMPTY_STRINGS)
                )
            ),
            false
        );
    }

    /** */
    private void onDropTable(String schema, String name) {
        assert !F.isEmpty(schema) && !F.isEmpty(name) : schema + ":" + name;

        StatisticsKey key = new StatisticsKey(schema, name);

        try {
            StatisticsObjectConfiguration cfg = config(key);

            if (cfg != null && !F.isEmpty(cfg.columns()))
                dropStatistics(Collections.singletonList(new StatisticsTarget(schema, name)), false);
        }
        catch (Throwable e) {
            if (!X.hasCause(e, NodeStoppingException.class))
                throw new IgniteSQLException("Error on drop statistics for dropped table [key=" + key + ']', e);
        }
    }

    /** */
    private ObjectStatisticsImpl aggregateLocalGathering(StatisticsKey key, Set<Integer> partsToAggregate) {
        synchronized (mux) {
            try {
                StatisticsObjectConfiguration cfg = distrMetaStorage.read(key2String(key));

                return repo.aggregatedLocalStatistics(partsToAggregate, cfg);
            }
            catch (Throwable e) {
                if (!X.hasCause(e, NodeStoppingException.class)) {
                    log.error("Error on aggregate statistic on finish local statistics collection" +
                        " [key=" + key + ", parts=" + partsToAggregate, e);
                }

                return null;
            }
        }
    }

    /** */
    public StatisticsObjectConfiguration config(StatisticsKey key) throws IgniteCheckedException {
        return distrMetaStorage.read(key2String(key));
    }

    /** */
    private static String key2String(StatisticsKey key) {
        StringBuilder sb = new StringBuilder(STAT_OBJ_PREFIX);

        sb.append(key.schema()).append('.').append(key.obj());

        return sb.toString();
    }
}
