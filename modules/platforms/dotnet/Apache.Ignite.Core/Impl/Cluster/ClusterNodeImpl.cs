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

namespace Apache.Ignite.Core.Impl.Cluster
{
    using System;
    using System.Collections.Generic;
    using System.Diagnostics;
    using Apache.Ignite.Core.Binary;
    using Apache.Ignite.Core.Cluster;
    using Apache.Ignite.Core.Common;
    using Apache.Ignite.Core.Impl.Binary;
    using Apache.Ignite.Core.Impl.Collections;
    using Apache.Ignite.Core.Impl.Common;

    /// <summary>
    /// Cluster node implementation.
    /// </summary>
    internal class ClusterNodeImpl : IClusterNode
    {
        /** Detached mode */
        private readonly bool _isDetached;

        /** Node ID. */
        private readonly Guid _id;

        /** Attributes. */
        private readonly IDictionary<string, object> _attrs;

        /** Addresses. */
        private readonly ICollection<string> _addrs;

        /** Hosts. */
        private readonly ICollection<string> _hosts;

        /** Order. */
        private readonly long _order;

        /** Local flag. */
        private readonly bool _isLocal;

        /** Daemon flag. */
        private readonly bool _isDaemon;

        /** Client flag. */
        private readonly bool _isClient;

        /** Consistent id. */
        private readonly object _consistentId;

        /** Ignite version. */
        private readonly IgniteProductVersion _version;

        /** Metrics. */
        private volatile ClusterMetricsImpl _metrics;
        
        /** Ignite reference. */
        private WeakReference _igniteRef;

        /// <summary>
        /// Initializes a new instance of the <see cref="ClusterNodeImpl"/> class.
        /// </summary>
        /// <param name="reader">The reader.</param>
        public ClusterNodeImpl(IBinaryRawReader reader)
        {
            //C++ breaker.
            _isDetached = reader.ReadBoolean();

            //If there would be any errors, the cluster will hang.
            //Rolling upgrade breaker.
            if (_isDetached)
            {
                var id = reader.ReadGuid();

                Debug.Assert(id.HasValue);

                _id = id.Value;

                _consistentId = reader.ReadObject<object>();
                _attrs = ReadAttributes(reader);
            }
            if (!_isDetached)
            {
                var id = reader.ReadGuid();

                Debug.Assert(id.HasValue);

                _id = id.Value;

                _attrs = ReadAttributes(reader);

                _addrs = reader.ReadCollectionAsList<string>().AsReadOnly();
                _hosts = reader.ReadCollectionAsList<string>().AsReadOnly();
                _order = reader.ReadLong();
                _isLocal = reader.ReadBoolean();
                _isDaemon = reader.ReadBoolean();
                _isClient = reader.ReadBoolean();
                _consistentId = reader.ReadObject<object>();
                _version = new IgniteProductVersion(reader);

                _metrics = reader.ReadBoolean() ? new ClusterMetricsImpl(reader) : null;
            }
        }

        /// <summary>
        /// Checks that current operation is available and if it's not
        /// throws the <exception cref="DetachedClusterNodeException"> exception</exception>.
        /// </summary>
        /// <typeparam name="T">Type of value.</typeparam>
        /// <param name="val">Value.</param>
        /// <returns>Value.</returns>
        private T ValidateDetachedNode<T>(T val)
        {
            if (_isDetached)
            {
                throw new DetachedClusterNodeException("Operation is not supported on DetachedClusterNode");
            }

            return val;
        }

        /** <inheritDoc /> */
        public Guid Id
        {
            get { return _id; }
        }

        /** <inheritDoc /> */
        public T GetAttribute<T>(string name)
        {
            IgniteArgumentCheck.NotNull(name, "name");

            return (T)_attrs[name];
        }

        /** <inheritDoc /> */
        public bool TryGetAttribute<T>(string name, out T attr)
        {
            IgniteArgumentCheck.NotNull(name, "name");

            object val;

            if (_attrs.TryGetValue(name, out val))
            {
                attr = (T)val;

                return true;
            }
            attr = default(T);

            return false;
        }

        /** <inheritDoc /> */
        public IDictionary<string, object> GetAttributes()
        {
            return _attrs;
        }

        /** <inheritDoc /> */
        public ICollection<string> Addresses
        {
            get { return ValidateDetachedNode(_addrs); }
        }

        /** <inheritDoc /> */
        public ICollection<string> HostNames
        {
            get { return ValidateDetachedNode(_hosts); }
        }

        /** <inheritDoc /> */
        public long Order
        {
            get { return ValidateDetachedNode(_order); }
        }

        /** <inheritDoc /> */
        public bool IsLocal
        {
            get { return _isLocal; }
        }

        /** <inheritDoc /> */
        public bool IsDaemon
        {
            get { return _isDaemon; }
        }

        /** <inheritDoc /> */
        public IgniteProductVersion Version
        {
            get { return ValidateDetachedNode(_version); }
        }

        public IClusterMetrics GetMetrics()
        {
            ValidateDetachedNode<object>(null);

            var ignite = (Ignite)_igniteRef.Target;

            if (ignite == null)
                return _metrics;

            ClusterMetricsImpl oldMetrics = _metrics;

            long lastUpdateTime = oldMetrics.LastUpdateTimeRaw;

            ClusterMetricsImpl newMetrics = ignite.ClusterGroup.RefreshClusterNodeMetrics(_id, lastUpdateTime);

            if (newMetrics != null)
            {
                lock (this)
                {
                    if (_metrics.LastUpdateTime < newMetrics.LastUpdateTime)
                        _metrics = newMetrics;
                }

                return newMetrics;
            }

            return oldMetrics;
        }

        /** <inheritDoc /> */
        public object ConsistentId
        {
            get { return _consistentId; }
        }

        /** <inheritDoc /> */
        public IDictionary<string, object> Attributes
        {
            get { return _attrs; }
        }

        /** <inheritDoc /> */
        public bool IsClient
        {
            get { return _isClient; }
        }

        /** <inheritDoc /> */
        public override string ToString()
        {
            return "GridNode [id=" + Id + ']';
        }

        /** <inheritDoc /> */
        public override bool Equals(object obj)
        {
            ClusterNodeImpl node = obj as ClusterNodeImpl;

            if (node != null)
                return _id.Equals(node._id);

            return false;
        }

        /** <inheritDoc /> */
        public override int GetHashCode()
        {
            // ReSharper disable once NonReadonlyMemberInGetHashCode
            return _id.GetHashCode();
        }

        /// <summary>
        /// Initializes this instance with a grid.
        /// </summary>
        /// <param name="grid">The grid.</param>
        internal void Init(Ignite grid)
        {
            _igniteRef = new WeakReference(grid);
        }

        /// <summary>
        /// Reads the attributes.
        /// </summary>
        internal static IDictionary<string, object> ReadAttributes(IBinaryRawReader reader)
        {
            Debug.Assert(reader != null);

            var count = reader.ReadInt();
            var res = new Dictionary<string, object>(count);

            for (var i = 0; i < count; i++)
            {
                res[reader.ReadString()] = reader.ReadObject<object>();
            }

            return res.AsReadOnly();
        }
    }
}
