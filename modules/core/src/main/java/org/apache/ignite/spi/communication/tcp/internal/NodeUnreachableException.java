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

package org.apache.ignite.spi.communication.tcp.internal;

import org.apache.ignite.spi.IgniteSpiException;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;

/**
 * Exception is thrown by {@link TcpCommunicationSpi} when some or all addresses of a node are unreachable and
 * direct communication connection cannot be established.
 *
 * <p>
 *     Ability to open direct connections between any nodes in cluster in any direction
 *     is necessary for proper functioning of the cluster.
 * </p>
 * <p>
 *     However if some nodes deployed without open public IPs (e.g. client deployed in a Kubernetes environment)
 *     this invariant is broken: these nodes still can open connections to other nodes
 *     but no other nodes are able to connect to such nodes.
 * </p>
 * <p>
 *     To enable connections to such "hidden" nodes inverse connection protocol is used: when a node detects
 *     that it cannot reach this "hidden" node it throws this exception and triggers the protocol.
 * </p>
 */
public class NodeUnreachableException extends IgniteSpiException {
    /** Serial version uid. */
    private static final long serialVersionUID = 0L;

    /** */
    public NodeUnreachableException(String msg) {
        super(msg);

        System.out.println("<$> Unreachable exception created: " + msg);
        Thread.dumpStack();
    }
}
