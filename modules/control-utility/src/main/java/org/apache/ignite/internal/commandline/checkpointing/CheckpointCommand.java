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

package org.apache.ignite.internal.commandline.checkpointing;

import java.util.logging.Logger;
import org.apache.ignite.internal.client.GridClientConfiguration;
import org.apache.ignite.internal.commandline.AbstractCommand;
import org.apache.ignite.internal.commandline.Command;
import org.apache.ignite.internal.commandline.CommandArgIterator;

import static org.apache.ignite.internal.commandline.Command.usage;
import static org.apache.ignite.internal.commandline.CommandList.CHECKPOINT;
import static org.apache.ignite.internal.commandline.checkpointing.CheckpointingSubCommandsList.FORCE;

public class CheckpointCommand extends AbstractCommand<Object> {
    /**
     *
     */
    private Command<?> delegate;

    /** {@inheritDoc} */
    @Override public void printUsage(Logger log) {
        usage(log, "Start checkpointing process:",
            CHECKPOINT
        );
    }

    /** {@inheritDoc} */
    @Override public String name() {
        return CHECKPOINT.toCommandName();
    }

    @Override public void parseArguments(CommandArgIterator argIter) {
        if (!argIter.hasNextArg()) {
            delegate = FORCE.command();
            return;
        }

        CheckpointingSubCommandsList subcommand = CheckpointingSubCommandsList.parse(argIter.nextArg("Expected checkpointing action."));

        if (subcommand == null)
            throw new IllegalArgumentException("Expected correct checkpointing action.");

        delegate = subcommand.command();

        delegate.parseArguments(argIter);
    }

    /** {@inheritDoc} */
    @Override public Object execute(GridClientConfiguration clientCfg, Logger log) throws Exception {
        return delegate.execute(clientCfg, log);
    }

    /** {@inheritDoc} */
    @Override public Object arg() {
        return delegate.arg();
    }

}
