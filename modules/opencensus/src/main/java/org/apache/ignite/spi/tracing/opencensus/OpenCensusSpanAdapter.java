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

package org.apache.ignite.spi.tracing.opencensus;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import io.opencensus.trace.Annotation;
import io.opencensus.trace.AttributeValue;
import org.apache.ignite.internal.processors.tracing.Scope;
import org.apache.ignite.internal.processors.tracing.Span;
import org.apache.ignite.internal.processors.tracing.SpanStatus;
import org.apache.ignite.internal.processors.tracing.SpanType;

/**
 * Span implementation based on OpenCensus library.
 */
public class OpenCensusSpanAdapter implements Span {
    /** OpenCensus span delegate. */
    private final io.opencensus.trace.Span span;

    /** Flag indicates that span is ended. */
    private volatile boolean ended;

    /** Span type. */
    private final SpanType spanType;

    /** Set of extra supported scopes for given span in addition to span's scope that is supported by default. */
    private final Set<Scope> supportedScopes;

    /**
     * @param span OpenCensus span delegate.
     * @param spanType Type of span to create.
     */
    public OpenCensusSpanAdapter(io.opencensus.trace.Span span, SpanType spanType) {
        this.span = span;
        this.spanType = spanType;
        supportedScopes = Collections.emptySet();
    }

    /**
     * @param span OpenCensus span delegate.
     * @param spanType Type of span to create.
     */
    public OpenCensusSpanAdapter(io.opencensus.trace.Span span, SpanType spanType, Set<Scope> supportedScopes) {
        this.span = span;
        this.spanType = spanType;
        this.supportedScopes = supportedScopes;
    }

    /** Implementation object. */
    public io.opencensus.trace.Span impl() {
        return span;
    }

    /** {@inheritDoc} */
    @Override public OpenCensusSpanAdapter addTag(String tagName, String tagVal) {
        tagVal = tagVal != null ? tagVal : "null";

        span.putAttribute(tagName, AttributeValue.stringAttributeValue(tagVal));

        return this;
    }

    /** {@inheritDoc} */
    @Override public Span addTag(String tagName, long tagVal) {
        span.putAttribute(tagName, AttributeValue.longAttributeValue(tagVal));

        return this;
    }

    /** {@inheritDoc} */
    @Override public OpenCensusSpanAdapter addLog(String logDesc) {
        span.addAnnotation(logDesc);

        return this;
    }

    /** {@inheritDoc} */
    @Override public OpenCensusSpanAdapter addLog(String logDesc, Map<String, String> attrs) {
        span.addAnnotation(Annotation.fromDescriptionAndAttributes(
            logDesc,
            attrs.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> AttributeValue.stringAttributeValue(e.getValue())
                ))
        ));

        return this;
    }

    /** {@inheritDoc} */
    @Override public OpenCensusSpanAdapter setStatus(SpanStatus spanStatus) {
        span.setStatus(StatusMatchTable.match(spanStatus));

        return this;
    }

    /** {@inheritDoc} */
    @Override public OpenCensusSpanAdapter end() {
        try {
            // TODO: https://ggsystems.atlassian.net/browse/GG-22503
            // This sleep hack is needed to consider span as sampled.
            // @see io.opencensus.implcore.trace.export.InProcessSampledSpanStoreImpl.Bucket.considerForSampling
            // Meaningful only for tracing tests.
            Thread.sleep(10);
        }
        catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        span.end();

        ended = true;

        return this;
    }

    /** {@inheritDoc} */
    @Override public boolean isEnded() {
        return ended;
    }

    /** {@inheritDoc} */
    @Override public SpanType type() {
        return spanType;
    }

    /** {@inheritDoc} */
    @Override public Set<Scope> supportedScopes() {
        return supportedScopes;
    }
}
