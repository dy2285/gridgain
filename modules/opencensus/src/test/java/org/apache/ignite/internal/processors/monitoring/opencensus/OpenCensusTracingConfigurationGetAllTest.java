/*
 * Copyright 2020 GridGain Systems, Inc. and Contributors.
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
import org.apache.ignite.internal.processors.tracing.configuration.TracingConfigurationCoordinates;
import org.apache.ignite.internal.processors.tracing.configuration.TracingConfigurationParameters;
import org.apache.ignite.spi.tracing.opencensus.OpenCensusTracingSpi;
import org.apache.ignite.internal.processors.tracing.Scope;
import org.apache.ignite.internal.processors.tracing.configuration.TracingConfiguration;
import org.junit.Test;

import static org.apache.ignite.internal.processors.tracing.Scope.TX;

/**
 * Tests for OpenCensus based {@link TracingConfiguration#getAll(Scope)}.
 */
public class OpenCensusTracingConfigurationGetAllTest extends AbstractTracingTest {
    /** {@inheritDoc} */
    @Override protected TracingSpi getTracingSpi() {
        return new OpenCensusTracingSpi();
    }

    /**
     * Ensure that getAll() retrieves default transaction configuration.
     */
    @Test
    public void testThatDefaultConfigurationReturnsIfScopeNotSpecifiedAndCustomConfigurationNotSet() {
        assertEquals(
            DFLT_CONFIG_MAP,
            grid(0).tracingConfiguration().getAll(null));
    }

    /**
     * Ensure that getAll(Scope) retrieves default scope specific transaction configuration.
     */
    @Test
    public void testThatDefaultScopeSpecificConfigurationReturnsIfScopeIsSpecifiedAndCustomConfigurationNotSet() {
        Map<TracingConfigurationCoordinates, TracingConfigurationParameters> expTracingCfg = new HashMap<>();

        expTracingCfg.put(TX_SCOPE_SPECIFIC_COORDINATES, TracingConfiguration.DEFAULT_TX_CONFIGURATION);

        assertEquals(
            expTracingCfg,
            grid(0).tracingConfiguration().getAll(TX));
    }

    /**
     * Update any scope specific configuration and add some label specific one.
     * Ensure that getAll() retrieves tracing configuration including updated one.
     */
    @Test
    public void testThatCustomConfigurationReturnsIfScopeNotSpecifiedAndCustomConfigurationIsSet() {
        grid(0).tracingConfiguration().set(TX_SCOPE_SPECIFIC_COORDINATES, UPDATED_SCOPE_SPECIFIC_PARAMETERS);

        grid(0).tracingConfiguration().set(TX_LABEL_SPECIFIC_COORDINATES, UPDATED_LABEL_SPECIFIC_PARAMETERS);

        Map<TracingConfigurationCoordinates, TracingConfigurationParameters> expTracingCfg =
            new HashMap<>(DFLT_CONFIG_MAP);

        expTracingCfg.put(TX_SCOPE_SPECIFIC_COORDINATES, UPDATED_SCOPE_SPECIFIC_PARAMETERS);

        expTracingCfg.put(TX_LABEL_SPECIFIC_COORDINATES, UPDATED_LABEL_SPECIFIC_PARAMETERS);

        assertEquals(
            expTracingCfg,
            grid(0).tracingConfiguration().getAll(null));
    }

    /**
     * Update any scope specific configuration and add some label specific one.
     * Ensure that getAll(scope) retrieves updated scope specific configuration.
     */
    @Test
    public void testThatCustomScopeSpecificConfigurationReturnsIfScopeIsSpecifiedAndCustomConfigurationIsSet() {
        grid(0).tracingConfiguration().set(TX_SCOPE_SPECIFIC_COORDINATES, UPDATED_SCOPE_SPECIFIC_PARAMETERS);

        grid(0).tracingConfiguration().set(TX_LABEL_SPECIFIC_COORDINATES, UPDATED_LABEL_SPECIFIC_PARAMETERS);

        Map<TracingConfigurationCoordinates, TracingConfigurationParameters> expTracingCfg = new HashMap<>();

        expTracingCfg.put(TX_SCOPE_SPECIFIC_COORDINATES, UPDATED_SCOPE_SPECIFIC_PARAMETERS);

        expTracingCfg.put(TX_LABEL_SPECIFIC_COORDINATES, UPDATED_LABEL_SPECIFIC_PARAMETERS);

        assertEquals(
            expTracingCfg,
            grid(0).tracingConfiguration().getAll(TX));
    }
}
