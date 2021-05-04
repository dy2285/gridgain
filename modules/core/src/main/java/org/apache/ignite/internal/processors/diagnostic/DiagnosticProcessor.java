/*
 * Copyright 2021 GridGain Systems, Inc. and Contributors.
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

package org.apache.ignite.internal.processors.diagnostic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteSystemProperties;
import org.apache.ignite.failure.FailureContext;
import org.apache.ignite.internal.GridKernalContext;
import org.apache.ignite.internal.pagemem.wal.IgniteWriteAheadLogManager;
import org.apache.ignite.internal.processors.GridProcessorAdapter;
import org.apache.ignite.internal.processors.cache.persistence.AbstractCorruptedPersistenceException;
import org.apache.ignite.internal.processors.cache.persistence.CorruptedPersistenceException;
import org.apache.ignite.internal.processors.cache.persistence.file.FileIO;
import org.apache.ignite.internal.processors.cache.persistence.file.FileIOFactory;
import org.apache.ignite.internal.processors.cache.persistence.wal.FileWriteAheadLogManager;
import org.apache.ignite.internal.processors.cache.persistence.wal.SegmentRouter;
import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.internal.util.typedef.T2;
import org.apache.ignite.internal.util.typedef.X;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.jetbrains.annotations.Nullable;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
import static org.apache.ignite.configuration.DataStorageConfiguration.DFLT_PAGE_SIZE;
import static org.apache.ignite.internal.processors.cache.GridCacheUtils.isPersistenceEnabled;

/**
 * Processor which contained helper methods for different diagnostic cases.
 */
public class DiagnosticProcessor extends GridProcessorAdapter {
    /** Value of the system property that enables page locks dumping on failure. */
    private static final boolean IGNITE_DUMP_PAGE_LOCK_ON_FAILURE =
        IgniteSystemProperties.getBoolean(IgniteSystemProperties.IGNITE_DUMP_PAGE_LOCK_ON_FAILURE, true);

    /** Time formatter for dump file name. */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'_'HH-mm-ss_SSS");

    /** Folder name for store diagnostic info. **/
    public static final String DEFAULT_TARGET_FOLDER = "diagnostic";

    /** Full path for store dubug info. */
    private final Path diagnosticPath;

    /** Reconciliation execution context. */
    private final ReconciliationExecutionContext reconciliationExecutionContext;

    /** File I/O factory. */
    @Nullable private final FileIOFactory fileIOFactory;

    /**
     * Constructor.
     *
     * @param ctx Kernal context.
     */
    public DiagnosticProcessor(GridKernalContext ctx) throws IgniteCheckedException {
        super(ctx);

        diagnosticPath = U.resolveWorkDirectory(ctx.config().getWorkDirectory(), DEFAULT_TARGET_FOLDER, false)
            .toPath();

        reconciliationExecutionContext = new ReconciliationExecutionContext(ctx);

        fileIOFactory = isPersistenceEnabled(ctx.config()) ?
            ctx.config().getDataStorageConfiguration().getFileIOFactory() : null;
    }

    /**
     * Print diagnostic info about failure occurred on {@code ignite} instance.
     * Failure details is contained in {@code failureCtx}.
     *
     * @param failureCtx Failure context.
     */
    public void onFailure(FailureContext failureCtx) {
        // Dump data structures page locks.
        if (IGNITE_DUMP_PAGE_LOCK_ON_FAILURE)
            ctx.cache().context().diagnostic().pageLockTracker().dumpLocksToLog();

        CorruptedPersistenceException corruptedPersistenceE =
            X.cause(failureCtx.error(), AbstractCorruptedPersistenceException.class);

        if (corruptedPersistenceE != null && !F.isEmpty(corruptedPersistenceE.pages()) && fileIOFactory != null) {
            File[] walDirs = walDirs(ctx);

            if (F.isEmpty(walDirs)) {
                if (log.isInfoEnabled())
                    log.info("Skipping dump diagnostic info due to WAL not configured");
            }
            else {
                try {
                    File corruptedPagesFile = corruptedPagesFile(
                        diagnosticPath,
                        fileIOFactory,
                        corruptedPersistenceE.pages()
                    );

                    String walDirsStr = Arrays.stream(walDirs).map(File::getAbsolutePath)
                        .collect(joining(", ", "[", "]"));

                    String args = "--wal-dir " + walDirs[0].getAbsolutePath() + (walDirs.length == 1 ? "" :
                        " --wal-archive-dir " + walDirs[1].getAbsolutePath());

                    if (ctx.config().getDataStorageConfiguration().getPageSize() != DFLT_PAGE_SIZE)
                        args += " --page-size " + ctx.config().getDataStorageConfiguration().getPageSize();

                    args += " --pages " + corruptedPagesFile.getAbsolutePath();

                    log.warning(corruptedPersistenceE.getClass().getSimpleName() + " has occurred. " +
                        "To diagnose it, make a backup of the following directories: " + walDirsStr + ". " +
                        "Then, run the following command: bin/wal-reader.sh " + args);
                }
                catch (Throwable t) {
                    String pages = Arrays.stream(corruptedPersistenceE.pages())
                        .map(t2 -> "(" + t2.get1() + ',' + t2.get2() + ')').collect(joining("", "[", "]"));

                    log.error("Failed to dump diagnostic info on tree corruption. PageIds=" + pages, t);
                }
            }
        }
    }

    /**
     * @return Reconciliation execution context.
     */
    public ReconciliationExecutionContext reconciliationExecutionContext() {
        return reconciliationExecutionContext;
    }

    /**
     * Creation and filling of a file with pages that can be corrupted.
     * Pages are written on each line in format "grpId:pageId".
     * File name format "corruptedPages_yyyy-MM-dd'_'HH-mm-ss_SSS.txt".
     *
     * @param dirPath Path to the directory where the file will be created.
     * @param ioFactory File I/O factory.
     * @param pages Pages that could be corrupted. Mapping: cache group id -> page id.
     * @return Created and filled file.
     * @throws IOException If an I/O error occurs.
     */
    public static File corruptedPagesFile(
        Path dirPath,
        FileIOFactory ioFactory,
        T2<Integer, Long>... pages
    ) throws IOException {
        dirPath.toFile().mkdirs();

        File f = dirPath.resolve("corruptedPages_" + LocalDateTime.now().format(TIME_FORMATTER) + ".txt").toFile();

        assert !f.exists();

        try (FileIO fileIO = ioFactory.create(f)) {
            for (T2<Integer, Long> p : pages) {
                byte[] bytes = (p.get1().toString() + ':' + p.get2().toString() + U.nl()).getBytes(UTF_8);

                int left = bytes.length;

                while ((left - fileIO.writeFully(bytes, bytes.length - left, left)) > 0)
                    ;
            }

            fileIO.force();
        }

        return f;
    }

    /**
     * Getting the WAL directories.
     * Note:
     * Index 0: WAL working directory.
     * Index 1: WAL archive directory (may be absent).
     *
     * @param ctx Kernal context.
     * @return WAL directories.
     */
    @Nullable static File[] walDirs(GridKernalContext ctx) {
        IgniteWriteAheadLogManager walMgr = ctx.cache().context().wal();

        if (walMgr instanceof FileWriteAheadLogManager) {
            SegmentRouter sr = ((FileWriteAheadLogManager)walMgr).getSegmentRouter();

            if (sr != null) {
                File workDir = sr.getWalWorkDir();
                return sr.hasArchive() ? F.asArray(workDir, sr.getWalArchiveDir()) : F.asArray(workDir);
            }
        }

        return null;
    }
}
