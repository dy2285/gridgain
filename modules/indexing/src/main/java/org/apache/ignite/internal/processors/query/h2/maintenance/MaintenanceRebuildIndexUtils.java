/*
 * Copyright 2022 GridGain Systems, Inc. and Contributors.
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

package org.apache.ignite.internal.processors.query.h2.maintenance;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.apache.ignite.maintenance.MaintenanceTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.apache.ignite.internal.processors.query.h2.IgniteH2Indexing.INDEX_REBUILD_MNTC_TASK_NAME;

/** Utility methods for the index rebuild maintenance task. */
public class MaintenanceRebuildIndexUtils {
    /** Separator for index rebuild maintenance task parameters. */
    static final String INDEX_REBUILD_PARAMETER_SEPARATOR = "|";

    /** Regex for {@link #INDEX_REBUILD_PARAMETER_SEPARATOR}. */
    public static final String INDEX_REBUILD_PARAMETER_SEPARATOR_REGEX = "\\|";

    /** Maintenance task description. */
    private static final String TASK_DESCRIPTION = "Corrupted index found";

    /** */
    private static final Base64.Encoder ENCODER = Base64.getEncoder();

    /** */
    private static final Base64.Decoder DECODER = Base64.getDecoder();

    /**
     * Parses {@link MaintenanceTask#parameters()} to a list of a MaintenanceRebuildIndexTargets.
     *
     * @param parameters Task's parameters.
     * @return List of MaintenanceRebuildIndexTargets.
     */
    public static List<MaintenanceRebuildIndexTarget> parseMaintenanceTaskParameters(@Nullable String parameters) {
        if (parameters == null)
            return Collections.emptyList();

        String[] parametersArray = parameters.split(INDEX_REBUILD_PARAMETER_SEPARATOR_REGEX);

        if (parametersArray.length == 0)
            return Collections.emptyList();

        assert (parametersArray.length % 2) == 0;

        List<MaintenanceRebuildIndexTarget> params = new ArrayList<>(parametersArray.length / 2);

        for (int i = 0; i < parametersArray.length; i += 2) {
            String idxNameEncoded = parametersArray[i + 1];

            String idxName = new String(
                DECODER.decode(idxNameEncoded.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8
            );

            params.add(new MaintenanceRebuildIndexTarget(Integer.parseInt(parametersArray[i]), idxName));
        }

        return params;
    }

    /**
     * Constructs an index rebuild maintenance task.
     *
     * @param cacheId Cache id.
     * @param idxName Index name.
     * @return Maintenance task.
     */
    public static MaintenanceTask toMaintenanceTask(int cacheId, @NotNull String idxName) {
        String encodedIdxName = ENCODER.encodeToString(idxName.getBytes(StandardCharsets.UTF_8));

        return new MaintenanceTask(
            INDEX_REBUILD_MNTC_TASK_NAME,
            TASK_DESCRIPTION,
            cacheId + INDEX_REBUILD_PARAMETER_SEPARATOR + encodedIdxName
        );
    }

    /**
     * Merges two index rebuild maintenance tasks concatenating their parameters.
     *
     * @param oldTask Old task
     * @param newTask New task.
     * @return Merged task.
     */
    public static MaintenanceTask mergeTasks(@NotNull MaintenanceTask oldTask, @NotNull MaintenanceTask newTask) {
        assert Objects.equals(INDEX_REBUILD_MNTC_TASK_NAME, oldTask.name());
        assert Objects.equals(TASK_DESCRIPTION, oldTask.description());
        assert Objects.equals(oldTask.name(), newTask.name());
        assert Objects.equals(oldTask.description(), newTask.description());

        String oldTaskParams = oldTask.parameters();
        String newTaskParams = newTask.parameters();

        if (oldTaskParams.contains(newTaskParams))
            return oldTask;

        String mergedParams = oldTaskParams + INDEX_REBUILD_PARAMETER_SEPARATOR + newTaskParams;

        return new MaintenanceTask(oldTask.name(), oldTask.description(), mergedParams);
    }
}
