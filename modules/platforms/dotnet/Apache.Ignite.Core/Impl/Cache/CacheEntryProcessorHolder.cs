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

namespace Apache.Ignite.Core.Impl.Cache
{
    using System;
    using System.Diagnostics;
    using System.Diagnostics.CodeAnalysis;
    using System.Reflection;
    using Apache.Ignite.Core.Binary;
    using Apache.Ignite.Core.Cache;
    using Apache.Ignite.Core.Impl.Binary;
    using Apache.Ignite.Core.Impl.Common;
    using Apache.Ignite.Core.Impl.Resource;

    /// <summary>
    /// Binary wrapper for the <see cref="ICacheEntryProcessor{TK,TV,TA,TR}"/> and it's argument.
    /// Marshals and executes wrapped processor with a non-generic interface.
    /// </summary>
    internal class CacheEntryProcessorHolder : IBinaryWriteAware
    {
        // generic processor
        private readonly object _proc;

        // argument
        private readonly object _arg;

        // func to invoke Process method on ICacheEntryProcessor in form of object.
        private readonly Func<IMutableCacheEntryInternal, object, object> _processFunc;

        // entry creator delegate
        private readonly Func<object, object, bool, IMutableCacheEntryInternal> _entryCtor;

        /// <summary>
        /// Initializes a new instance of the <see cref="CacheEntryProcessorHolder"/> class.
        /// </summary>
        /// <param name="proc">The processor to wrap.</param>
        /// <param name="arg">The argument.</param>
        /// <param name="processFunc">Delegate to call generic <see cref="ICacheEntryProcessor{K, V, A, R}.Process"/> on local node.</param>
        /// <param name="keyType">Type of the key.</param>
        /// <param name="valType">Type of the value.</param>
        public CacheEntryProcessorHolder(object proc, object arg, 
            Func<IMutableCacheEntryInternal, object, object> processFunc, Type keyType, Type valType)
        {
            Debug.Assert(proc != null);
            Debug.Assert(processFunc != null);

            _proc = proc;
            _arg = arg;
            _processFunc = processFunc;

            _processFunc = GetProcessFunc(_proc);

            _entryCtor = MutableCacheEntry.GetCtor(keyType, valType);
        }

        /// <summary>
        /// Processes specified cache entry.
        /// </summary>
        /// <param name="key">The cache entry key.</param>
        /// <param name="value">The cache entry value.</param>
        /// <param name="exists">Indicates whether cache entry exists.</param>
        /// <param name="grid"></param>
        /// <returns>
        /// Pair of resulting cache entry and result of processing it.
        /// </returns>
        [SuppressMessage("Microsoft.Design", "CA1031:DoNotCatchGeneralExceptionTypes", 
            Justification = "User processor can throw any exception")]
        public CacheEntryProcessorResultHolder Process(object key, object value, bool exists, Ignite grid)
        {
            ResourceProcessor.Inject(_proc, grid);

            var entry = _entryCtor(key, value, exists);

            try
            {
                return new CacheEntryProcessorResultHolder(entry, _processFunc(entry, _arg), null);
            }
            catch (TargetInvocationException ex)
            {
                return new CacheEntryProcessorResultHolder(null, null, ex.InnerException);
            }
            catch (Exception ex)
            {
                return new CacheEntryProcessorResultHolder(null, null, ex);
            }
        }

        /** <inheritDoc /> */
        public void WriteBinary(IBinaryWriter writer)
        {
            var writer0 = (BinaryWriter) writer.GetRawWriter();

            writer0.WriteObjectDetached(_proc);
            writer0.WriteObjectDetached(_arg);
        }

        /// <summary>
        /// Initializes a new instance of the <see cref="CacheEntryProcessorHolder"/> class.
        /// </summary>
        /// <param name="reader">The reader.</param>
        public CacheEntryProcessorHolder(IBinaryRawReader reader)
        {
            _proc = reader.ReadObject<object>();
            _arg = reader.ReadObject<object>();

            _processFunc = GetProcessFunc(_proc);

            var kvTypes = DelegateTypeDescriptor.GetCacheEntryProcessorTypes(_proc.GetType());

            _entryCtor = MutableCacheEntry.GetCtor(kvTypes.Item1, kvTypes.Item2);
        }

        /// <summary>
        /// Gets a delegate to call generic <see cref="ICacheEntryProcessor{K, V, A, R}.Process"/>.
        /// </summary>
        /// <param name="proc">The processor instance.</param>
        /// <returns>
        /// Delegate to call generic <see cref="ICacheEntryProcessor{K, V, A, R}.Process"/>.
        /// </returns>
        private static Func<IMutableCacheEntryInternal, object, object> GetProcessFunc(object proc)
        {
            var func = DelegateTypeDescriptor.GetCacheEntryProcessor(proc.GetType());
            
            return (entry, arg) => func(proc, entry, arg);
        }
    }
}