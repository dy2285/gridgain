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

package org.apache.ignite.internal.visor.node;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.internal.visor.VisorDataTransferObject;
import org.apache.ignite.internal.visor.cache.VisorCache;
import org.apache.ignite.internal.visor.cache.VisorMemoryMetrics;
import org.apache.ignite.internal.visor.event.VisorGridEvent;
import org.apache.ignite.internal.visor.util.VisorExceptionWrapper;

/**
 * Data collector task result.
 */
public class VisorNodeDataCollectorTaskResult extends VisorDataTransferObject {
    /** */
    private static final long serialVersionUID = 0L;

    /** Grid active flag. */
    private boolean active;

    /** Unhandled exceptions from nodes. */
    private Map<UUID, VisorExceptionWrapper> unhandledEx = new HashMap<>();

    /** Nodes grid names. */
    private Map<UUID, String> gridNames = new HashMap<>();

    /** Nodes topology versions. */
    private Map<UUID, Long> topVersions = new HashMap<>();

    /** All task monitoring state collected from nodes. */
    private Map<UUID, Boolean> taskMonitoringEnabled = new HashMap<>();

    /** Nodes error counts. */
    private Map<UUID, Long> errCnts = new HashMap<>();

    /** All events collected from nodes. */
    private List<VisorGridEvent> evts = new ArrayList<>();

    /** Exceptions caught during collecting events from nodes. */
    private Map<UUID, VisorExceptionWrapper> evtsEx = new HashMap<>();

    /** All data region metrics collected from nodes. */
    private Map<UUID, Collection<VisorMemoryMetrics>> memoryMetrics = new HashMap<>();

    /** Exceptions caught during collecting memory metrics from nodes. */
    private Map<UUID, VisorExceptionWrapper> memoryMetricsEx = new HashMap<>();

    /** All caches collected from nodes. */
    private Map<UUID, Collection<VisorCache>> caches = new HashMap<>();

    /** Exceptions caught during collecting caches from nodes. */
    private Map<UUID, VisorExceptionWrapper> cachesEx = new HashMap<>();

    /** Topology version of latest completed partition exchange from nodes. */
    private Map<UUID, VisorAffinityTopologyVersion> readyTopVers = new HashMap<>();

    /** Whether pending exchange future exists from nodes. */
    private Map<UUID, Boolean> pendingExchanges = new HashMap<>();

    /** All persistence metrics collected from nodes. */
    private Map<UUID, VisorPersistenceMetrics> persistenceMetrics = new HashMap<>();

    /** Exceptions caught during collecting persistence metrics from nodes. */
    private Map<UUID, VisorExceptionWrapper> persistenceMetricsEx = new HashMap<>();

    /** Rebalance state on nodes. */
    private Map<UUID, Double> rebalance = new HashMap<>();

    /**
     * Default constructor.
     */
    public VisorNodeDataCollectorTaskResult() {
        // No-op.
    }

    /**
     * @return {@code true} If no data was collected.
     */
    public boolean isEmpty() {
        return
            gridNames.isEmpty() &&
            topVersions.isEmpty() &&
            unhandledEx.isEmpty() &&
            taskMonitoringEnabled.isEmpty() &&
            evts.isEmpty() &&
            evtsEx.isEmpty() &&
            memoryMetrics.isEmpty() &&
            memoryMetricsEx.isEmpty() &&
            caches.isEmpty() &&
            cachesEx.isEmpty() &&
            readyTopVers.isEmpty() &&
            pendingExchanges.isEmpty() &&
            persistenceMetrics.isEmpty() &&
            persistenceMetricsEx.isEmpty() &&
            rebalance.isEmpty();
    }

    /**
     * @return {@code True} if grid is active.
     */
    public boolean isActive() {
        return active;
    }

    /**
     * @param active active New value of grid active flag.
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * @return Unhandled exceptions from nodes.
     */
    public Map<UUID, VisorExceptionWrapper> getUnhandledEx() {
        return unhandledEx;
    }

    /**
     * @return Nodes grid names.
     */
    public Map<UUID, String> getGridNames() {
        return gridNames;
    }

    /**
     * @return Nodes topology versions.
     */
    public Map<UUID, Long> getTopologyVersions() {
        return topVersions;
    }

    /**
     * @return All task monitoring state collected from nodes.
     */
    public Map<UUID, Boolean> getTaskMonitoringEnabled() {
        return taskMonitoringEnabled;
    }

    /**
     * @return All events collected from nodes.
     */
    public List<VisorGridEvent> getEvents() {
        return evts;
    }

    /**
     * @return Exceptions caught during collecting events from nodes.
     */
    public Map<UUID, VisorExceptionWrapper> getEventsEx() {
        return evtsEx;
    }

