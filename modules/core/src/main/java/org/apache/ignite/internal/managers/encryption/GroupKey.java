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

package org.apache.ignite.internal.managers.encryption;

import java.io.Serializable;
import java.util.Objects;

/**
 * Cache group encryption key with identifier.
 */
public class GroupKey {
    /** Encryption key ID. */
    private final int id;

    /** Encryption key. */
    private final Serializable key;

    /**
     * @param id Encryption key ID.
     * @param key Encryption key.
     */
    public GroupKey(int id, Serializable key) {
        this.id = id;
        this.key = key;
    }

    /**
     * @return Encryption key ID.
     */
    public byte id() {
        return (byte)id;
    }

    /**
     * @return Unsigned encryption key ID.
     */
    public int unsignedId() {
        return id & 0xff;
    }

    /**
     * @return Encryption key.
     */
    public Serializable key() {
        return key;
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        GroupKey grpKey = (GroupKey)o;

        return id == grpKey.id && Objects.equals(key, grpKey.key);
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        return Objects.hash(id, key);
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return "GroupKey [id=" + id + ']';
    }
}
