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

#include <cstring>

#include <ignite/ignite_error.h>

#include "ignite/impl/interop//interop_output_stream.h"

/**
 * Common macro to write a single value.
 */
#define IGNITE_INTEROP_OUT_WRITE(val, type, len) { \
    EnsureCapacity(pos + len); \
    *reinterpret_cast<type*>(data + pos) = val; \
    Shift(len); \
}

/**
 * Common macro to write an array.
 */
#define IGNITE_INTEROP_OUT_WRITE_ARRAY(val, len) { \
    CopyAndShift(reinterpret_cast<const int8_t*>(val), 0, len); \
}

namespace ignite
{
    namespace impl
    {
        namespace interop 
        {
            union BinaryFloatInt32
            {
                float f;
                int32_t i;                
            };

            union BinaryDoubleInt64
            {
                double d;
                int64_t i;                
            };

            InteropOutputStream::InteropOutputStream(InteropMemory* mem)
            {
                this->mem = mem;

                data = mem->Data();
                cap = mem->Capacity();
                pos = 0;
            }

            void InteropOutputStream::WriteInt8(const int8_t val)
            {
                IGNITE_INTEROP_OUT_WRITE(val, int8_t, 1);
            }

            void InteropOutputStream::WriteInt8(const int8_t val, const int32_t pos)
            {
                EnsureCapacity(pos + 1);

                *(data + pos) = val;
            }

            void InteropOutputStream::WriteInt8Array(const int8_t* val, const int32_t len)
            {
                IGNITE_INTEROP_OUT_WRITE_ARRAY(val, len);
            }

            void InteropOutputStream::WriteBool(const bool val)
            {
                WriteInt8(val ? 1 : 0);
            }

            void InteropOutputStream::WriteBoolArray(const bool* val, const int32_t len)
            {
                for (int i = 0; i < len; i++)
                    WriteBool(*(val + i));
            }

            void InteropOutputStream::WriteInt16(const int16_t val)
            {
                IGNITE_INTEROP_OUT_WRITE(val, int16_t, 2);
            }

            void InteropOutputStream::WriteInt16(const int32_t pos, const int16_t val)
            {
                EnsureCapacity(pos + 2);

                *reinterpret_cast<int16_t*>(data + pos) = val;
            }

            void InteropOutputStream::WriteInt16Array(const int16_t* val, const int32_t len)
            {
                IGNITE_INTEROP_OUT_WRITE_ARRAY(val, len << 1);
            }

            void InteropOutputStream::WriteUInt16(const uint16_t val)
            {
                IGNITE_INTEROP_OUT_WRITE(val, uint16_t, 2);
            }

            void InteropOutputStream::WriteUInt16Array(const uint16_t* val, const int32_t len)
            {
                IGNITE_INTEROP_OUT_WRITE_ARRAY(val, len << 1);
            }

            void InteropOutputStream::WriteInt32(const int32_t val)
            {
                IGNITE_INTEROP_OUT_WRITE(val, int32_t, 4);
            }

            void InteropOutputStream::WriteInt32(const int32_t pos, const int32_t val)
            {
                EnsureCapacity(pos + 4);

                *reinterpret_cast<int32_t*>(data + pos) = val;
            }

            void InteropOutputStream::WriteInt32Array(const int32_t* val, const int32_t len)
            {
                IGNITE_INTEROP_OUT_WRITE_ARRAY(val, len << 2);
            }

            void InteropOutputStream::WriteInt64(const int64_t val)
            {
                IGNITE_INTEROP_OUT_WRITE(val, int64_t, 8);
            }

            void InteropOutputStream::WriteInt64(const int32_t pos, const int64_t val)
            {
                EnsureCapacity(pos + 8);

                *reinterpret_cast<int64_t*>(data + pos) = val;
            }

            void InteropOutputStream::WriteInt64Array(const int64_t* val, const int32_t len)
            {
                IGNITE_INTEROP_OUT_WRITE_ARRAY(val, len << 3);
            }

            void InteropOutputStream::WriteFloat(const float val)
            {
                BinaryFloatInt32 u;

                u.f = val;

                WriteInt32(u.i);
            }

            void InteropOutputStream::WriteFloatArray(const float* val, const int32_t len)
            {
                for (int i = 0; i < len; i++)
                    WriteFloat(*(val + i));
            }

            void InteropOutputStream::WriteDouble(const double val)
            {
                BinaryDoubleInt64 u;

                u.d = val;

                WriteInt64(u.i);
            }

            void InteropOutputStream::WriteDoubleArray(const double* val, const int32_t len)
            {
                for (int i = 0; i < len; i++)
                    WriteDouble(*(val + i));
            }

            int32_t InteropOutputStream::Position() const
            {
                return pos;
            }

            void InteropOutputStream::Position(const int32_t val)
            {
                EnsureCapacity(val);

                pos = val;
            }

            int32_t InteropOutputStream::Reserve(int32_t num)
            {
                EnsureCapacity(pos + num);

                int32_t res = pos;

                Shift(num);

                return res;
            }

            void InteropOutputStream::Synchronize()
            {
                mem->Length(pos);
            }

            InteropMemory* InteropOutputStream::GetMemory()
            {
                return mem;
            }

            void InteropOutputStream::EnsureCapacity(int32_t reqCap) {
                if (reqCap > cap) {
                    int newCap = cap << 1;

                    if (newCap < reqCap)
                        newCap = reqCap;

                    mem->Reallocate(newCap);
                    data = mem->Data();
                    cap = newCap;
                }
            }

            void InteropOutputStream::Shift(int32_t cnt) {
                pos += cnt;
            }

            void InteropOutputStream::CopyAndShift(const int8_t* src, int32_t off, int32_t len) {
                EnsureCapacity(pos + len);

                memcpy(data + pos, src + off, len);

                Shift(len);
            }
        }
    }
}