    /**
     * @return All data region metrics collected from nodes.
     */
    public Map<UUID, Collection<VisorMemoryMetrics>> getMemoryMetrics() {
        return memoryMetrics;
    }

    /**
     * @return Exceptions caught during collecting memory metrics from nodes.
     */
    public Map<UUID, VisorExceptionWrapper> getMemoryMetricsEx() {
        return memoryMetricsEx;
    }

    /**
     * @return All caches collected from nodes.
     */
    public Map<UUID, Collection<VisorCache>> getCaches() {
        return caches;
    }

    /**
     * @return Exceptions caught during collecting caches from nodes.
     */
    public Map<UUID, VisorExceptionWrapper> getCachesEx() {
        return cachesEx;
    }

    /**
     * @return Nodes error counts.
     */
    public Map<UUID, Long> getErrorCounts() {
        return errCnts;
    }

    /**
     * @return Topology version of latest completed partition exchange from nodes.
     */
    public Map<UUID, VisorAffinityTopologyVersion> getReadyAffinityVersions() {
        return readyTopVers;
    }

    /**
     * @return Whether pending exchange future exists from nodes.
     */
    public Map<UUID, Boolean> getPendingExchanges() {
        return pendingExchanges;
    }

    /**
     * All persistence metrics collected from nodes.
     */
    public Map<UUID, VisorPersistenceMetrics> getPersistenceMetrics() {
        return persistenceMetrics;
    }

    /**
     * @return Exceptions caught during collecting persistence metrics from nodes.
     */
    public Map<UUID, VisorExceptionWrapper> getPersistenceMetricsEx() {
        return persistenceMetricsEx;
    }

    /**
     * @return Rebalance on nodes.
     */
    public Map<UUID, Double> getRebalance() {
        return rebalance;
    }

    /** {@inheritDoc} */
    @Override public byte getProtocolVersion() {
        return V2;
    }

    /**
     * Add specified results.
     *
     * @param res Results to add.
     */
    public void add(VisorNodeDataCollectorTaskResult res) {
        assert res != null;

        active = active || res.isActive();
        unhandledEx.putAll(res.getUnhandledEx());
        gridNames.putAll(res.getGridNames());
        topVersions.putAll(res.getTopologyVersions());
        taskMonitoringEnabled.putAll(res.getTaskMonitoringEnabled());
        errCnts.putAll(res.getErrorCounts());
        evts.addAll(res.getEvents());
        evtsEx.putAll(res.getEventsEx());
        memoryMetrics.putAll(res.getMemoryMetrics());
        memoryMetricsEx.putAll(res.getMemoryMetricsEx());
        caches.putAll(res.getCaches());
        cachesEx.putAll(res.getCachesEx());
        readyTopVers.putAll(res.getReadyAffinityVersions());
        pendingExchanges.putAll(res.getPendingExchanges());
        persistenceMetrics.putAll(res.getPersistenceMetrics());
        persistenceMetricsEx.putAll(res.getPersistenceMetricsEx());
        rebalance.putAll(res.getRebalance());
    }

    /** {@inheritDoc} */
    @Override protected void writeExternalData(ObjectOutput out) throws IOException {
        out.writeBoolean(active);
        U.writeMap(out, unhandledEx);
        U.writeMap(out, gridNames);
        U.writeMap(out, topVersions);
        U.writeMap(out, taskMonitoringEnabled);
        U.writeMap(out, errCnts);
        U.writeCollection(out, evts);
        U.writeMap(out, evtsEx);
        U.writeMap(out, memoryMetrics);
        U.writeMap(out, memoryMetricsEx);
        U.writeMap(out, caches);
        U.writeMap(out, cachesEx);
        U.writeMap(out, readyTopVers);
        U.writeMap(out, pendingExchanges);
        U.writeMap(out, persistenceMetrics);
        U.writeMap(out, persistenceMetricsEx);
        U.writeMap(out, rebalance);
    }

    /** {@inheritDoc} */
    @Override protected void readExternalData(byte protoVer, ObjectInput in) throws IOException, ClassNotFoundException {
        active = in.readBoolean();
        unhandledEx = U.readMap(in);
        gridNames = U.readMap(in);
        topVersions = U.readMap(in);
        taskMonitoringEnabled = U.readMap(in);
        errCnts = U.readMap(in);
        evts = U.readList(in);
        evtsEx = U.readMap(in);
        memoryMetrics = U.readMap(in);
        memoryMetricsEx = U.readMap(in);
        caches = U.readMap(in);
        cachesEx = U.readMap(in);
        readyTopVers = U.readMap(in);
        pendingExchanges = U.readMap(in);
        persistenceMetrics = U.readMap(in);
        persistenceMetricsEx = U.readMap(in);

        if (protoVer > V1)
            rebalance = U.readMap(in);
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(VisorNodeDataCollectorTaskResult.class, this);
    }
}
