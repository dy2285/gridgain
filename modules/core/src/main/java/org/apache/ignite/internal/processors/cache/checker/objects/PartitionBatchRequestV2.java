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

package org.apache.ignite.internal.processors.cache.checker.objects;

import java.util.Map;
import java.util.UUID;
import org.apache.ignite.internal.processors.affinity.AffinityTopologyVersion;
import org.apache.ignite.internal.processors.cache.KeyCacheObject;
import org.apache.ignite.internal.processors.cache.verify.RepairAlgorithm;
import org.apache.ignite.internal.util.typedef.internal.S;

/**
 * Pageable partition batch request.
 */
public class PartitionBatchRequestV2 extends PartitionBatchRequest {
    /** If {@code true} - reconciliation of data consistency is needed. */
    private boolean dataReconciliation;

    /** If {@code true} - reconciliation of cache size consistency is needed. */
    private boolean cacheSizeReconciliation;

    /** If {@code true} - Partition Reconciliation&Fix. */
    private boolean repair;

    /**
     * Specifies which fix algorithm to use: options {@code PartitionReconciliationRepairMeta.RepairAlg} while
     * repairing doubtful keys.
     */
    private RepairAlgorithm repairAlg;

    /**
     *
     */
    private static final long serialVersionUID = 0L;

    /**
     *
     */
    private Map<UUID, NodePartitionSize> partSizesMap;

    /**
     * @param dataReconciliation Flag indicates that data consistency reconciliation is requested.
     * @param cacheSizeReconciliation Flag indicates that cache size consistency reconciliation is requested.
     * @param repair Flag indicates that an inconsistency should be fixed in accordance with {@code repairAlg} parameter.
     * @param repairAlg Partition reconciliation repair algorithm to be used.
     * @param sesId Session id.
     * @param workloadChainId Workload chain id.
     * @param cacheName Cache name.
     * @param partId Partition id.
     * @param batchSize Batch size.
     * @param lowerKey Lower key.
     * @param partSizesMap Map of partition sizes.
     * @param startTopVer Start topology version.
     */
    public PartitionBatchRequestV2(
        boolean dataReconciliation,
        boolean cacheSizeReconciliation,
        boolean repair,
        RepairAlgorithm repairAlg,
        long sesId,
        UUID workloadChainId,
        String cacheName,
        int partId,
        int batchSize,
        KeyCacheObject lowerKey,
        Map<UUID, NodePartitionSize> partSizesMap,
        AffinityTopologyVersion startTopVer
    ) {
        super(sesId, workloadChainId, cacheName, partId, batchSize, lowerKey, startTopVer);
        this.dataReconciliation = dataReconciliation;
        this.cacheSizeReconciliation = cacheSizeReconciliation;
        this.repair = repair;
        this.repairAlg = repairAlg;
        this.partSizesMap = partSizesMap;
    }

    /**
     * @return Data reconciliation.
     */
    public boolean dataReconciliation() {
        return dataReconciliation;
    }

    /**
     * @return Cache size reconciliation.
     */
    public boolean cacheSizeReconciliation() {
        return cacheSizeReconciliation;
    }

    /**
     * @return Repair.
     */
    public boolean repair() {
        return repair;
    }

    /**
     * @return Map of partition sizes.
     */
    public Map<UUID, NodePartitionSize> partSizesMap() {
        return partSizesMap;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(PartitionBatchRequestV2.class, this);
    }
}
