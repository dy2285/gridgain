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

package org.apache.ignite.util;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteException;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.internal.managers.communication.GridIoMessage;
import org.apache.ignite.lang.IgniteInClosure;
import org.apache.ignite.plugin.extensions.communication.Message;
import org.apache.ignite.spi.IgniteSpiException;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;

/**
 * TcpCommunicationSpi with additional features needed for tests.
 */
public class TestTcpCommunicationSpi extends TcpCommunicationSpi {
    /** */
    private volatile boolean stopped;

    /** */
    private Class ignoreMsg;

    /** {@inheritDoc} */
    @Override public void sendMessage(final ClusterNode node, final Message msg,
        IgniteInClosure<IgniteException> ackClosure) throws IgniteSpiException {
        if (stopped)
            return;

        if (ignoreMsg != null && ((GridIoMessage)msg).message().getClass().equals(ignoreMsg))
            return;

        super.sendMessage(node, msg, ackClosure);
    }

    /**
     *
     */
    public void stop() {
        stopped = true;
    }

    /**
     *
     */
    public void stop(Class ignoreMsg) {
        this.ignoreMsg = ignoreMsg;
    }

    /**
     * Stop SPI, messages will not send anymore.
     */
    public static void stop(Ignite ignite) {
        ((TestTcpCommunicationSpi)ignite.configuration().getCommunicationSpi()).stop();
    }

    /**
     * Skip messages will not send anymore.
     */
    public static void skipMsgType(Ignite ignite, Class clazz) {
        ((TestTcpCommunicationSpi)ignite.configuration().getCommunicationSpi()).stop(clazz);
    }
}