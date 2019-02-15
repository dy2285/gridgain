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

package org.apache.ignite.spi;

import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.internal.processors.resource.GridResourceProcessor;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.testframework.junits.GridTestKernalContext;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * This class tests injection of {@code localHost} property to various SPIs.
 */
@RunWith(JUnit4.class)
public class GridSpiLocalHostInjectionTest extends GridCommonAbstractTest {
    /** Value to be set globally in config. */
    public static final String CONFIG_LOCAL_ADDR_VALUE = "127.0.0.3";

    /** Value to be set locally to SPI.before injection */
    public static final String SPI_LOCAL_ADDR_VALUE = "127.0.0.2";

    /**
     * @throws IgniteCheckedException If test fails.
     */
    @Test
    public void testTcpDiscoverySpiBothSet() throws IgniteCheckedException {
        processTcpDiscoverySpiTestInjection(true, true, SPI_LOCAL_ADDR_VALUE);
    }

    /**
     * @throws IgniteCheckedException If test fails.
     */
    @Test
    public void testTcpDiscoverySpiOnlySet() throws IgniteCheckedException {
        processTcpDiscoverySpiTestInjection(false, true, SPI_LOCAL_ADDR_VALUE);
    }

    /**
     * @throws IgniteCheckedException If test fails.
     */
    @Test
    public void testTcpDiscoverySpiConfigOnlySet() throws IgniteCheckedException {
        processTcpDiscoverySpiTestInjection(true, false, CONFIG_LOCAL_ADDR_VALUE);
    }

    /**
     * @throws IgniteCheckedException If test fails.
     */
    @Test
    public void testTcpDiscoverySpiBothNotSet() throws IgniteCheckedException {
        processTcpDiscoverySpiTestInjection(false, false, null);
    }

    /**
     * @throws IgniteCheckedException If test fails.
     */
    @Test
    public void testTcpCommunicationSpiBothSet() throws IgniteCheckedException {
        processTcpCommunicationSpiTestInjection(true, true, SPI_LOCAL_ADDR_VALUE);
    }

    /**
     * @throws IgniteCheckedException If test fails.
     */
    @Test
    public void testTcpCommunicationSpiOnlySet() throws IgniteCheckedException {
        processTcpCommunicationSpiTestInjection(false, true, SPI_LOCAL_ADDR_VALUE);
    }

    /**
     * @throws IgniteCheckedException If test fails.
     */
    @Test
    public void testTcpCommunicationSpiConfigOnlySet() throws IgniteCheckedException {
        processTcpCommunicationSpiTestInjection(true, false, CONFIG_LOCAL_ADDR_VALUE);
    }

    /**
     * @throws IgniteCheckedException If test fails.
     */
    @Test
    public void testTcpCommunicationSpiBothNotSet() throws IgniteCheckedException {
        processTcpCommunicationSpiTestInjection(false, false, null);
    }

    /**
     * Performs test of {@code localHost} resource injection for {@link org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi}.
     *
     * @param cfgVal {@code true} if {@code localHost} should be set in configuration adapter.
     * @param spiVal {@code true} if {@code localHost} should be set in SPI
     * @param exp Expected value of {@code localHost} property in SPI after injection.
     * @throws IgniteCheckedException If test fails.
     */
    private void processTcpDiscoverySpiTestInjection(boolean cfgVal, boolean spiVal, @Nullable String exp)
        throws IgniteCheckedException {
        GridResourceProcessor proc = getResourceProcessor(cfgVal);

        TcpDiscoverySpi spi = new TcpDiscoverySpi();

        if (spiVal)
            spi.setLocalAddress(SPI_LOCAL_ADDR_VALUE);

        proc.inject(spi);

        assertEquals("Invalid localAddr value after injection: ", exp, spi.getLocalAddress());
    }

    /**
     * Performs test of {@code localHost} resource injection for {@link org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi}.
     *
     * @param cfgVal {@code true} if {@code localHost} should be set in configuration adapter.
     * @param spiVal {@code true} if {@code localHost} should be set in SPI
     * @param exp Expected value of {@code localHost} property in SPI after injection.
     * @throws IgniteCheckedException If test fails.
     */
    private void processTcpCommunicationSpiTestInjection(boolean cfgVal, boolean spiVal, @Nullable String exp)
        throws IgniteCheckedException {
        GridResourceProcessor proc = getResourceProcessor(cfgVal);

        TcpCommunicationSpi spi = new TcpCommunicationSpi();

        if (spiVal)
            spi.setLocalAddress(SPI_LOCAL_ADDR_VALUE);

        proc.inject(spi);

        assertEquals("Invalid localAddr value after injection: ", exp, spi.getLocalAddress());
    }

    /**
     * Get test resource processor with or without {@code localHost} property set.
     *
     * @param cfgVal {@code true} if {@code localHost} property value should be set to configuration.
     * @return Resource processor.
     */
    private GridResourceProcessor getResourceProcessor(boolean cfgVal) throws IgniteCheckedException {
        GridTestKernalContext ctx = newContext();

        if (cfgVal)
            ctx.config().setLocalHost(CONFIG_LOCAL_ADDR_VALUE);

        GridResourceProcessor proc = new GridResourceProcessor(ctx);

        proc.setSpringContext(null);

        return proc;
    }
}
