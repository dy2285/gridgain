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

package org.apache.ignite.internal.commandline.walconverter;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.ignite.IgniteSystemProperties;
import org.apache.ignite.internal.pagemem.wal.WALIterator;
import org.apache.ignite.internal.pagemem.wal.WALPointer;
import org.apache.ignite.internal.pagemem.wal.record.DataEntry;
import org.apache.ignite.internal.pagemem.wal.record.DataRecord;
import org.apache.ignite.internal.pagemem.wal.record.MetastoreDataRecord;
import org.apache.ignite.internal.pagemem.wal.record.TimeStampRecord;
import org.apache.ignite.internal.pagemem.wal.record.WALRecord;
import org.apache.ignite.internal.processors.cache.persistence.tree.io.PageIO;
import org.apache.ignite.internal.processors.cache.persistence.wal.FileDescriptor;
import org.apache.ignite.internal.processors.cache.persistence.wal.reader.IgniteWalIteratorFactory;
import org.apache.ignite.internal.processors.cache.persistence.wal.serializer.RecordV1Serializer;
import org.apache.ignite.internal.processors.query.h2.database.io.H2ExtrasInnerIO;
import org.apache.ignite.internal.processors.query.h2.database.io.H2ExtrasLeafIO;
import org.apache.ignite.internal.processors.query.h2.database.io.H2InnerIO;
import org.apache.ignite.internal.processors.query.h2.database.io.H2LeafIO;
import org.apache.ignite.internal.processors.query.h2.database.io.H2MvccInnerIO;
import org.apache.ignite.internal.processors.query.h2.database.io.H2MvccLeafIO;
import org.apache.ignite.internal.util.IgniteUtils;
import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.lang.IgniteBiTuple;

import static org.apache.ignite.internal.commandline.walconverter.IgniteWalConverterArguments.Args.BINARY_METADATA_DIR;
import static org.apache.ignite.internal.commandline.walconverter.IgniteWalConverterArguments.Args.INCLUDE_SENSITIVE;
import static org.apache.ignite.internal.commandline.walconverter.IgniteWalConverterArguments.Args.MARSHALLER_MAPPING_DIR;
import static org.apache.ignite.internal.commandline.walconverter.IgniteWalConverterArguments.Args.SKIP_CRC;
import static org.apache.ignite.internal.commandline.walconverter.IgniteWalConverterArguments.Args.WAL_DIR;

/**
 * Print WAL log data in human-readable form.
 */
public class IgniteWalConverter {
    /**
     * @param args Args.
     * @throws Exception If failed.
     */
    public static void main0(String[] args) {
        final IgniteWalConverterArguments parameters = IgniteWalConverterArguments.parse(System.out, args);

        if (parameters != null)
            convert(System.out, parameters);
    }

    public static void main(String[] args) {
        args = new String[] {
//            WAL_ARCHIVE_DIR.arg(), "C:\\Users\\tkalk\\Downloads\\mts_work\\work20\\work\\db\\wal\\archive\\mdp_node_3",
            WAL_DIR.arg(), "C:\\Users\\tkalk\\Downloads\\mts_work\\work20\\work\\db\\wal\\mdp_node_3",
            BINARY_METADATA_DIR.arg(), "C:\\Users\\tkalk\\Downloads\\mts_work\\work20\\work\\db\\binary_meta\\mdp_node_3",
            BINARY_METADATA_DIR.arg(), "C:\\Users\\tkalk\\Downloads\\mts_work\\work20\\work\\db\\binary_meta\\mdp_node_3",
            MARSHALLER_MAPPING_DIR.arg(), "C:\\Users\\tkalk\\Downloads\\mts_work\\work20\\work\\db\\marshaller",
            INCLUDE_SENSITIVE.arg(), ProcessSensitiveData.SHOW.toString(),
            SKIP_CRC.arg()
        };

        final IgniteWalConverterArguments parameters = IgniteWalConverterArguments.parse(System.out, args);

        if (parameters != null)
            convert(System.out, parameters);
    }

