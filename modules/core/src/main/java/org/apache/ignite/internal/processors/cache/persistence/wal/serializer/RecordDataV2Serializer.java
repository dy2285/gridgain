/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.processors.cache.persistence.wal.serializer;

import java.io.DataInput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.internal.pagemem.wal.record.CacheState;
import org.apache.ignite.internal.pagemem.wal.record.CheckpointRecord;
import org.apache.ignite.internal.pagemem.wal.record.DataEntry;
import org.apache.ignite.internal.pagemem.wal.record.DataRecord;
import org.apache.ignite.internal.pagemem.wal.record.ExchangeRecord;
import org.apache.ignite.internal.pagemem.wal.record.RollbackRecord;
import org.apache.ignite.internal.pagemem.wal.record.SnapshotRecord;
import org.apache.ignite.internal.pagemem.wal.record.TxRecord;
import org.apache.ignite.internal.pagemem.wal.record.WALRecord;
import org.apache.ignite.internal.pagemem.wal.record.delta.TrackingPageRepairDeltaRecord;
import org.apache.ignite.internal.processors.cache.persistence.wal.ByteBufferBackedDataInput;
import org.apache.ignite.internal.processors.cache.persistence.wal.FileWALPointer;
import org.apache.ignite.internal.processors.cache.persistence.wal.record.HeaderRecord;

/**
 * Record data V2 serializer.
 */
public class RecordDataV2Serializer implements RecordDataSerializer {
    /** Length of HEADER record data. */
    static final int HEADER_RECORD_DATA_SIZE = /*Magic*/8 + /*Version*/4;

    /** V1 data serializer delegate. */
    private final RecordDataV1Serializer delegateSerializer;

    /** Serializer of {@link TxRecord} records. */
    private final TxRecordSerializer txRecordSerializer;

    /**
     * Create an instance of V2 data serializer.
     *
     * @param delegateSerializer V1 data serializer.
     */
    public RecordDataV2Serializer(RecordDataV1Serializer delegateSerializer) {
        this.delegateSerializer = delegateSerializer;
        this.txRecordSerializer = new TxRecordSerializer();
    }

    /** {@inheritDoc} */
    @Override public int size(WALRecord rec) throws IgniteCheckedException {
        switch (rec.type()) {
            case HEADER_RECORD:
                return HEADER_RECORD_DATA_SIZE;

            case CHECKPOINT_RECORD:
                CheckpointRecord cpRec = (CheckpointRecord)rec;

                assert cpRec.checkpointMark() == null || cpRec.checkpointMark() instanceof FileWALPointer :
                    "Invalid WAL record: " + cpRec;

                int cacheStatesSize = cacheStatesSize(cpRec.cacheGroupStates());

                FileWALPointer walPtr = (FileWALPointer)cpRec.checkpointMark();

                return 18 + cacheStatesSize + (walPtr == null ? 0 : 16);

            case DATA_RECORD:
                return delegateSerializer.size(rec) + 8/*timestamp*/;

            case SNAPSHOT:
                return 8 + 1;

            case EXCHANGE:
                return 4 /*type*/ + 8 /*timestamp*/ + 2 /*constId*/;

            case TX_RECORD:
                return txRecordSerializer.size((TxRecord)rec);

            case ROLLBACK_TX_RECORD:
                return 4 + 4 + 8 + 8;

            case TRACKING_PAGE_REPAIR_DELTA:
                return 4 + 8;

            default:
                return delegateSerializer.size(rec);
        }
    }

    /** {@inheritDoc} */
    @Override public WALRecord readRecord(
        WALRecord.RecordType type,
        ByteBufferBackedDataInput in
    ) throws IOException, IgniteCheckedException {
        switch (type) {
            case CHECKPOINT_RECORD:
                long msb = in.readLong();
                long lsb = in.readLong();
                boolean hasPtr = in.readByte() != 0;
                long idx0 = hasPtr ? in.readLong() : 0;
                int off = hasPtr ? in.readInt() : 0;
                int len = hasPtr ? in.readInt() : 0;

                Map<Integer, CacheState> states = readPartitionStates(in);

                boolean end = in.readByte() != 0;

                FileWALPointer walPtr = hasPtr ? new FileWALPointer(idx0, off, len) : null;

                CheckpointRecord cpRec = new CheckpointRecord(new UUID(msb, lsb), walPtr, end);

                cpRec.cacheGroupStates(states);

                return cpRec;

            case DATA_RECORD:
                int entryCnt = in.readInt();
                long timeStamp = in.readLong();

                List<DataEntry> entries = new ArrayList<>(entryCnt);

                for (int i = 0; i < entryCnt; i++)
                    entries.add(delegateSerializer.readDataEntry(in));

                return new DataRecord(entries, timeStamp);
            case SNAPSHOT:
                long snpId = in.readLong();
                byte full = in.readByte();

                return new SnapshotRecord(snpId, full == 1);

            case EXCHANGE:
                int idx = in.readInt();
                short constId = in.readShort();
                long ts = in.readLong();

                return new ExchangeRecord(constId, ExchangeRecord.Type.values()[idx], ts);

            case TX_RECORD:
                return txRecordSerializer.read(in);

            case ROLLBACK_TX_RECORD:
                int grpId = in.readInt();
                int partId = in.readInt();
                long start = in.readLong();
                long range = in.readLong();

                return new RollbackRecord(grpId, partId, start, range);

            case TRACKING_PAGE_REPAIR_DELTA:
                int cacheId = in.readInt();
                long pageId = in.readLong();

                return new TrackingPageRepairDeltaRecord(cacheId, pageId);

            default:
                return delegateSerializer.readRecord(type, in);
        }

    }

