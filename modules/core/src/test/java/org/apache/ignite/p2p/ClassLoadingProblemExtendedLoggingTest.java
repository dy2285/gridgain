/*
 * Copyright 2020 GridGain Systems, Inc. and Contributors.
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
package org.apache.ignite.p2p;

import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.ScanQuery;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.internal.TestRecordingCommunicationSpi;
import org.apache.ignite.internal.managers.deployment.GridDeploymentRequest;
import org.apache.ignite.internal.managers.deployment.GridDeploymentResponse;
import org.apache.ignite.internal.util.typedef.internal.LT;
import org.apache.ignite.lang.IgniteBiPredicate;
import org.apache.ignite.testframework.ListeningTestLogger;
import org.apache.ignite.testframework.LogListener;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static com.google.common.primitives.Ints.asList;
import static org.apache.ignite.configuration.DeploymentMode.SHARED;
import static org.apache.ignite.internal.TestRecordingCommunicationSpi.spi;
import static org.apache.ignite.testframework.GridTestUtils.setFieldValue;

/**
 * Tests of extended logging of class loading problems.
 */
@RunWith(Parameterized.class)
public class ClassLoadingProblemExtendedLoggingTest extends GridCommonAbstractTest {
    /** Test predicate class name. */
    private static final String PREDICATE_NAME = "org.apache.ignite.tests.p2p.P2PTestPredicate";

    /** */
    private ListeningTestLogger listeningLog = new ListeningTestLogger(log);

    /** */
    private IgniteEx ignite;

    /** */
    private IgniteEx client;

    /** */
    @Parameterized.Parameter(0)
    public Integer allowSuccessfulClassRequestsCnt;

    @Parameterized.Parameters(name = "{0}")
    public static List<Integer> allowSuccessfulClassRequestsCntList() {
        return asList(0, 1);
    }

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        return super.getConfiguration(igniteInstanceName)
            .setPeerClassLoadingEnabled(true)
            .setDeploymentMode(SHARED)
            .setCommunicationSpi(new TestRecordingCommunicationSpi())
            .setGridLogger(listeningLog)
            .setNetworkTimeout(1000);
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        super.beforeTest();

        stopAllGrids();

        LT.clear();

        listeningLog.clearListeners();

        ignite = startGrid(0);

        client = startClientGrid(1);
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();

        LT.clear();

