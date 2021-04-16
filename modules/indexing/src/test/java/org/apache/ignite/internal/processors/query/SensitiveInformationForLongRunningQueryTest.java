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

package org.apache.ignite.internal.processors.query;

import java.util.regex.Pattern;

import org.apache.ignite.internal.processors.query.h2.H2Utils;
import org.apache.ignite.testframework.junits.WithSystemProperty;

import static org.apache.ignite.IgniteSystemProperties.IGNITE_SENSITIVE_DATA_LOGGING;

/**
 * Tests for log print for long running query.
 */
@WithSystemProperty(key = IGNITE_SENSITIVE_DATA_LOGGING, value = "none")
public class SensitiveInformationForLongRunningQueryTest extends LongRunningQueryTest {
    /** {@inheritDoc} */
    @Override protected Pattern longRunningPattern() {
        return Pattern.compile(H2Utils.SENSITIVE_DATA_MSG);
    }

    /** {@inheritDoc} */
    @Override protected Pattern hugeResultsPattern() {
        return Pattern.compile(H2Utils.SENSITIVE_DATA_MSG);
    }
}