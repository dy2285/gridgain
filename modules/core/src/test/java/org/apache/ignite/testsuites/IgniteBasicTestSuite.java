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

package org.apache.ignite.testsuites;

import org.apache.ignite.GridSuppressedExceptionSelfTest;
import org.apache.ignite.internal.ClusterGroupHostsSelfTest;
import org.apache.ignite.internal.ClusterGroupSelfTest;
import org.apache.ignite.internal.ClusterProcessorCheckGlobalStateComputeRequestTest;
import org.apache.ignite.internal.GridFailFastNodeFailureDetectionSelfTest;
import org.apache.ignite.internal.GridLifecycleAwareSelfTest;
import org.apache.ignite.internal.GridLifecycleBeanSelfTest;
import org.apache.ignite.internal.GridMBeansTest;
import org.apache.ignite.internal.GridMbeansMiscTest;
import org.apache.ignite.internal.GridNodeMetricsLogSelfTest;
import org.apache.ignite.internal.GridProjectionForCachesSelfTest;
import org.apache.ignite.internal.GridReduceSelfTest;
import org.apache.ignite.internal.GridReleaseTypeSelfTest;
import org.apache.ignite.internal.GridSelfTest;
import org.apache.ignite.internal.GridStartStopSelfTest;
import org.apache.ignite.internal.GridStopWithCancelSelfTest;
import org.apache.ignite.internal.GridStopWithCollisionSpiTest;
import org.apache.ignite.internal.IgniteLocalNodeMapBeforeStartTest;
import org.apache.ignite.internal.IgniteSlowClientDetectionSelfTest;
import org.apache.ignite.internal.TransactionsMXBeanImplTest;
import org.apache.ignite.internal.processors.affinity.GridAffinityAssignmentV2Test;
import org.apache.ignite.internal.processors.affinity.GridAffinityAssignmentV2TestNoOptimizations;
import org.apache.ignite.internal.processors.affinity.GridAffinityProcessorMemoryLeakTest;
import org.apache.ignite.internal.processors.affinity.GridAffinityProcessorRendezvousSelfTest;
import org.apache.ignite.internal.processors.affinity.GridHistoryAffinityAssignmentTest;
import org.apache.ignite.internal.processors.affinity.GridHistoryAffinityAssignmentTestNoOptimization;
import org.apache.ignite.internal.processors.cache.GridLocalIgniteSerializationTest;
import org.apache.ignite.internal.processors.cache.GridProjectionForCachesOnDaemonNodeSelfTest;
import org.apache.ignite.internal.processors.cache.IgniteDaemonNodeMarshallerCacheTest;
import org.apache.ignite.internal.processors.cache.IgniteMarshallerCacheConcurrentReadWriteTest;
import org.apache.ignite.internal.processors.cache.SetTxTimeoutOnPartitionMapExchangeTest;
import org.apache.ignite.internal.processors.cache.distributed.dht.topology.EvictPartitionInLogTest;
import org.apache.ignite.internal.processors.cache.distributed.dht.topology.PartitionEvictionOrderTest;
import org.apache.ignite.internal.processors.cache.query.continuous.DiscoveryDataDeserializationFailureHanderTest;
import org.apache.ignite.internal.processors.closure.GridClosureProcessorRemoteTest;
import org.apache.ignite.internal.processors.closure.GridClosureProcessorSelfTest;
import org.apache.ignite.internal.processors.closure.GridClosureSerializationTest;
import org.apache.ignite.internal.processors.continuous.GridEventConsumeSelfTest;
import org.apache.ignite.internal.processors.continuous.GridMessageListenSelfTest;
import org.apache.ignite.internal.processors.odbc.ClientListenerMetricsTest;
import org.apache.ignite.internal.processors.odbc.OdbcConfigurationValidationSelfTest;
import org.apache.ignite.internal.processors.odbc.OdbcEscapeSequenceSelfTest;
import org.apache.ignite.internal.processors.odbc.SqlListenerUtilsTest;
import org.apache.ignite.internal.product.FeaturesIsNotAvailableTest;
import org.apache.ignite.internal.product.GridProductVersionSelfTest;
import org.apache.ignite.internal.util.nio.IgniteExceptionInNioWorkerSelfTest;
import org.apache.ignite.messaging.GridMessagingNoPeerClassLoadingSelfTest;
import org.apache.ignite.messaging.GridMessagingSelfTest;
import org.apache.ignite.messaging.IgniteMessagingSendAsyncTest;
import org.apache.ignite.messaging.IgniteMessagingWithClientTest;
import org.apache.ignite.spi.GridSpiLocalHostInjectionTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Basic test suite.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    IgniteMarshallerSelfTestSuite.class,
    IgniteLangSelfTestSuite.class,
    IgniteUtilSelfTestSuite.class,

    IgniteKernalSelfTestSuite.class,
    IgniteStartUpTestSuite.class,
    IgniteExternalizableSelfTestSuite.class,
    IgniteP2PSelfTestSuite.class,
    IgniteCacheP2pUnmarshallingErrorTestSuite.class,
    IgniteStreamSelfTestSuite.class,

    IgnitePlatformsTestSuite.class,

    SecurityTestSuite.class,

    GridSelfTest.class,
    ClusterGroupHostsSelfTest.class,
    IgniteMessagingWithClientTest.class,
    IgniteMessagingSendAsyncTest.class,

    ClusterProcessorCheckGlobalStateComputeRequestTest.class,
    ClusterGroupSelfTest.class,
    GridMessagingSelfTest.class,
    GridMessagingNoPeerClassLoadingSelfTest.class,

    GridReleaseTypeSelfTest.class,
    GridProductVersionSelfTest.class,
    GridAffinityAssignmentV2Test.class,
    GridAffinityAssignmentV2TestNoOptimizations.class,
    GridHistoryAffinityAssignmentTest.class,
    GridHistoryAffinityAssignmentTestNoOptimization.class,
    GridAffinityProcessorRendezvousSelfTest.class,
    GridAffinityProcessorMemoryLeakTest.class,
    GridClosureProcessorSelfTest.class,
    GridClosureProcessorRemoteTest.class,
    GridClosureSerializationTest.class,
    GridStartStopSelfTest.class,
    GridProjectionForCachesSelfTest.class,
    GridProjectionForCachesOnDaemonNodeSelfTest.class,
    GridSpiLocalHostInjectionTest.class,
    GridLifecycleBeanSelfTest.class,
    GridStopWithCancelSelfTest.class,
    GridStopWithCollisionSpiTest.class,
    GridReduceSelfTest.class,
    GridEventConsumeSelfTest.class,
    GridSuppressedExceptionSelfTest.class,
    GridLifecycleAwareSelfTest.class,
    GridMessageListenSelfTest.class,
    GridFailFastNodeFailureDetectionSelfTest.class,
    IgniteSlowClientDetectionSelfTest.class,
    IgniteDaemonNodeMarshallerCacheTest.class,
    IgniteMarshallerCacheConcurrentReadWriteTest.class,
    GridNodeMetricsLogSelfTest.class,
    GridLocalIgniteSerializationTest.class,
    GridMBeansTest.class,
    GridMbeansMiscTest.class,
    TransactionsMXBeanImplTest.class,
    SetTxTimeoutOnPartitionMapExchangeTest.class,
    DiscoveryDataDeserializationFailureHanderTest.class,

    FeaturesIsNotAvailableTest.class,

    EvictPartitionInLogTest.class,
    PartitionEvictionOrderTest.class,

    IgniteExceptionInNioWorkerSelfTest.class,
    IgniteLocalNodeMapBeforeStartTest.class,

    ClientListenerMetricsTest.class,
    OdbcConfigurationValidationSelfTest.class,
    OdbcEscapeSequenceSelfTest.class,
    SqlListenerUtilsTest.class,
})
public class IgniteBasicTestSuite {
}
