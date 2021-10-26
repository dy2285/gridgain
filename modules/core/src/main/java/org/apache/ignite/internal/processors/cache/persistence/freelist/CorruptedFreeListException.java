/*
 * Copyright 2021 GridGain Systems, Inc. and Contributors.
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

package org.apache.ignite.internal.processors.cache.persistence.freelist;

import org.apache.ignite.internal.processors.cache.persistence.AbstractCorruptedPersistenceException;
import org.jetbrains.annotations.Nullable;

/**
 * Exception to distinguish {@link AbstractFreeList} broken invariants.
 */
public class CorruptedFreeListException extends AbstractCorruptedPersistenceException {
    /** Serial version uid. */
    private static final long serialVersionUID = 0L;

    /**
     * Constructor.
     *
     * @param msg Message.
     * @param cause Cause.
     * @param grpId Cache group id.
     * @param pageIds PageId's that can be corrupted.
     */
    public CorruptedFreeListException(String msg, @Nullable Throwable cause, int grpId, long... pageIds) {
        super(msg, cause, grpId, pageIds);
    }
}
