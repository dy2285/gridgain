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

package org.apache.ignite.internal.processors.metastorage;

import java.io.Serializable;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.internal.GridKernalContext;
import org.apache.ignite.internal.managers.discovery.IgniteDiscoverySpi;
import org.apache.ignite.internal.util.future.GridFutureAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.apache.ignite.internal.IgniteFeatures.METASTORAGE_LONG_KEYS;
import static org.apache.ignite.internal.IgniteFeatures.allNodesSupport;

/**
 * API for distributed data storage with the ability to write into it.
 *
 * @see ReadableDistributedMetaStorage
 */
public interface DistributedMetaStorage extends ReadableDistributedMetaStorage {
    /**
     * Prefix for keys in metastorage used by Ignite internally. No user keys should start with this prefix.
     */
    public static final String IGNITE_INTERNAL_KEY_PREFIX = "ignite.internal.";

    /**
     * Check that long keys are supported by all nodes in the cluster.
     *
     * @param ctx Kernal context.
     * @return {@code True} if all nodes in the cluster support long keys.
     */
    public static boolean longKeysSupported(GridKernalContext ctx) {
        return allNodesSupport(ctx, METASTORAGE_LONG_KEYS, IgniteDiscoverySpi.SRV_NODES);
    }

    /**
     * Write value into distributed metastorage.
     *
     * @param key The key.
     * @param val Value to write. Must not be null.
     * @throws IgniteCheckedException In case of marshalling error or some other unexpected exception.
     */
    void write(@NotNull String key, @NotNull Serializable val) throws IgniteCheckedException;

    /**
     * Write value into distributed metastorage asynchronously.
     *
     * @param key The key.
     * @param val Value to write. Must not be null.
     * @return Future with the operation result.
     * @throws IgniteCheckedException In case of marshalling error or some other unexpected exception.
     */
    GridFutureAdapter<?> writeAsync(@NotNull String key, @NotNull Serializable val) throws IgniteCheckedException;

    /**
     * Remove value from distributed metastorage asynchronously.
     *
     * @param key The key.
     * @return Future with the operation result.
     * @throws IgniteCheckedException In case of marshalling error or some other unexpected exception.
     */
    GridFutureAdapter<?> removeAsync(@NotNull String key) throws IgniteCheckedException;

    /**
     * Remove value from distributed metastorage.
     *
     * @param key The key.
     * @throws IgniteCheckedException In case of marshalling error or some other unexpected exception.
     */
    void remove(@NotNull String key) throws IgniteCheckedException;

    /**
     * Write value into distributed metastorage but only if current value matches the expected one.
     *
     * @param key The key.
     * @param expVal Expected value. Might be null.
     * @param newVal Value to write. Must not be null.
     * @throws IgniteCheckedException In case of marshalling error or some other unexpected exception.
     * @return {@code True} if expected value matched the actual one and write was completed successfully.
     *      {@code False} otherwise.
     */
    boolean compareAndSet(
        @NotNull String key,
        @Nullable Serializable expVal,
        @NotNull Serializable newVal
    ) throws IgniteCheckedException;

    /**
     * Write value into distributed metastorage asynchronously but only if current value matches the expected one.
     *
     * @param key The key.
     * @param expVal Expected value. Might be null.
     * @param newVal Value to write. Must not be null.
     * @throws IgniteCheckedException In case of marshalling error or some other unexpected exception.
     * @return {@code True} if expected value matched the actual one and write was completed successfully.
     *      {@code False} otherwise.
     */
    GridFutureAdapter<Boolean> compareAndSetAsync(
        @NotNull String key,
        @Nullable Serializable expVal,
        @NotNull Serializable newVal
    ) throws IgniteCheckedException;

    /**
     * Remove value from distributed metastorage but only if current value matches the expected one.
     *
     * @param key The key.
     * @param expVal Expected value. Must not be null.
     * @throws IgniteCheckedException In case of marshalling error or some other unexpected exception.
     * @return {@code True} if expected value matched the actual one and remove was completed successfully.
     *      {@code False} otherwise.
     */
    boolean compareAndRemove(@NotNull String key, @NotNull Serializable expVal) throws IgniteCheckedException;
}
