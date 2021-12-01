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

import org.apache.ignite.cache.QueryEntity;
import org.apache.ignite.internal.util.typedef.F;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Extended query entity with not-null fields support.
 */
public class QueryEntityEx extends QueryEntity {
    /** */
    private static final long serialVersionUID = 0L;

    /** Fields that must have non-null value. */
    private Set<String> notNullFields;

    /** Whether to preserve order specified by {@link #getKeyFields()} or not. */
    private boolean preserveKeysOrder;
    
    /** Whether to allow deduplication of composite PKs with null parts or not. */
    private boolean forceFillAbsentPKsWithDefaults;
    
    /**
     * Default constructor.
     */
    public QueryEntityEx() {
        // No-op.
    }

    /**
     * Copying constructor.
     *
     * @param other Instance to copy.
     */
    public QueryEntityEx(QueryEntity other) {
        super(other);

        if (other instanceof QueryEntityEx) {
            QueryEntityEx other0 = (QueryEntityEx)other;

            notNullFields = other0.notNullFields != null ? new HashSet<>(other0.notNullFields) : null;

            preserveKeysOrder = other0.preserveKeysOrder;
            
            forceFillAbsentPKsWithDefaults = other0.forceFillAbsentPKsWithDefaults;
        }
    }

    /** {@inheritDoc} */
    @Override @Nullable public Set<String> getNotNullFields() {
        return notNullFields;
    }

    /** {@inheritDoc} */
    @Override public QueryEntity setNotNullFields(@Nullable Set<String> notNullFields) {
        this.notNullFields = notNullFields;

        return this;
    }

    /**
     * @return {@code true} if order should be preserved, {@code false} otherwise.
     */
    public boolean isPreserveKeysOrder() {
        return preserveKeysOrder;
    }

    /**
     * @param preserveKeysOrder Whether the order should be preserved or not.
     * @return {@code this} for chaining.
     */
    public QueryEntity setPreserveKeysOrder(boolean preserveKeysOrder) {
        this.preserveKeysOrder = preserveKeysOrder;

        return this;
    }
    
    /**
     * @return {@code true} if PKs with null parts can be deduplicate, {@code false} otherwise.
     */
    public boolean forceFillAbsentPKsWithDefaults() {
        return forceFillAbsentPKsWithDefaults;
    }
    
    /**
     * @param forceFillAbsentPKsWithDefaults Whether the PKs should be deduplicate or not.
     * @return {@code this} for chaining.
     */
    public QueryEntity forceFillAbsentPKsWithDefaults(boolean forceFillAbsentPKsWithDefaults) {
        this.forceFillAbsentPKsWithDefaults = forceFillAbsentPKsWithDefaults;
        
        return this;
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        QueryEntityEx entity = (QueryEntityEx)o;

        return super.equals(entity) && F.eq(notNullFields, entity.notNullFields)
            && preserveKeysOrder == entity.preserveKeysOrder;
    }

    /** {@inheritDoc} */
    @Override public int hashCode() {
        int res = super.hashCode();

        res = 31 * res + (notNullFields != null ? notNullFields.hashCode() : 0);
        res = 31 * res + (preserveKeysOrder ? 1 : 0);

        return res;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(QueryEntityEx.class, this);
    }
}