        super.afterTest();
    }

    /** Tests logging when executing job with communication problems. */
    @Test
    public void testTimeoutJob() throws ClassNotFoundException {
        LogListener lsnr1 = LogListener
            .matches(msg -> msg
                .replace("\n", "")
                .matches(".*?Failed to get resource from node \\(is node alive\\?\\).*?" +
                    TimeoutException.class.getName() + ".*")
            )
            .build();

        LogListener lsnr2 = LogListener
            .matches(msg -> msg
                .replace("\n", "")
                .matches(".*?Failed to peer load class.*?" +
                    TimeoutException.class.getName() + ".*")
            )
            .build();

        listeningLog.registerListener(lsnr1);
        listeningLog.registerListener(lsnr2);

        TestRecordingCommunicationSpi clientSpi = spi(client);

        AtomicInteger reqCntr = new AtomicInteger(0);

        spi(ignite).closure((node, msg) -> {
            if (msg instanceof GridDeploymentRequest && allowSuccessfulClassRequestsCnt - reqCntr.get() <= 0)
                clientSpi.blockMessages(GridDeploymentResponse.class, ignite.name());

            reqCntr.incrementAndGet();
        });

        Class cls = getExternalClassLoader()
            .loadClass("org.apache.ignite.tests.p2p.P2PTestTaskExternalPath1");

        try {
            client.compute().execute(cls, ignite.cluster().localNode().id());
        }
        catch (Exception ignored) {
            /* No-op. */
        }

        doSleep(2000);

        assertTrue(lsnr1.check() || lsnr2.check());

        clientSpi.stopBlock();
    }

    /** Tests logging when executing scan query with communication problems. */
    @Test
    public void testTimeoutScanQuery() throws ClassNotFoundException {
        LogListener lsnr1= LogListener
            .matches(msg -> msg
                .replace("\n", "")
                .matches(".*?Failed to get resource from node \\(is node alive\\?\\).*?" +
                    TimeoutException.class.getName() + ".*")
            )
            .build();

        LogListener lsnr2 = LogListener
            .matches(msg -> msg
                .replace("\n", "")
                .matches(".*?Failed to peer load class.*?" +
                    TimeoutException.class.getName() + ".*")
            )
            .build();

        listeningLog.registerListener(lsnr1);
        listeningLog.registerListener(lsnr2);

        TestRecordingCommunicationSpi clientSpi = spi(client);

        AtomicInteger reqCntr = new AtomicInteger(0);

        spi(ignite).closure((node, msg) -> {
            if (msg instanceof GridDeploymentRequest && allowSuccessfulClassRequestsCnt - reqCntr.get() <= 0)
                clientSpi.blockMessages(GridDeploymentResponse.class, ignite.name());

            reqCntr.incrementAndGet();
        });

        try {
            IgniteCache<Integer, Integer> cache = client.getOrCreateCache(DEFAULT_CACHE_NAME);

            Class<IgniteBiPredicate> predCls = (Class<IgniteBiPredicate>)getExternalClassLoader().loadClass(PREDICATE_NAME);

            cache.query(new ScanQuery<>(predCls.newInstance())).getAll();
        }
        catch (Exception ignored) {
            /* No-op. */
        }

        doSleep(2000);

        assertTrue(lsnr1.check() || lsnr2.check());

        clientSpi.stopBlock();
    }

    /** Tests logging when executing job and class is not found on initiator. */
    @Test
    public void testCNFEJob() throws Exception {
        LogListener srvLsnr1 = LogListener.matches("Failed to get resource from node").build();
        LogListener srvLsnr2 = LogListener.matches("Failed to find class on remote node").build();
        LogListener clientLsnr = LogListener.matches("Failed to resolve class").build();

        listeningLog.registerListener(srvLsnr1);
        listeningLog.registerListener(srvLsnr2);
        listeningLog.registerListener(clientLsnr);

        AtomicInteger reqCntr = new AtomicInteger(0);

        spi(ignite).closure((node, msg) -> {
            if (msg instanceof GridDeploymentRequest && allowSuccessfulClassRequestsCnt - reqCntr.get() <= 0)
                setFieldValue(msg, "rsrcName", "asdf");

            reqCntr.incrementAndGet();
        });

        Class cls = getExternalClassLoader()
            .loadClass("org.apache.ignite.tests.p2p.P2PTestTaskExternalPath1");

        try {
            client.compute().execute(cls, ignite.cluster().localNode().id());
        }
        catch (Exception ignored) {
            /* No-op. */
        }

        assertTrue(srvLsnr1.check() || srvLsnr2.check());
        assertTrue(clientLsnr.check());

        spi(ignite).closure(null);
    }

    /** Tests logging when executing scan query and class is not found on initiator. */
    @Test
    public void testCNFEScanQuery() throws Exception {
        LogListener srvLsnr1 = LogListener.matches("Failed to get resource from node").build();
        LogListener srvLsnr2 = LogListener.matches("Failed to find class on remote node").build();
        LogListener clientLsnr = LogListener.matches("Failed to resolve class").build();

        listeningLog.registerListener(srvLsnr1);
        listeningLog.registerListener(srvLsnr2);
        listeningLog.registerListener(clientLsnr);

        AtomicInteger reqCntr = new AtomicInteger(0);

        spi(ignite).closure((node, msg) -> {
            if (msg instanceof GridDeploymentRequest && allowSuccessfulClassRequestsCnt - reqCntr.get() <= 0)
                setFieldValue(msg, "rsrcName", "asdf");

            reqCntr.incrementAndGet();
        });

        try {
            IgniteCache<Integer, Integer> cache = client.getOrCreateCache(DEFAULT_CACHE_NAME);

            Class<IgniteBiPredicate> predCls = (Class<IgniteBiPredicate>)getExternalClassLoader().loadClass(PREDICATE_NAME);

            cache.query(new ScanQuery<>(predCls.newInstance())).getAll();
        }
        catch (Exception ignored) {
            /* No-op. */
        }

        assertTrue(srvLsnr1.check() || srvLsnr2.check());
        assertTrue(clientLsnr.check());

        spi(ignite).closure(null);
    }
}
