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

package org.apache.ignite.internal.visor.encryption;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.apache.ignite.internal.dto.IgniteDataTransferObject;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.jetbrains.annotations.Nullable;

/**
 * Re-encryption rate task argument.
 */
public class VisorReencryptionRateTaskArg extends IgniteDataTransferObject {
    /** */
    private static final long serialVersionUID = 0L;

    /** Re-encryption rate limit in megabytes per second. */
    private Double rate;

    /** Default constructor. */
    public VisorReencryptionRateTaskArg() {
        // No-op.
    }

    /**
     * @param rate Re-encryption rate limit in megabytes per second.
     */
    public VisorReencryptionRateTaskArg(Double rate) {
        this.rate = rate;
    }

    /**
     * @return Re-encryption rate limit in megabytes per second.
     */
    public @Nullable Double rate() {
        return rate;
    }

    /** {@inheritDoc} */
    @Override protected void writeExternalData(ObjectOutput out) throws IOException {
        out.writeBoolean(rate != null);

        if (rate != null)
            out.writeDouble(rate);
    }

    /** {@inheritDoc} */
    @Override protected void readExternalData(byte ver, ObjectInput in) throws IOException, ClassNotFoundException {
        if (in.readBoolean())
            rate = in.readDouble();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(VisorReencryptionRateTaskArg.class, this);
    }
}
