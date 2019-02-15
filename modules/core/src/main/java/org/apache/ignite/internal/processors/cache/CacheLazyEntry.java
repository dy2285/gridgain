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

package org.apache.ignite.internal.processors.cache;

import org.apache.ignite.Ignite;
import org.apache.ignite.cache.CacheInterceptorEntry;
import org.apache.ignite.internal.util.tostring.GridToStringInclude;
import org.apache.ignite.internal.util.typedef.internal.S;

/**
 *
 */
public class CacheLazyEntry<K, V> extends CacheInterceptorEntry<K, V> {
    /** Cache context. */
    protected GridCacheContext cctx;

    /** Key cache object. */
    protected KeyCacheObject keyObj;

    /** Cache object value. */
    protected CacheObject valObj;

    /** Key. */
    @GridToStringInclude(sensitive = true)
    protected K key;

    /** Value. */
    @GridToStringInclude(sensitive = true)
    protected V val;

    /** Keep binary flag. */
    private boolean keepBinary;

    /** Update counter. */
    private Long updateCntr;

    /**
     * @param cctx Cache context.
     * @param keyObj Key cache object.
     * @param valObj Cache object value.
     * @param keepBinary Keep binary flag.
     */
    public CacheLazyEntry(GridCacheContext cctx, KeyCacheObject keyObj, CacheObject valObj, boolean keepBinary) {
        this.cctx = cctx;
        this.keyObj = keyObj;
        this.valObj = valObj;
        this.keepBinary = keepBinary;
    }

    /**
     * @param keyObj Key cache object.
     * @param val Value.
     * @param keepBinary Keep binary flag.
     * @param cctx Cache context.
     */
    public CacheLazyEntry(GridCacheContext cctx, KeyCacheObject keyObj, V val, boolean keepBinary) {
        this.cctx = cctx;
        this.keyObj = keyObj;
        this.val = val;
        this.keepBinary = keepBinary;
    }

    /**
     * @param ctx Cache context.
     * @param keyObj Key cache object.
     * @param key Key value.
     * @param valObj Cache object
     * @param keepBinary Keep binary flag.
     * @param updateCntr Partition update counter.
     * @param val Cache value.
     */
    public CacheLazyEntry(GridCacheContext ctx,
        KeyCacheObject keyObj,
        K key,
        CacheObject valObj,
        V val,
        boolean keepBinary,
        Long updateCntr
    ) {
        this.cctx = ctx;
        this.keyObj = keyObj;
        this.key = key;
        this.valObj = valObj;
        this.val = val;
        this.keepBinary = keepBinary;
        this.updateCntr = updateCntr;
    }

    /**
     * @param ctx Cache context.
     * @param keyObj Key cache object.
     * @param key Key value.
     * @param valObj Cache object
     * @param keepBinary Keep binary flag.
     * @param val Cache value.
     */
    public CacheLazyEntry(GridCacheContext ctx,
        KeyCacheObject keyObj,
        K key,
        CacheObject valObj,
        V val,
        boolean keepBinary
    ) {
        this.cctx = ctx;
        this.keyObj = keyObj;
        this.key = key;
        this.valObj = valObj;
        this.val = val;
        this.keepBinary = keepBinary;
    }

    /** {@inheritDoc} */
    @Override public K getKey() {
        if (key == null)
            key = (K)cctx.unwrapBinaryIfNeeded(keyObj, keepBinary);

        return key;
    }

    /** {@inheritDoc} */
    @Override public V getValue() {
        return getValue(keepBinary);
    }

    /**
     * Returns the value stored in the cache when this entry was created.
     *
     * @param keepBinary Flag to keep binary if needed.
     * @return the value corresponding to this entry
     */
    @SuppressWarnings("unchecked")
    public V getValue(boolean keepBinary) {
        if (val == null)
            val = (V)cctx.unwrapBinaryIfNeeded(valObj, keepBinary, true);

        return val;
    }

    /**
     * @return Return value. This methods doesn't initialize value.
     */
    public V value() {
        return val;
    }

    /**
     * @return Return key. This methods doesn't initialize key.
     */
    public K key() {
        return key;
    }

    /**
     * @return Keep binary flag.
     */
    public boolean keepBinary() {
        return keepBinary;
    }

    /** {@inheritDoc} */
    @Override public long getPartitionUpdateCounter() {
        return updateCntr == null ? 0L : updateCntr;
    }

    /**
     * Sets update counter.
     *
     * @param updateCntr Update counter.
     */
    public void updateCounter(long updateCntr) {
        this.updateCntr = updateCntr;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override public <T> T unwrap(Class<T> cls) {
        if (cls.isAssignableFrom(Ignite.class))
            return (T)cctx.kernalContext().grid();
        else if (cls.isAssignableFrom(GridCacheContext.class))
            return (T)cctx;
        else if (cls.isAssignableFrom(getClass()))
            return cls.cast(this);

        throw new IllegalArgumentException("Unwrapping to class is not supported: " + cls);
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(CacheLazyEntry.class, this);
    }
}
