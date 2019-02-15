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

package org.apache.ignite.internal.processors.resource;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.jetbrains.annotations.NotNull;

/**
 * Wrapper for data where resource should be injected.
 * Bean contains {@link Field} and {@link Annotation} for that class field.
 */
class GridResourceField {
    /** */
    static final GridResourceField[] EMPTY_ARRAY = new GridResourceField[0];

    /** Field where resource should be injected. */
    private final Field field;

    /** Resource annotation. */
    private final Annotation ann;

    /**
     * Creates new bean.
     *
     * @param field Field where resource should be injected.
     * @param ann Resource annotation.
     */
    GridResourceField(@NotNull Field field, @NotNull Annotation ann) {
        this.field = field;
        this.ann = ann;

        field.setAccessible(true);
    }

    /**
     * Gets class field object.
     *
     * @return Class field.
     */
    public Field getField() {
        return field;
    }

    /**
     * Gets annotation for class field object.
     *
     * @return Field annotation.
     */
    public Annotation getAnnotation() {
        return ann;
    }

    /**
     * Return {@code true} if field contains object that should be process too.
     */
    public boolean processFieldValue() {
        return ann == null;
    }

    /**
     * @param c Closure.
     */
    public static GridResourceField[] toArray(Collection<GridResourceField> c) {
        if (c.isEmpty())
            return EMPTY_ARRAY;

        return c.toArray(new GridResourceField[c.size()]);
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridResourceField.class, this);
    }
}