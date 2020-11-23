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

package org.apache.ignite.internal.processors.cache.persistence.defragmentation;

import java.nio.ByteBuffer;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.internal.processors.cache.persistence.pagemem.PageMemoryEx;
import org.apache.ignite.internal.processors.cache.persistence.tree.BPlusTree;
import org.apache.ignite.internal.processors.cache.persistence.tree.io.BPlusIO;
import org.apache.ignite.internal.processors.cache.persistence.tree.io.BPlusLeafIO;
import org.apache.ignite.internal.processors.cache.persistence.tree.io.BPlusMetaIO;
import org.apache.ignite.internal.processors.cache.persistence.tree.io.PageIO;
import org.apache.ignite.internal.util.GridUnsafe;

/** */
public class TreeIterator {
    /** Direct memory buffer with a size of one page. */
    private final ByteBuffer pageBuf;

    /** Offheap page size. */
    private final int pageSize;

    /** */
    public TreeIterator(int size) {
        pageSize = size;

        pageBuf = ByteBuffer.allocateDirect(pageSize);
    }

    // Performance impact of constant closures allocation is not clear. So this method should be avoided in massive
    // operations like tree leaves access.
    /** */
    public static <T> T access(
        PageAccessType access,
        PageMemoryEx pageMemory,
        int grpId,
        long pageId,
        PageAccessor<T> accessor
    ) throws IgniteCheckedException {
        assert access != null;
        long page = pageMemory.acquirePage(grpId, pageId);

        try {
            long pageAddr = access == PageAccessType.ACCESS_READ
                    ? pageMemory.readLock(grpId, pageId, page)
                    : pageMemory.writeLock(grpId, pageId, page);

            try {
                return accessor.access(pageAddr);
            }
            finally {
                if (access == PageAccessType.ACCESS_READ)
                    pageMemory.readUnlock(grpId, pageId, page);
                else
                    pageMemory.writeUnlock(grpId, pageId, page, null, true);
            }
        }
        finally {
            pageMemory.releasePage(grpId, pageId, page);
        }
    }

    /** */
    @SuppressWarnings("PublicInnerClass")
    public enum PageAccessType {
        /** Read access. */
        ACCESS_READ,

        /** Write access. */
        ACCESS_WRITE;
    }

    /** */
    @SuppressWarnings("PublicInnerClass")
    @FunctionalInterface
    public interface PageAccessor<T> {
        /** */
        public T access(long pageAddr) throws IgniteCheckedException;
    }

    /** */
    // TODO Prefetch future pages?
    public <L, T extends L> void iterate(
        BPlusTree<L, T> tree,
        PageMemoryEx pageMemory,
        BPlusTree.TreeRowClosure<L, T> c
    ) throws IgniteCheckedException {
        int grpId = tree.groupId();

        long leafId = findFirstLeafId(grpId, tree.getMetaPageId(), pageMemory);

        long bufAddr = GridUnsafe.bufferAddress(pageBuf);

        while (leafId != 0L) {
            long leafPage = pageMemory.acquirePage(grpId, leafId);

            BPlusIO<L> io;

            try {
                long leafPageAddr = pageMemory.readLock(grpId, leafId, leafPage);

                try {
                    io = PageIO.getBPlusIO(leafPageAddr);

                    assert io instanceof BPlusLeafIO : io;

                    GridUnsafe.copyMemory(leafPageAddr, bufAddr, pageSize);
                }
                finally {
                    pageMemory.readUnlock(grpId, leafId, leafPage);
                }
            }
            finally {
                pageMemory.releasePage(grpId, leafId, leafPage);
            }

            int cnt = io.getCount(bufAddr);

            for (int idx = 0; idx < cnt; idx++)
                c.apply(tree, io, bufAddr, idx);

            leafId = io.getForward(bufAddr);
        }
    }

    /** */
    private long findFirstLeafId(int grpId, long metaPageId, PageMemoryEx partPageMemory) throws IgniteCheckedException {
        return access(PageAccessType.ACCESS_READ, partPageMemory, grpId, metaPageId, metaPageAddr -> {
            BPlusMetaIO metaIO = PageIO.getPageIO(metaPageAddr);

            return metaIO.getFirstPageId(metaPageAddr, 0);
        });
    }
}