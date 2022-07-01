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

package org.apache.ignite.client;

import java.util.Collection;
import java.util.List;
import org.apache.ignite.IgniteBinary;
import org.apache.ignite.cache.query.FieldsQueryCursor;
import org.apache.ignite.cache.query.SqlFieldsQuery;

/**
 * Ignite thin client.
 * <p>
 * Unlike Ignite client nodes, thin clients do not start Ignite infrastructure and communicate with Ignite cluster
 * over a fast and lightweight protocol.
 * </p>
 */
public interface IgniteClient extends AutoCloseable {
    /**
     * Gets the existing cache or creates a new cache with default configuration if it does not exist.
     *
     * @param name Cache name.
     */
    public <K, V> ClientCache<K, V> getOrCreateCache(String name) throws ClientException;

    /**
     * Gets the existing cache or creates a new cache with default configuration if it does not exist.
     *
     * @param name Cache name.
     * @return a Future representing pending completion of the operation, which wraps the resulting cache.
     */
    public <K, V> IgniteClientFuture<ClientCache<K, V>> getOrCreateCacheAsync(String name) throws ClientException;

    /**
     * Gets the existing cache or creates a new cache if it does not exist.
     *
     * @param cfg Cache configuration. If the cache exists, this configuration is ignored.
     */
    public <K, V> ClientCache<K, V> getOrCreateCache(ClientCacheConfiguration cfg) throws ClientException;

    /**
     * Gets the existing cache or creates a new cache if it does not exist.
     *
     * @param cfg Cache configuration. If the cache exists, this configuration is ignored.
     * @return a Future representing pending completion of the operation, which wraps the resulting cache.
     */
    public <K, V> IgniteClientFuture<ClientCache<K, V>> getOrCreateCacheAsync(ClientCacheConfiguration cfg)
            throws ClientException;

    /**
     * Get existing cache.
     *
     * @param name Cache name.
     */
    public <K, V> ClientCache<K, V> cache(String name);

    /**
     * Gets the names of all available caches.
     * @return Collection of names of currently available caches or an empty collection if no caches are available.
     */
    public Collection<String> cacheNames() throws ClientException;

    /**
     * Gets the names of all available caches.
     * @return a Future representing pending completion of the operation, which wraps the сollection of names
     * of currently available caches or an empty collection if no caches are available.
     */
    public IgniteClientFuture<Collection<String>> cacheNamesAsync() throws ClientException;

    /**
     * Destroys the cache with the given name.
     * Throws {@link ClientException} if the cache does not exist.
     */
    public void destroyCache(String name) throws ClientException;

    /**
     * Destroys the cache with the given name.
     * Throws {@link ClientException} if the cache does not exist.
     * @return a Future representing pending completion of the operation.
     */
    public IgniteClientFuture<Void> destroyCacheAsync(String name) throws ClientException;

    /**
     * Creates a cache with a default configuration.
     *
     * @param name Cache name.
     * @return Resulting cache.
     */
    public <K, V> ClientCache<K, V> createCache(String name) throws ClientException;

    /**
     * Creates a cache with a default configuration.
     *
     * @param name Cache name.
     * @return a Future representing pending completion of the operation, which wraps the resulting cache.
     */
    public <K, V> IgniteClientFuture<ClientCache<K, V>> createCacheAsync(String name) throws ClientException;

    /**
     * Creates a cache with the specified configuration.
     *
     * @param cfg Cache configuration.
     * @return Resulting cache.
     */
    public <K, V> ClientCache<K, V> createCache(ClientCacheConfiguration cfg) throws ClientException;

    /**
     * Creates a cache with the specified configuration.
     *
     * @param cfg Cache configuration.
     * @return a Future representing pending completion of the operation, which wraps the resulting cache.
     */
    public <K, V> IgniteClientFuture<ClientCache<K, V>> createCacheAsync(ClientCacheConfiguration cfg)
            throws ClientException;

    /**
     * @return Instance of {@link IgniteBinary} interface.
     */
    public IgniteBinary binary();

    /**
     * Execute SQL query and get cursor to iterate over results.
     *
     * @param qry SQL query.
     * @return Cursor.
     */
    public FieldsQueryCursor<List<?>> query(SqlFieldsQuery qry);

    /**
     * Gets client transactions facade.
     *
     * @return Client transactions facade.
     */
    public ClientTransactions transactions();

    /**
     * Gets compute facade over all cluster nodes started in server mode.
     *
     * @return Compute instance over all cluster nodes started in server mode.
     */
    public ClientCompute compute();

    /**
     * Gets compute facade over the specified cluster group. All operations
     * on the returned {@link ClientCompute} instance will only include nodes from
     * this cluster group.
     *
     * @param grp Cluster group.
     * @return Compute instance over given cluster group.
     */
    public ClientCompute compute(ClientClusterGroup grp);

    /**
     * Gets client cluster facade.
     *
     * @return Client cluster facade.
     */
    public ClientCluster cluster();

    /**
     * Gets {@code services} facade over all cluster nodes started in server mode.
     *
     * @return Services facade over all cluster nodes started in server mode.
     */
    public ClientServices services();

    /**
     * Gets {@code services} facade over nodes within the cluster group. All operations
     * on the returned {@link ClientServices} instance will only include nodes from
     * the specified cluster group.
     *
     * Note: In some cases there will be additional requests for each service invocation from client to server
     * to resolve cluster group.
     *
     * @param grp Cluster group.
     * @return {@code Services} functionality over given cluster group.
     */
    public ClientServices services(ClientClusterGroup grp);

    /**
     * Gets an atomic long from cache and creates one if it has not been created yet and {@code create} flag
     * is {@code true}.
     *
     * @param name Name of atomic long.
     * @param initVal Initial value for atomic long. Ignored if {@code create} flag is {@code false}.
     * @param create Boolean flag indicating whether data structure should be created if it does not exist.
     * @return Atomic long.
     */
    public ClientAtomicLong atomicLong(String name, long initVal, boolean create);

    /**
     * Gets an atomic long from cache and creates one if it has not been created yet and {@code create} flag
     * is {@code true}.
     *
     * @param name Name of atomic long.
     * @param cfg Configuration.
     * @param initVal Initial value for atomic long. Ignored if {@code create} flag is {@code false}.
     * @param create Boolean flag indicating whether data structure should be created if it does not exist.
     * @return Atomic long.
     */
    public ClientAtomicLong atomicLong(String name, ClientAtomicConfiguration cfg, long initVal, boolean create);

    /**
     * Closes this client's open connections and relinquishes all underlying resources.
     */
    @Override public void close();
}
