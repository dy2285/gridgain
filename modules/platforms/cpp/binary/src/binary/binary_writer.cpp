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

#include "ignite/impl/binary/binary_writer_impl.h"
#include "ignite/binary/binary_writer.h"

using namespace ignite::impl::binary;

namespace ignite
{
    namespace binary
    {
        BinaryWriter::BinaryWriter(BinaryWriterImpl* impl) : impl(impl)
        {
            // No-op.
        }

        void BinaryWriter::WriteInt8(const char* fieldName, int8_t val)
        {
            impl->WriteInt8(fieldName, val);
        }

        void BinaryWriter::WriteInt8Array(const char* fieldName, const int8_t* val, int32_t len)
        {
            impl->WriteInt8Array(fieldName, val, len);
        }

        void BinaryWriter::WriteBool(const char* fieldName, bool val)
        {
            impl->WriteBool(fieldName, val);
        }

        void BinaryWriter::WriteBoolArray(const char* fieldName, const bool* val, int32_t len)
        {
            impl->WriteBoolArray(fieldName, val, len);
        }

        void BinaryWriter::WriteInt16(const char* fieldName, int16_t val)
        {
            impl->WriteInt16(fieldName, val);
        }

        void BinaryWriter::WriteInt16Array(const char* fieldName, const int16_t* val, int32_t len)
        {
            impl->WriteInt16Array(fieldName, val, len);
        }

        void BinaryWriter::WriteUInt16(const char* fieldName, uint16_t val)
        {
            impl->WriteUInt16(fieldName, val);
        }

        void BinaryWriter::WriteUInt16Array(const char* fieldName, const uint16_t* val, int32_t len)
        {
            impl->WriteUInt16Array(fieldName, val, len);
        }

        void BinaryWriter::WriteInt32(const char* fieldName, int32_t val)
        {
            impl->WriteInt32(fieldName, val);
        }

        void BinaryWriter::WriteInt32Array(const char* fieldName, const int32_t* val, int32_t len)
        {
            impl->WriteInt32Array(fieldName, val, len);
        }

        void BinaryWriter::WriteInt64(const char* fieldName, const int64_t val)
        {
            impl->WriteInt64(fieldName, val);
        }

        void BinaryWriter::WriteInt64Array(const char* fieldName, const int64_t* val, int32_t len)
        {
            impl->WriteInt64Array(fieldName, val, len);
        }

        void BinaryWriter::WriteFloat(const char* fieldName, float val)
        {
            impl->WriteFloat(fieldName, val);
        }

        void BinaryWriter::WriteFloatArray(const char* fieldName, const float* val, int32_t len)
        {
            impl->WriteFloatArray(fieldName, val, len);
        }

        void BinaryWriter::WriteDouble(const char* fieldName, double val)
        {
            impl->WriteDouble(fieldName, val);
        }

        void BinaryWriter::WriteDoubleArray(const char* fieldName, const double* val, int32_t len)
        {
            impl->WriteDoubleArray(fieldName, val, len);
        }

        void BinaryWriter::WriteGuid(const char* fieldName, const Guid& val)
        {
            impl->WriteGuid(fieldName, val);
        }

        void BinaryWriter::WriteGuidArray(const char* fieldName, const Guid* val, const int32_t len)
        {
            impl->WriteGuidArray(fieldName, val, len);
        }

        void BinaryWriter::WriteDate(const char * fieldName, const Date & val)
        {
            impl->WriteDate(fieldName, val);
        }

        void BinaryWriter::WriteDateArray(const char * fieldName, const Date * val, const int32_t len)
        {
            impl->WriteDateArray(fieldName, val, len);
        }

        void BinaryWriter::WriteTimestamp(const char * fieldName, const Timestamp & val)
        {
            impl->WriteTimestamp(fieldName, val);
        }

        void BinaryWriter::WriteTimestampArray(const char * fieldName, const Timestamp * val, const int32_t len)
        {
            impl->WriteTimestampArray(fieldName, val, len);
        }

        void BinaryWriter::WriteTime(const char* fieldName, const Time& val)
        {
            impl->WriteTime(fieldName, val);
        }

        void BinaryWriter::WriteTimeArray(const char* fieldName, const Time* val, const int32_t len)
        {
            impl->WriteTimeArray(fieldName, val, len);
        }

        void BinaryWriter::WriteString(const char* fieldName, const char* val)
        {
            if (val)
                WriteString(fieldName, val, static_cast<int32_t>(strlen(val)));
            else
                WriteNull(fieldName);
        }

        void BinaryWriter::WriteString(const char* fieldName, const char* val, int32_t len)
        {
            impl->WriteString(fieldName, val, len);
        }

        BinaryStringArrayWriter BinaryWriter::WriteStringArray(const char* fieldName)
        {
            int32_t id = impl->WriteStringArray(fieldName);

            return BinaryStringArrayWriter(impl, id);
        }

        void BinaryWriter::WriteNull(const char* fieldName)
        {
            impl->WriteNull(fieldName);
        }

        BinaryRawWriter BinaryWriter::RawWriter()
        {
            impl->SetRawMode();

            return BinaryRawWriter(impl);
        }
    }
}