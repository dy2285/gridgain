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

package org.apache.ignite.internal.processors.cache.checker.objects;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.ignite.internal.dto.IgniteDataTransferObject;
import org.apache.ignite.internal.util.typedef.internal.U;

/**
 * Common result of partition reconciliation.
 */
public class ReconciliationResult extends IgniteDataTransferObject {
    /** */
    private static final long serialVersionUID = 0L;

    /** Result. */
    private ReconciliationAffectedEntries res;

    /** Map of partition sizes for reconciliation of cache sizes. */
    private Map<Integer/*cache id*/, Map<Integer/*partition id*/, Map<UUID, NodePartitionSize>>> partSizesMap = new HashMap();

    /** Folders with local results. */
    private Map<UUID, String> nodeIdToFolder;

    /** Errors happened during execution. */
    private List<String> errors;

    /**
     * Default constructor.
     */
    public ReconciliationResult() {
    }

    /**
     * @param partReconciliationRes Partition reconciliation response.
     * @param nodeIdToFolder Node id to folder.
     * @param errors Errors.
     */
    public ReconciliationResult(
        ReconciliationAffectedEntries partReconciliationRes,
        Map<UUID, String> nodeIdToFolder,
        List<String> errors
    ) {
        this.res = partReconciliationRes;
        this.nodeIdToFolder = nodeIdToFolder;
        this.errors = errors;
    }

    /**
     * @param partReconciliationRes Partition reconciliation response.
     * @param partSizesMap Map of partition sizes for reconciliation of cache sizes.
     * @param nodeIdToFolder Node id to folder.
     * @param errors Errors.
     */
    public ReconciliationResult(
        ReconciliationAffectedEntries partReconciliationRes,
        Map<Integer, Map<Integer, Map<UUID, NodePartitionSize>>> partSizesMap,
        Map<UUID, String> nodeIdToFolder,
        List<String> errors
    ) {
        this.res = partReconciliationRes;
        this.partSizesMap = partSizesMap;
        this.nodeIdToFolder = nodeIdToFolder;
        this.errors = errors;
    }

    /** {@inheritDoc} */
    @Override protected void writeExternalData(ObjectOutput out) throws IOException {
        out.writeObject(res);
        U.writeMap(out, nodeIdToFolder);
        U.writeCollection(out, errors);
    }

    /** {@inheritDoc} */
    @Override protected void readExternalData(byte protoVer, ObjectInput in) throws IOException, ClassNotFoundException {
        res = (ReconciliationAffectedEntries)in.readObject();

        nodeIdToFolder = U.readMap(in);

        errors = U.readList(in);
    }

    /**
     * @return Result.
     */
    public ReconciliationAffectedEntries partitionReconciliationResult() {
        return res;
    }

    /**
     * @return Map of partition sizes for reconciliation of cache sizes.
     */
    public Map<Integer, Map<Integer, Map<UUID, NodePartitionSize>>> partSizesMap() {
        return partSizesMap;
    }

    /**
     * @return Folders with local results.
     */
    public Map<UUID, String> nodeIdToFolder() {
        return nodeIdToFolder;
    }

    /**
     * @return Errors happened during execution.
     */
    public List<String> errors() {
        return errors;
    }
}