    /**
     * Write to out WAL log data in human-readable form.
     *
     * @param out        Receiver of result.
     * @param params Parameters.
     */
    public static void convert(final PrintStream out, final IgniteWalConverterArguments params) {
        PageIO.registerH2(H2InnerIO.VERSIONS, H2LeafIO.VERSIONS, H2MvccInnerIO.VERSIONS, H2MvccLeafIO.VERSIONS);
        H2ExtrasInnerIO.register();
        H2ExtrasLeafIO.register();

        System.setProperty(IgniteSystemProperties.IGNITE_TO_STRING_INCLUDE_SENSITIVE,
            Boolean.toString(params.includeSensitive() == ProcessSensitiveData.SHOW));

        System.setProperty(IgniteSystemProperties.IGNITE_PDS_SKIP_CRC, Boolean.toString(params.isSkipCrc()));
        RecordV1Serializer.skipCrc = params.isSkipCrc();

        System.setProperty(IgniteSystemProperties.IGNITE_TO_STRING_MAX_LENGTH, String.valueOf(Integer.MAX_VALUE));

        final WalStat stat = params.isPrintStat() ? new WalStat() : null;

        IgniteWalIteratorFactory.IteratorParametersBuilder iteratorParametersBuilder = new IgniteWalIteratorFactory.IteratorParametersBuilder()
            .pageSize(params.getPageSize())
            .binaryMetadataFileStoreDir(params.getBinaryMetadataDir())
            .marshallerMappingFileStoreDir(params.getMarshallerMappingDir())
            .keepBinary(!params.isUnwrapBinary());

        if (params.getWalDir() != null)
            iteratorParametersBuilder.filesOrDirs(params.getWalDir());

        if (params.getWalArchiveDir() != null)
            iteratorParametersBuilder.filesOrDirs(params.getWalArchiveDir());

        final IgniteWalIteratorFactory factory = new IgniteWalIteratorFactory();

        boolean printAlways = F.isEmpty(params.getRecordTypes());

        try (WALIterator stIt = factory.iterator(iteratorParametersBuilder)) {
            String currentWalPath = null;

            while (stIt.hasNextX()) {
                final String currentRecordWalPath = getCurrentWalFilePath(stIt);

                if (currentWalPath == null || !currentWalPath.equals(currentRecordWalPath)) {
                    out.println("File: " + currentRecordWalPath);

                    currentWalPath = currentRecordWalPath;
                }

                IgniteBiTuple<WALPointer, WALRecord> next = stIt.nextX();

                final WALPointer pointer = next.get1();

                final WALRecord record = next.get2();

                if (stat != null)
                    stat.registerRecord(record, pointer, true);

                if (printAlways || params.getRecordTypes().contains(record.type())) {
                    boolean print = true;

                    if (record instanceof TimeStampRecord)
                        print = withinTimeRange((TimeStampRecord) record, params.getFromTime(), params.getToTime());

                    final String recordStr = toString(record, params.includeSensitive());

                    if (print && (F.isEmpty(params.hasText()) || recordStr.contains(params.hasText())))
                        out.println(recordStr);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace(out);
        }

        if (stat != null)
            out.println("Statistic collected:\n" + stat.toString());
    }

    /**
     * Checks if provided TimeStampRecord is within time range.
     *
     * @param rec Record.
     * @param fromTime Lower bound for timestamp.
     * @param toTime Upper bound for timestamp;
     * @return {@code True} if timestamp is within range.
     */
    private static boolean withinTimeRange(TimeStampRecord rec, Long fromTime, Long toTime) {
        if (fromTime != null && rec.timestamp() < fromTime)
            return false;

        if (toTime != null && rec.timestamp() > toTime)
            return false;

        return true;
    }

    /**
     * Get current wal file path, used in {@code WALIterator}
     *
     * @param it WALIterator.
     * @return Current wal file path.
     */
    private static String getCurrentWalFilePath(WALIterator it) {
        String result = null;

        try {
            final Integer curIdx = IgniteUtils.field(it, "curIdx");

            final List<FileDescriptor> walFileDescriptors = IgniteUtils.field(it, "walFileDescriptors");

            if (curIdx != null && walFileDescriptors != null && !walFileDescriptors.isEmpty())
                result = walFileDescriptors.get(curIdx).getAbsolutePath();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Converting {@link WALRecord} to a string with sensitive data.
     *
     * @param walRecord     Instance of {@link WALRecord}.
     * @param sensitiveData Strategy for processing of sensitive data.
     * @return String representation of {@link WALRecord}.
     */
    private static String toString(WALRecord walRecord, ProcessSensitiveData sensitiveData) {
        if (walRecord instanceof DataRecord) {
            final DataRecord dataRecord = (DataRecord)walRecord;

            final List<DataEntry> entryWrappers = new ArrayList<>(dataRecord.writeEntries().size());

            for (DataEntry dataEntry : dataRecord.writeEntries())
                entryWrappers.add(new DataEntryWrapper(dataEntry, sensitiveData));

            dataRecord.setWriteEntries(entryWrappers);
        }
        else if (walRecord instanceof MetastoreDataRecord)
            walRecord = new MetastoreDataRecordWrapper((MetastoreDataRecord)walRecord, sensitiveData);

        return walRecord.toString();
    }
}
