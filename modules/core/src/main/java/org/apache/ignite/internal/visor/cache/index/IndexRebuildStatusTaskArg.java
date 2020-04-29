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

package org.apache.ignite.internal.visor.cache.index;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.UUID;
import org.apache.ignite.internal.dto.IgniteDataTransferObject;
import org.apache.ignite.internal.util.typedef.internal.U;

/**
 * Argument for {@link IndexRebuildStatusTask}.
 */
public class IndexRebuildStatusTaskArg extends IgniteDataTransferObject {
    /** Required for serialization. */
    private static final long serialVersionUID = 0L;

    /** Node id. */
    private UUID nodeId;

    /**
     * Empty constructor required for Serializable.
     */
    public IndexRebuildStatusTaskArg() {
        // No-op.
    }

    /** */
    public IndexRebuildStatusTaskArg(UUID nodeId) {
        this.nodeId = nodeId;
    }

    /** {@inheritDoc} */
    @Override protected void writeExternalData(ObjectOutput out) throws IOException {
        U.writeUuid(out, nodeId);
    }

    /** {@inheritDoc} */
    @Override
    protected void readExternalData(byte protoVer, ObjectInput in) throws IOException, ClassNotFoundException {
        nodeId = U.readUuid(in);
    }

    /**
     * @return Node id.
     */
    public UUID nodeId() {
        return nodeId;
    }

    /**
     * @param nodeId New node id.
     */
    public void nodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }
}