    /** {@inheritDoc} */
    @Override public void writeRecord(WALRecord rec, ByteBuffer buf) throws IgniteCheckedException {
        if (rec instanceof HeaderRecord)
            throw new UnsupportedOperationException("Writing header records is forbidden since version 2 of serializer");

        switch (rec.type()) {
            case CHECKPOINT_RECORD:
                CheckpointRecord cpRec = (CheckpointRecord)rec;

                assert cpRec.checkpointMark() == null || cpRec.checkpointMark() instanceof FileWALPointer :
                    "Invalid WAL record: " + cpRec;

                FileWALPointer walPtr = (FileWALPointer)cpRec.checkpointMark();
                UUID cpId = cpRec.checkpointId();

                buf.putLong(cpId.getMostSignificantBits());
                buf.putLong(cpId.getLeastSignificantBits());

                buf.put(walPtr == null ? (byte)0 : 1);

                if (walPtr != null) {
                    buf.putLong(walPtr.index());
                    buf.putInt(walPtr.fileOffset());
                    buf.putInt(walPtr.length());
                }

                putCacheStates(buf, cpRec.cacheGroupStates());

                buf.put(cpRec.end() ? (byte)1 : 0);

                break;

            case DATA_RECORD:
                DataRecord dataRec = (DataRecord)rec;

                buf.putInt(dataRec.writeEntries().size());
                buf.putLong(dataRec.timestamp());

                for (DataEntry dataEntry : dataRec.writeEntries())
                    RecordDataV1Serializer.putDataEntry(buf, dataEntry);

                break;

            case SNAPSHOT:
                SnapshotRecord snpRec = (SnapshotRecord)rec;

                buf.putLong(snpRec.getSnapshotId());
                buf.put(snpRec.isFull() ? (byte)1 : 0);

                break;

            case EXCHANGE:
                ExchangeRecord r = (ExchangeRecord)rec;

                buf.putInt(r.getType().ordinal());
                buf.putShort(r.getConstId());
                buf.putLong(r.timestamp());

                break;

            case TX_RECORD:
                txRecordSerializer.write((TxRecord)rec, buf);

                break;

            case ROLLBACK_TX_RECORD:
                RollbackRecord rb = (RollbackRecord)rec;

                buf.putInt(rb.groupId());
                buf.putInt(rb.partitionId());
                buf.putLong(rb.start());
                buf.putLong(rb.range());

                break;

            case TRACKING_PAGE_REPAIR_DELTA:
                TrackingPageRepairDeltaRecord tprDelta = (TrackingPageRepairDeltaRecord)rec;

                buf.putInt(tprDelta.groupId());
                buf.putLong(tprDelta.pageId());

                break;

            default:
                delegateSerializer.writeRecord(rec, buf);
        }
    }

    /**
     * @param buf Buffer to read from.
     * @return Read map.
     */
    private Map<Integer, CacheState> readPartitionStates(DataInput buf) throws IOException {
        int caches = buf.readShort() & 0xFFFF;

        if (caches == 0)
            return Collections.emptyMap();

        Map<Integer, CacheState> states = new HashMap<>(caches, 1.0f);

        for (int i = 0; i < caches; i++) {
            int cacheId = buf.readInt();

            int parts = buf.readShort() & 0xFFFF;

            CacheState state = new CacheState(parts);

            for (int p = 0; p < parts; p++) {
                int partId = buf.readShort() & 0xFFFF;
                long size = buf.readLong();
                long partCntr = buf.readLong();
                byte partState = buf.readByte();

                state.addPartitionState(partId, size, partCntr, partState);
            }

            states.put(cacheId, state);
        }

        return states;
    }

    /**
     * @param states Cache states.
     */
    private static void putCacheStates(ByteBuffer buf, Map<Integer, CacheState> states) {
        buf.putShort((short)states.size());

        for (Map.Entry<Integer, CacheState> entry : states.entrySet()) {
            buf.putInt(entry.getKey());

            CacheState state = entry.getValue();

            // Need 2 bytes for the number of partitions.
            buf.putShort((short)state.size());

            for (int i = 0; i < state.size(); i++) {
                short partIdx = (short)state.partitionByIndex(i);

                buf.putShort(partIdx);

                buf.putLong(state.partitionSizeByIndex(i));
                buf.putLong(state.partitionCounterByIndex(i));
                buf.put(state.stateByIndex(i));
            }
        }
    }

    /**
     * @param states Partition states.
     * @return Size required to write partition states.
     */
    private int cacheStatesSize(Map<Integer, CacheState> states) {
        // Need 4 bytes for the number of caches.
        int size = 2;

        for (Map.Entry<Integer, CacheState> entry : states.entrySet()) {
            // Cache ID.
            size += 4;

            // Need 2 bytes for the number of partitions.
            size += 2;

            CacheState state = entry.getValue();

            // 2 bytes partition ID, size and counter per partition, part state.
            size += 19 * state.size();
        }

        return size;
    }
}
