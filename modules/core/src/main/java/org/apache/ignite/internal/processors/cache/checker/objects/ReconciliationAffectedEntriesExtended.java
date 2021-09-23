/*
 * Copyright 2019 GridGain Systems, Inc. and Contributors.
 *
 * Licensed under the GridGain Community Edition License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.gridgain.com/products/software/community-edition/gridgain-community-edition-license
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
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.apache.ignite.internal.util.typedef.internal.U;

/**
 * Partition reconciliation result that contains only info about amount of inconsistent keys, skipped caches etc,
 * instead of full information. Used in case of non-console mode for console-scoped report.
 */
public class ReconciliationAffectedEntriesExtended extends ReconciliationAffectedEntries {
    /** */
    private static final long serialVersionUID = 0L;

    /** Inconsistent keys count. */
    private int inconsistentKeysCnt;

    /** Skipped caches count. */
    private int skippedCachesCnt;

    /** Skipped entries count. */
    private int skippedEntriesCnt;

    /** Partitions with broken size count. */
    private int partSizeConflictsCnt;

    /**
     * Default constructor for externalization.
     */
    public ReconciliationAffectedEntriesExtended() {
        // No-op
    }

    /**
     * Constructor.
     *
     * @param inconsistentKeysCnt Inconsistent keys count.
     * @param skippedCachesCnt Skipped caches count.
     * @param skippedEntriesCnt Skipped entries count.
     * @param partSizeConflictsCnt Partitions with broken size count.
     * @param partSizesMap Map of partition sizes.
     */
    public ReconciliationAffectedEntriesExtended(int inconsistentKeysCnt, int skippedCachesCnt, int skippedEntriesCnt,
        int partSizeConflictsCnt, Map<String, Map<Integer, Map<UUID, NodePartitionSize>>> partSizesMap) {
        this.inconsistentKeysCnt = inconsistentKeysCnt;
        this.skippedCachesCnt = skippedCachesCnt;
        this.skippedEntriesCnt = skippedEntriesCnt;
        this.partSizeConflictsCnt = partSizeConflictsCnt;
        this.partSizesMap = partSizesMap;
    }

    /** {@inheritDoc} */
    @Override public byte getProtocolVersion() {
        return V2;
    }

    /** {@inheritDoc} */
    @Override protected void writeExternalData(ObjectOutput out) throws IOException {
        out.writeInt(inconsistentKeysCnt);

        out.writeInt(skippedCachesCnt);

        out.writeInt(skippedEntriesCnt);

        out.writeInt(partSizeConflictsCnt);

        U.writeMap(out, partSizesMap);
    }

    /** {@inheritDoc} */
    @Override protected void readExternalData(byte protoVer, ObjectInput in)
        throws IOException, ClassNotFoundException {
        inconsistentKeysCnt = in.readInt();

        skippedCachesCnt = in.readInt();

        skippedEntriesCnt = in.readInt();

        if (protoVer >= V2) {
            partSizeConflictsCnt = in.readInt();

            partSizesMap = U.readMap(in);
        }
    }

    /**
     * @return Inconsistent keys count.
     */
    @Override public int inconsistentKeysCount() {
        return inconsistentKeysCnt;
    }

    /**
     * @return Skipped caches count.
     */
    @Override public int skippedCachesCount() {
        return skippedCachesCnt;
    }

    /**
     * @return Skipped entries count.
     */
    @Override public int skippedEntriesCount() {
        return skippedEntriesCnt;
    }

    /**
     * @return Partitions with broken size count.
     */
    @Override public int partSizeConflictsCnt() {
        return partSizeConflictsCnt;
    }

    /**
     * @return Result of cache size consistency reconciliation.
     */
     public Map<String, Map<Integer, Map<UUID, NodePartitionSize>>> partSizesMap() {
        return partSizesMap;
    }

    /** @inheritDoc */
    @Override public void merge(ReconciliationAffectedEntries outer) {
        assert outer instanceof ReconciliationAffectedEntriesExtended;

        inconsistentKeysCnt += outer.inconsistentKeysCount();

        skippedCachesCnt += outer.skippedEntriesCount();

        skippedEntriesCnt += outer.skippedCachesCount();
    }

    /** @inheritDoc */
    @Override public void print(Consumer<String> printer, boolean includeSensitive) {
        if (inconsistentKeysCnt != 0)
            printer.accept("\nINCONSISTENT KEYS: " + inconsistentKeysCount() + "\n\n");

        if (skippedCachesCnt != 0)
            printer.accept("\nSKIPPED CACHES: " + skippedCachesCount() + "\n\n");

        if (skippedEntriesCnt != 0)
            printer.accept("\nSKIPPED ENTRIES: " + skippedEntriesCount() + "\n\n");

        if (partSizeConflictsCnt != 0)
            printer.accept("\nPARTITIONS WITH BROKEN SIZE: " + partSizeConflictsCnt() + "\n\n");
    }
}
