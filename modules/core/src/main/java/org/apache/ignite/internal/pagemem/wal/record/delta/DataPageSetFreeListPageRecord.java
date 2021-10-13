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

package org.apache.ignite.internal.pagemem.wal.record.delta;

import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.internal.pagemem.PageMemory;
import org.apache.ignite.internal.processors.cache.persistence.tree.io.AbstractDataPageIO;
import org.apache.ignite.internal.processors.cache.persistence.tree.io.PageIO;
import org.apache.ignite.internal.util.typedef.internal.S;

import static org.apache.ignite.internal.pagemem.wal.record.WALRecord.RecordType.DATA_PAGE_SET_FREE_LIST_PAGE;

/**
 *
 */
public class DataPageSetFreeListPageRecord extends PageDeltaRecord {
    /** */
    private long freeListPage;

    /**
     * @param grpId Cache group ID.
     * @param pageId Page ID.
     * @param freeListPage Free list page ID.
     */
    public DataPageSetFreeListPageRecord(int grpId, long pageId, long freeListPage) {
        super(grpId, pageId);

        this.freeListPage = freeListPage;
    }

    /**
     * @return Free list page ID.
     */
    public long freeListPage() {
        return freeListPage;
    }

    /** {@inheritDoc} */
    @Override public void applyDelta(PageMemory pageMem, long pageAddr) throws IgniteCheckedException {
        AbstractDataPageIO<?> io = PageIO.getPageIO(pageAddr, pageMem.bigPages());

        io.setFreeListPageId(pageAddr, freeListPage);
    }

    /** {@inheritDoc} */
    @Override public RecordType type() {
        return DATA_PAGE_SET_FREE_LIST_PAGE;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(DataPageSetFreeListPageRecord.class, this, "super", super.toString());
    }
}
