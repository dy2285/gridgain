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

package org.apache.ignite.internal.util.tostring;

import org.apache.ignite.IgniteSystemProperties;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Attach this annotation to a field or a class to indicate that this field or fields of this
 * class <b>should</b> be included in {@code toString()} output. This annotation allows
 * to override the default exclusion policy.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface GridToStringInclude {
    /**
     * A flag indicating a sensitive information stored in the field or fields of the class.<br/>
     * Such information will be included in {@code toString()} output ONLY when the system property
     * {@link IgniteSystemProperties#IGNITE_SENSITIVE_DATA_LOGGING}
     * is set to {@code true}.
     *
     * @return Attribute value.
     */
    boolean sensitive() default false;
}