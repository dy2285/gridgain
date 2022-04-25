/*
 * Copyright 2022 GridGain Systems, Inc. and Contributors.
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

package org.apache.ignite.internal.visor.verify;

import static org.apache.ignite.internal.util.IgniteUtils.readSet;
import static org.apache.ignite.internal.util.IgniteUtils.writeCollection;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;
import org.apache.ignite.internal.dto.IgniteDataTransferObject;
import org.apache.ignite.internal.util.typedef.internal.S;

/**
 * Validate dr cache task arguments.
 */
public class VisorDrValidateCacheTaskArg extends IgniteDataTransferObject {
    /** */
    private static final long serialVersionUID = 0L;

    /** Caches. */
    private Set<String> caches;

    /** Check first K elements. */
    private int checkFirst = -1;

    /**
     * Default constructor.
     */
    public VisorDrValidateCacheTaskArg() {
        // No-op.
    }

    /**
     * Constructor.
     *
     * @param caches Caches.
     * @param checkFirst Check first K elements.
     */
    public VisorDrValidateCacheTaskArg(
        Set<String> caches,
        int checkFirst
    ) {
        this.caches = caches;
        this.checkFirst = checkFirst;
    }

    /**
     * @return Caches.
     */
    public Set<String> getCaches() {
        return caches;
    }

    /**
     * @return checkFirst.
     */
    public int getCheckFirst() {
        return checkFirst;
    }

    /** {@inheritDoc} */
    @Override protected void writeExternalData(ObjectOutput out) throws IOException {
        writeCollection(out, caches);
        out.writeInt(checkFirst);
    }

    /** {@inheritDoc} */
    @Override protected void readExternalData(byte protoVer, ObjectInput in) throws IOException, ClassNotFoundException {
        caches = readSet(in);

        checkFirst = in.readInt();
    }

    /** {@inheritDoc} */
    @Override public byte getProtocolVersion() {
        return V1;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(VisorDrValidateCacheTaskArg.class, this);
    }
}
