﻿/*
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

namespace Apache.Ignite.Core.Impl.Binary.Structure
{
    using System;
    using System.Collections.Generic;
    using System.Diagnostics;
    using Apache.Ignite.Core.Binary;

    /// <summary>
    /// Binary type structure. Cache field IDs and metadata to improve marshalling performance.
    /// Every object write contains a set of field writes. Every unique ordered set of written fields
    /// produce write "path". We cache these paths allowing for very fast traverse over object structure
    /// without expensive map lookups and field ID calculations. 
    /// </summary>
    internal class BinaryStructure
    {
        /// <summary>
        /// Create empty type structure.
        /// </summary>
        /// <returns>Empty type structure.</returns>
        public static BinaryStructure CreateEmpty()
        {
            return new BinaryStructure(new[] { new BinaryStructureEntry[0] }, 
                new BinaryStructureJumpTable[1], new Dictionary<string, byte>());
        }

        /** Entries. */
        private readonly BinaryStructureEntry[][] _paths;

        /** Jumps. */
        private readonly BinaryStructureJumpTable[] _jumps;

        /** Field types. */
        private readonly IDictionary<string, byte> _fieldTypes;

        /// <summary>
        /// Constructor.
        /// </summary>
        /// <param name="paths">Paths.</param>
        /// <param name="jumps">Jumps.</param>
        /// <param name="fieldTypes">Field types.</param>
        private BinaryStructure(BinaryStructureEntry[][] paths,
            BinaryStructureJumpTable[] jumps, IDictionary<string, byte> fieldTypes)
        {
            _paths = paths;
            _jumps = jumps;
            _fieldTypes = fieldTypes;
        }

        /// <summary>
        /// Gets field ID if possible.
        /// </summary>
        /// <param name="fieldName">Field name.</param>
        /// <param name="fieldType">Field type.</param>
        /// <param name="pathIdx">Path index, changes during jumps.</param>
        /// <param name="actionIdx">Action index.</param>
        /// <returns>Field ID or zero in case there are no matching path.</returns>
        public int GetFieldId(string fieldName, byte fieldType, ref int pathIdx, int actionIdx)
        {
            Debug.Assert(pathIdx <= _paths.Length);

            // Get path.
            BinaryStructureEntry[] path = _paths[pathIdx];

            if (actionIdx < path.Length)
            {
                // Get entry matching the action index.
                BinaryStructureEntry entry = path[actionIdx];

                if (entry.IsExpected(fieldName, fieldType))
                    // Entry matches our expectations, return.
                    return entry.Id;

                if (entry.IsJumpTable)
                {
                    // Entry is a pointer to a jump table.
                    Debug.Assert(entry.Id < _jumps.Length);

                    BinaryStructureJumpTable jmpTbl = _jumps[entry.Id];

                    int pathIdx0 = jmpTbl.GetPathIndex(fieldName);

                    if (pathIdx0 < 0)
                        return 0;

                    Debug.Assert(pathIdx0 < _paths.Length);

                    entry = _paths[pathIdx0][actionIdx];

                    entry.ValidateType(fieldType);

                    pathIdx = pathIdx0;

                    return entry.Id;
                }
            }

            // Failed to find anything because this is a new field.
            return 0;
        }

        /// <summary>
        /// Merge updates into a new type structure.
        /// </summary>
        /// <param name="pathIdx">Path index.</param>
        /// <param name="updates">Updates.</param>
        /// <returns>New type structure with updates.</returns>
        public BinaryStructure Merge(int pathIdx, IList<BinaryStructureUpdate> updates)
        {
            if (updates == null || updates.Count == 0)
                return this;

            // Algorithm ensures that updates are applied to the same type structure,
            // where they were initially observed. This allow us to keep structure
            // internals simpler and more efficient. On the other hand, this imposes
            // some performance hit because in case of concurrent update, recorded
            // changes will be discarded and recorded again during the next write
            // on the same path. This should occur only during application warmup.

            // Note that field types are merged anyway to avoid metadata clashes.
            BinaryStructure res = MergeFieldTypes(updates);
            BinaryStructureUpdate firstUpdate = updates[0];

            if (firstUpdate.Index == 0)
            {
                // Special case: the very first structure update. Simply attach all updates.
                Debug.Assert(_paths.Length == 1);
                Debug.Assert(_paths[0].Length == 0);
                Debug.Assert(pathIdx == 0);

                var newPaths = CopyPaths(updates.Count, 0);

                ApplyUpdatesToPath(newPaths[0], updates);

                res = new BinaryStructure(newPaths, _jumps, res._fieldTypes);
            }
            else
            {
                // Get entry where updates should start.
                BinaryStructureEntry[] path = _paths[pathIdx];

                BinaryStructureEntry startEntry = default(BinaryStructureEntry);

                if (firstUpdate.Index < path.Length)
                    startEntry = path[firstUpdate.Index];

                if (startEntry.IsEmpty)
                {
                    // We are on the empty/non-existent entry. Continue the path without branching.
                    var newPaths = CopyPaths(firstUpdate.Index + updates.Count, 0);

                    ApplyUpdatesToPath(newPaths[pathIdx], updates);

                    res = new BinaryStructure(newPaths, _jumps, res._fieldTypes);
                }
                else if (startEntry.IsJumpTable)
                {
                    // We are on the jump table. Add a new path and record it in the jump table.

                    // 1. Prepare new structures.
                    var newPaths = CopyPaths(firstUpdate.Index + updates.Count, 1);
                    var newJumps = CopyJumps(0);

                    // New path will be the last one.
                    int newPathIdx = newPaths.Length - 1;

                    // Apply updates to the new path.
                    ApplyUpdatesToPath(newPaths[newPathIdx], updates);

                    // Add the jump to the table.
                    newJumps[startEntry.Id] =
                        newJumps[startEntry.Id].CopyAndAdd(firstUpdate.FieldName, newPathIdx);

                    res = new BinaryStructure(newPaths, newJumps, res._fieldTypes);
                }
                else
                {
                    // We are on existing entry. Need to create a new jump table here and two new paths.

                    // 1. Prepaare new structures.
                    var newPaths = CopyPaths(firstUpdate.Index + updates.Count, 2);
                    var newJumps = CopyJumps(1);

                    // Old path will be moved here.
                    int oldPathIdx = newPaths.Length - 2;

                    // New path will reside here.
                    int newPathIdx = newPaths.Length - 1;

                    // Create new jump table.
                    int newJumpIdx = newJumps.Length - 1;

                    newJumps[newJumpIdx] = new BinaryStructureJumpTable(startEntry.Name, oldPathIdx,
                        firstUpdate.FieldName, newPathIdx);

                    // Re-create old path in two steps: move old path to the new place, then clean the old path.
                    for (int i = firstUpdate.Index; i < path.Length; i++)
                    {
                        newPaths[oldPathIdx][i] = newPaths[pathIdx][i];

                        if (i == firstUpdate.Index)
                            // Inject jump table ...
                            newPaths[pathIdx][i] = new BinaryStructureEntry(newJumpIdx);
                        else
                            // ... or just reset.
                            newPaths[pathIdx][i] = new BinaryStructureEntry();
                    }

                    // Apply updates to the new path.
                    ApplyUpdatesToPath(newPaths[newPaths.Length - 1], updates);

                    res = new BinaryStructure(newPaths, newJumps, res._fieldTypes);
                }
            }

            return res;
        }

        /// <summary>
        /// Copy and possibly expand paths.
        /// </summary>
        /// <param name="minLen">Minimum length.</param>
        /// <param name="additionalPaths">Amount of additional paths required.</param>
        /// <returns>Result.</returns>
        private BinaryStructureEntry[][] CopyPaths(int minLen, int additionalPaths)
        {
            var newPaths = new BinaryStructureEntry[_paths.Length + additionalPaths][];

            int newPathLen = Math.Max(_paths[0].Length, minLen);

            for (int i = 0; i < newPaths.Length; i++)
            {
                newPaths[i] = new BinaryStructureEntry[newPathLen];

                if (i < _paths.Length)
                    Array.Copy(_paths[i], newPaths[i], _paths[i].Length);
            }

            return newPaths;
        }

        /// <summary>
        /// Copy and possibly expand jump tables.
        /// </summary>
        /// <param name="additionalJumps">Amount of additional jumps required.</param>
        /// <returns>Result.</returns>
        private BinaryStructureJumpTable[] CopyJumps(int additionalJumps)
        {
            var newJumps = new BinaryStructureJumpTable[_jumps.Length + additionalJumps];

            // The very first jump is always null so that we can distinguish between jump table
            // and empty value in BinaryStructureEntry.
            for (int i = 1; i < _jumps.Length; i++)
                newJumps[i] = _jumps[i].Copy();

            return newJumps;
        }

        /// <summary>
        /// Apply updates to path.
        /// </summary>
        /// <param name="path">Path.</param>
        /// <param name="updates">Updates.</param>
        private static void ApplyUpdatesToPath(IList<BinaryStructureEntry> path,
            IEnumerable<BinaryStructureUpdate> updates)
        {
            foreach (var u in updates)
                path[u.Index] = new BinaryStructureEntry(u.FieldName, u.FieldId, u.FieldType);
        }

        /// <summary>
        /// Merge field types.
        /// </summary>
        /// <param name="updates">Updates.</param>
        /// <returns>Type structure with applied updates.</returns>
        private BinaryStructure MergeFieldTypes(IList<BinaryStructureUpdate> updates)
        {
            IDictionary<string, byte> newFieldTypes = new Dictionary<string, byte>(_fieldTypes);

            foreach (BinaryStructureUpdate update in updates)
            {
                byte expType;

                if (_fieldTypes.TryGetValue(update.FieldName, out expType))
                {
                    // This is an old field.
                    if (expType != update.FieldType)
                    {
                        throw new BinaryObjectException("Field type mismatch detected [fieldName=" + update.FieldName +
                            ", expectedType=" + expType + ", actualType=" + update.FieldType + ']');
                    }
                }
                else
                    // This is a new field.
                    newFieldTypes[update.FieldName] = update.FieldType;
            }

            return newFieldTypes.Count == _fieldTypes.Count ?
                this : new BinaryStructure(_paths, _jumps, newFieldTypes);
        }

        /// <summary>
        /// Recorded field types.
        /// </summary>
        internal IDictionary<string, byte> FieldTypes
        {
            get { return _fieldTypes; }
        } 
    }
}
