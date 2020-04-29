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
import java.util.Set;
import org.apache.ignite.internal.dto.IgniteDataTransferObject;
import org.apache.ignite.internal.util.typedef.internal.U;

/**
 * Argument for {@link IndexForceRebuildTask}.
 */
public class IndexForceRebuildTaskArg extends IgniteDataTransferObject {
    /** */
    private static final long serialVersionUID = 0L;

    /** Cache group name. */
    private Set<String> cacheGrps;

    /** Cache name. */
    private Set<String> cacheNames;

    /**
     * Empty constructor required for Serializable.
     */
    public IndexForceRebuildTaskArg() {
        // No-op.
    }

    /** */
    public IndexForceRebuildTaskArg(Set<String> cacheGrps, Set<String> cacheNames) {
        this.cacheGrps = cacheGrps;
        this.cacheNames = cacheNames;
    }

    /** {@inheritDoc} */
    @Override protected void writeExternalData(ObjectOutput out) throws IOException {
        U.writeCollection(out, cacheGrps);
        U.writeCollection(out, cacheNames);
    }

    /** {@inheritDoc} */
    @Override
    protected void readExternalData(byte protoVer, ObjectInput in) throws IOException, ClassNotFoundException {
        cacheGrps = U.readSet(in);
        cacheNames = U.readSet(in);
    }

    /**
     * @return Cache group name.
     */
    public Set<String> cacheGrps() {
        return cacheGrps;
    }

    /**
     * @return Cache name.
     */
    public Set<String> cacheNames() {
        return cacheNames;
    }
}
