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

package org.apache.ignite.spi.communication.tcp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.IgniteException;
import org.apache.ignite.IgniteSystemProperties;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.events.Event;
import org.apache.ignite.internal.IgniteInternalFuture;
import org.apache.ignite.internal.IgniteInterruptedCheckedException;
import org.apache.ignite.internal.util.lang.GridAbsPredicate;
import org.apache.ignite.internal.util.nio.GridCommunicationClient;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.lang.IgnitePredicate;
import org.apache.ignite.lang.IgniteRunnable;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.internal.TcpDiscoveryNode;
import org.apache.ignite.testframework.GridTestUtils;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.apache.ignite.events.EventType.EVT_NODE_FAILED;

/**
 * Tests that faulty client will be failed if connection can't be established.
 */
@RunWith(JUnit4.class)
public class TcpCommunicationSpiFaultyClientTest extends GridCommonAbstractTest {
    /** Predicate. */
    private static final IgnitePredicate<ClusterNode> PRED = new IgnitePredicate<ClusterNode>() {
        @Override public boolean apply(ClusterNode node) {
            return block && node.order() == 3;
        }
    };

    /** Client mode. */
    private static boolean clientMode;

    /** Block. */
    private static volatile boolean block;

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String gridName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(gridName);

        cfg.setFailureDetectionTimeout(1000);
        cfg.setClientMode(clientMode);

        TestCommunicationSpi spi = new TestCommunicationSpi();

        spi.setIdleConnectionTimeout(100);
        spi.setSharedMemoryPort(-1);

        ((TcpDiscoverySpi)cfg.getDiscoverySpi()).setClientReconnectDisabled(true);

        cfg.setCommunicationSpi(spi);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        super.beforeTestsStarted();

        System.setProperty(IgniteSystemProperties.IGNITE_ENABLE_FORCIBLE_NODE_KILL,"true");
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        System.clearProperty(IgniteSystemProperties.IGNITE_ENABLE_FORCIBLE_NODE_KILL);
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        super.beforeTest();

        block = false;
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        super.afterTest();

        stopAllGrids();
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testNoServerOnHost() throws Exception {
        testFailClient(null);
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void testNotAcceptedConnection() throws Exception {
        testFailClient(new FakeServer());
    }

    /**
     * @param srv Server.
     * @throws Exception If failed.
     */
    private void testFailClient(FakeServer srv) throws Exception {
        IgniteInternalFuture<Long> fut = null;

        try {
            if (srv != null)
                fut = GridTestUtils.runMultiThreadedAsync(srv, 1, "fake-server");

            clientMode = false;

            startGrids(2);

            clientMode = true;

            startGrid(2);
            startGrid(3);

            U.sleep(1000); // Wait for write timeout and closing idle connections.

            final CountDownLatch latch = new CountDownLatch(1);

            grid(0).events().localListen(new IgnitePredicate<Event>() {
                @Override public boolean apply(Event event) {
                    latch.countDown();

                    return true;
                }
            }, EVT_NODE_FAILED);

            block = true;

            try {
                grid(0).compute(grid(0).cluster().forClients()).withNoFailover().broadcast(new IgniteRunnable() {
                    @Override public void run() {
                        // No-op.
                    }
                });
            }
            catch (IgniteException e) {
                // No-op.
            }

            assertTrue(latch.await(3, TimeUnit.SECONDS));

            assertTrue(GridTestUtils.waitForCondition(new GridAbsPredicate() {
                @Override public boolean apply() {
                    return grid(0).cluster().forClients().nodes().size() == 1;
                }
            }, 5000));

            for (int i = 0; i < 5; i++) {
                U.sleep(1000);

                log.info("Check topology (" + (i + 1) + "): " + grid(0).cluster().nodes());

                assertEquals(1, grid(0).cluster().forClients().nodes().size());
            }
        }
        finally {
            if (srv != null) {
                srv.stop();

                assert fut != null;

                fut.get();
            }

            stopAllGrids();
        }
    }

    /**
     * Server that emulates connection troubles.
     */
    private static class FakeServer implements Runnable {
        /** Server. */
        private final ServerSocket srv;

        /** Stop. */
        private volatile boolean stop;

        /**
         * Default constructor.
         */
        FakeServer() throws IOException {
            this.srv = new ServerSocket(47200, 50, InetAddress.getByName("127.0.0.1"));
        }

        /**
         *
         */
        public void stop() {
            stop = true;
        }

        /** {@inheritDoc} */
        @Override public void run() {
            try {
                while (!stop) {
                    try {
                        U.sleep(10);
                    }
                    catch (IgniteInterruptedCheckedException e) {
                        // No-op.
                    }
                }
            }
            finally {
                U.closeQuiet(srv);
            }
        }
    }

    /**
     *
     */
    private static class TestCommunicationSpi extends TcpCommunicationSpi {
        /** {@inheritDoc} */
        @Override protected GridCommunicationClient createTcpClient(ClusterNode node, int connIdx)
            throws IgniteCheckedException {
            if (PRED.apply(node)) {
                Map<String, Object> attrs = new HashMap<>(node.attributes());

                attrs.put(createAttributeName(ATTR_ADDRS), Collections.singleton("127.0.0.1"));
                attrs.put(createAttributeName(ATTR_PORT), 47200);
                attrs.put(createAttributeName(ATTR_EXT_ADDRS), Collections.emptyList());
                attrs.put(createAttributeName(ATTR_HOST_NAMES), Collections.emptyList());

                ((TcpDiscoveryNode)node).setAttributes(attrs);
            }

            return super.createTcpClient(node, connIdx);
        }

        /**
         * @param name Name.
         */
        private String createAttributeName(String name) {
            return getClass().getSimpleName() + '.' + name;
        }
    }
}
