/*
 * Copyright 2019 GridGain Systems, Inc. and Contributors.
 *
 * Licensed under the GridGain Community Edition License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.gridgain.com/products/software/community-edition/gridgain-community-edition-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.processors.monitoring.opencensus;

import java.util.HashMap;
import java.util.Map;
import org.apache.ignite.internal.processors.tracing.TracingSpi;
import org.apache.ignite.internal.processors.tracing.configuration.TracingConfiguration;
import org.apache.ignite.internal.processors.tracing.configuration.TracingConfigurationCoordinates;
import org.apache.ignite.internal.processors.tracing.configuration.TracingConfigurationParameters;
import org.apache.ignite.spi.tracing.opencensus.OpenCensusTracingSpi;
import org.junit.Test;

/**
 * Tests for OpenCensus based {@link TracingConfiguration#reset(TracingConfigurationCoordinates)}.
 */
public class OpenCensusTracingConfigurationResetTest  extends AbstractTracingTest {
    /** {@inheritDoc} */
    @Override protected TracingSpi getTracingSpi() {
        return new OpenCensusTracingSpi();
    }

    /**
     * Ensure that resetting default tracing configuration doesn't effect it.
     */
    @Test
    public void testThatResettingDefaultTracingConfigurationDoesNotEffectIt() {
        grid(0).tracingConfiguration().reset(TX_SCOPE_SPECIFIC_COORDINATES);

        assertEquals(
            DFLT_CONFIG_MAP,
            grid(0).tracingConfiguration().getAll(null));
    }

    /**
     * Update any scope specific configuration and add some label specific one.
     * Reset scope specific configuration and ensure that it was restored to default,
     * also ensure that label specific configuration wasn't affected.
     */
    @Test
    public void testThatResettingScopeSpecificConfigurationWithScopeAndLabelSpecificItemsRestoresOnlyScopeToDefault() {
        grid(0).tracingConfiguration().set(TX_SCOPE_SPECIFIC_COORDINATES, UPDATED_SCOPE_SPECIFIC_PARAMETERS);

        grid(0).tracingConfiguration().set(TX_LABEL_SPECIFIC_COORDINATES, UPDATED_LABEL_SPECIFIC_PARAMETERS);

        grid(0).tracingConfiguration().set(EXCHANGE_SCOPE_SPECIFIC_COORDINATES, UPDATED_SCOPE_SPECIFIC_PARAMETERS);


        Map<TracingConfigurationCoordinates, TracingConfigurationParameters> expTracingCfg =
            new HashMap<>(DFLT_CONFIG_MAP);

        expTracingCfg.put(TX_SCOPE_SPECIFIC_COORDINATES, UPDATED_SCOPE_SPECIFIC_PARAMETERS);

        expTracingCfg.put(TX_LABEL_SPECIFIC_COORDINATES, UPDATED_LABEL_SPECIFIC_PARAMETERS);

        expTracingCfg.put(EXCHANGE_SCOPE_SPECIFIC_COORDINATES, UPDATED_SCOPE_SPECIFIC_PARAMETERS);

        // Check tracing configuration. Just in case.
        assertEquals(
            expTracingCfg,
            grid(0).tracingConfiguration().getAll(null));

        // Reset scope specific configuration.
        grid(0).tracingConfiguration().reset(TX_SCOPE_SPECIFIC_COORDINATES);

        // Modify expected tracing configuration map to reflect changes after resetting scope specific configuration.
        expTracingCfg.put(TX_SCOPE_SPECIFIC_COORDINATES, TracingConfiguration.DEFAULT_TX_CONFIGURATION);

        // Check tracing configuration after resetting.
        assertEquals(
            expTracingCfg,
            grid(0).tracingConfiguration().getAll(null));

        // Just in case, assert with simple get()
        assertEquals(
            TracingConfiguration.DEFAULT_TX_CONFIGURATION,
            grid(0).tracingConfiguration().get(TX_SCOPE_SPECIFIC_COORDINATES));

        assertEquals(
            UPDATED_LABEL_SPECIFIC_PARAMETERS,
            grid(0).tracingConfiguration().get(TX_LABEL_SPECIFIC_COORDINATES));

        assertEquals(
            UPDATED_SCOPE_SPECIFIC_PARAMETERS,
            grid(0).tracingConfiguration().get(EXCHANGE_SCOPE_SPECIFIC_COORDINATES));
    }

    /**
     * Update any scope specific configuration and add some label specific one.
     * Reset label specific configuration and ensure that it was restored to default,
     * also ensure that scope specific configuration wasn't affected.
     */
    @Test
    public void testThatResettingLabelSpecificConfigurationWithScopeAndLabelSpecificItemsRestoresOnlyLabelToDefault() {
        grid(0).tracingConfiguration().set(TX_SCOPE_SPECIFIC_COORDINATES, UPDATED_SCOPE_SPECIFIC_PARAMETERS);

        grid(0).tracingConfiguration().set(TX_LABEL_SPECIFIC_COORDINATES, UPDATED_LABEL_SPECIFIC_PARAMETERS);

        grid(0).tracingConfiguration().set(EXCHANGE_SCOPE_SPECIFIC_COORDINATES, UPDATED_SCOPE_SPECIFIC_PARAMETERS);


        Map<TracingConfigurationCoordinates, TracingConfigurationParameters> expTracingCfg =
            new HashMap<>(DFLT_CONFIG_MAP);

        expTracingCfg.put(TX_SCOPE_SPECIFIC_COORDINATES, UPDATED_SCOPE_SPECIFIC_PARAMETERS);

        expTracingCfg.put(TX_LABEL_SPECIFIC_COORDINATES, UPDATED_LABEL_SPECIFIC_PARAMETERS);

        expTracingCfg.put(EXCHANGE_SCOPE_SPECIFIC_COORDINATES, UPDATED_SCOPE_SPECIFIC_PARAMETERS);

        // Check tracing configuration. Just in case.
        assertEquals(
            expTracingCfg,
            grid(0).tracingConfiguration().getAll(null));

        // Reset scope specific configuration.
        grid(0).tracingConfiguration().reset(TX_LABEL_SPECIFIC_COORDINATES);

        // Modify expected tracing configuration map to reflect changes after resetting scope specific configuration.
        expTracingCfg.remove(TX_LABEL_SPECIFIC_COORDINATES);

        // Check tracing configuration after resetting.
        assertEquals(
            expTracingCfg,
            grid(0).tracingConfiguration().getAll(null));

        // Just in case, assert with simple get()
        assertEquals(
            UPDATED_SCOPE_SPECIFIC_PARAMETERS,
            grid(0).tracingConfiguration().get(TX_SCOPE_SPECIFIC_COORDINATES));

        // Cause there's no label specific configuration after reset, scope specific should be returned.
        assertEquals(
            UPDATED_SCOPE_SPECIFIC_PARAMETERS,
            grid(0).tracingConfiguration().get(TX_LABEL_SPECIFIC_COORDINATES));

        assertEquals(
            UPDATED_SCOPE_SPECIFIC_PARAMETERS,
            grid(0).tracingConfiguration().get(EXCHANGE_SCOPE_SPECIFIC_COORDINATES));
    }
}
