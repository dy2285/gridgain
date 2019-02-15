/*
 *                   GridGain Community Edition Licensing
 *                   Copyright 2019 GridGain Systems, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License") modified with Commons Clause
 * Restriction; you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * Commons Clause Restriction
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including without
 * limitation fees for hosting or consulting/ support services related to the Software), a product or
 * service whose value derives, entirely or substantially, from the functionality of the Software.
 * Any license notice or attribution required by the License must also include this Commons Clause
 * License Condition notice.
 *
 * For purposes of the clause above, the “Licensor” is Copyright 2019 GridGain Systems, Inc.,
 * the “License” is the Apache License, Version 2.0, and the Software is the GridGain Community
 * Edition software provided with this notice.
 */

package org.apache.ignite.spi.communication;

import java.nio.ByteBuffer;
import java.util.UUID;
import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.plugin.extensions.communication.Message;
import org.apache.ignite.plugin.extensions.communication.MessageReader;
import org.apache.ignite.plugin.extensions.communication.MessageWriter;

/**
 * Test message for communication SPI tests.
 */
public class GridTestMessage implements Message {
    /** */
    public static final short DIRECT_TYPE = 200;

    /** */
    private UUID srcNodeId;

    /** */
    private long msgId;

    /** */
    private long resId;

    /** Network payload */
    private byte[] payload;

    /** */
    public GridTestMessage() {
        // No-op.
    }

    /**
     * @param srcNodeId Node that originated message.
     * @param msgId Message sequence id.
     * @param resId Response id.
     */
    public GridTestMessage(UUID srcNodeId, long msgId, long resId) {
        this.srcNodeId = srcNodeId;
        this.msgId = msgId;
        this.resId = resId;
    }

    /**
     * @return Id of message originator.
     */
    public UUID getSourceNodeId() {
        return srcNodeId;
    }

    /**
     * @return Message sequence id.
     */
    public long getMsgId() {
        return msgId;
    }

    /**
     * @return Response id.
     */
    public long getResponseId() {
        return resId;
    }

    /**
     * @param payload Payload to be set.
     */
    public void payload(byte[] payload) {
        this.payload = payload;
    }

    /**
     * @return Network payload.
     */
    public byte[] payload() {
        return payload;
    }

    /** {@inheritDoc} */
    @Override public void onAckReceived() {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public boolean writeTo(ByteBuffer buf, MessageWriter writer) {
        writer.setBuffer(buf);

        if (!writer.isHeaderWritten()) {
            if (!writer.writeHeader(directType(), fieldsCount()))
                return false;

            writer.onHeaderWritten();
        }

        switch (writer.state()) {
            case 0:
                if (!writer.writeUuid(null, srcNodeId))
                    return false;

                writer.incrementState();

            case 1:
                if (!writer.writeLong(null, msgId))
                    return false;

                writer.incrementState();

            case 2:
                if (!writer.writeLong(null, resId))
                    return false;

                writer.incrementState();

            case 3:
                if (!writer.writeByteArray(null, payload))
                    return false;

                writer.incrementState();
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override public boolean readFrom(ByteBuffer buf, MessageReader reader) {
        reader.setBuffer(buf);

        switch (reader.state()) {
            case 0:
                srcNodeId = reader.readUuid(null);

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

            case 1:
                msgId = reader.readLong(null);

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

            case 2:
                resId = reader.readLong(null);

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();

            case 3:
                payload = reader.readByteArray(null);

                if (!reader.isLastRead())
                    return false;

                reader.incrementState();
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override public short directType() {
        return DIRECT_TYPE;
    }

    /** {@inheritDoc} */
    @Override public byte fieldsCount() {
        return 4;
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object o) {
        if (this == o)
            return true;

        if (!(o instanceof GridTestMessage))
            return false;

        GridTestMessage m = (GridTestMessage)o;

        return F.eq(srcNodeId, m.srcNodeId) && F.eq(msgId, m.msgId) && F.eq(resId, m.resId);
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        int res = srcNodeId.hashCode();

        res = 31 * res + (int)(msgId ^ (msgId >>> 32));
        res = 31 * res + (int)(resId ^ (resId >>> 32));

        return res;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        StringBuilder buf = new StringBuilder();

        buf.append(getClass().getSimpleName());
        buf.append(" [srcNodeId=").append(srcNodeId);
        buf.append(", msgId=").append(msgId);
        buf.append(", resId=").append(resId);
        buf.append(']');

        return buf.toString();
    }
}