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

package org.apache.ignite.logger.slf4j;

import java.util.logging.Handler;
import org.apache.ignite.IgniteLogger;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import static org.apache.ignite.IgniteSystemProperties.IGNITE_QUIET;

/**
 * SLF4J-based implementation for logging. This logger should be used
 * by loaders that have prefer slf4j-based logging.
 * <p>
 * Here is an example of configuring SLF4J logger in Ignite configuration Spring file:
 * <pre name="code" class="xml">
 *      &lt;property name="gridLogger"&gt;
 *          &lt;bean class="org.apache.ignite.logger.slf4j.Slf4jLogger"/&gt;
 *      &lt;/property&gt;
 * </pre>
 * <p>
 * It's recommended to use Ignite's logger injection instead of using/instantiating
 * logger in your task/job code. See {@link org.apache.ignite.resources.LoggerResource} annotation about logger
 * injection.
 */
public class Slf4jLogger implements IgniteLogger {
    /** SLF4J implementation proxy. */
    private final Logger impl;

    /** Quiet flag. */
    private final boolean quiet;

    /**
     * Creates new logger.
     */
    public Slf4jLogger() {
        this(LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME));
    }

    /**
     * Creates new logger with given implementation.
     *
     * @param impl SLF4J implementation to use.
     */
    public Slf4jLogger(Logger impl) {
        assert impl != null;

        this.impl = impl;

        quiet = Boolean.parseBoolean(System.getProperty(IGNITE_QUIET, "true"));

        if (!SLF4JBridgeHandler.isInstalled()) {
            java.util.logging.Logger rootLog = getJulRootLogger();

            for (Handler handler : rootLog.getHandlers())
                if (handler instanceof java.util.logging.ConsoleHandler)
                    rootLog.removeHandler(handler);

            SLF4JBridgeHandler.install();
        }
    }

    /** {@inheritDoc} */
    @Override public Slf4jLogger getLogger(Object ctgr) {
        Logger impl = ctgr == null ? LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) :
            ctgr instanceof Class ? LoggerFactory.getLogger(((Class<?>)ctgr).getName()) :
                LoggerFactory.getLogger(ctgr.toString());

        return new Slf4jLogger(impl);
    }

    /** {@inheritDoc} */
    @Override public void trace(String msg) {
        trace(null, msg);
    }

    /** {@inheritDoc} */
    @Override public void trace(@Nullable String marker, String msg) {
        if (!impl.isTraceEnabled())
            warning("Logging at TRACE level without checking if TRACE level is enabled: " + msg);

        impl.trace(getMarkerOrNull(marker), msg);
    }

    /** {@inheritDoc} */
    @Override public void debug(String msg) {
        debug(null, msg);
    }

    /** {@inheritDoc} */
    @Override public void debug(@Nullable String marker, String msg) {
        if (!impl.isDebugEnabled())
            warning("Logging at DEBUG level without checking if DEBUG level is enabled: " + msg);

        impl.debug(getMarkerOrNull(marker), msg);
    }

    /** {@inheritDoc} */
    @Override public void info(String msg) {
        info(null, msg);
    }

    /** {@inheritDoc} */
    @Override public void info(@Nullable String marker, String msg) {
        if (!impl.isInfoEnabled())
            warning("Logging at INFO level without checking if INFO level is enabled: " + msg);

        impl.info(getMarkerOrNull(marker), msg);
    }

    /** {@inheritDoc} */
    @Override public void warning(String msg, @Nullable Throwable e) {
        warning(null, msg, e);
    }

    /** {@inheritDoc} */
    @Override public void warning(@Nullable String marker, String msg, @Nullable Throwable e) {
        impl.warn(getMarkerOrNull(marker), msg, e);
    }

    /** {@inheritDoc} */
    @Override public void error(String msg, @Nullable Throwable e) {
        error(null, msg, e);
    }

    /** {@inheritDoc} */
    @Override public void error(@Nullable String marker, String msg, @Nullable Throwable e) {
        impl.error(getMarkerOrNull(marker), msg, e);
    }

    /** Returns Marker object for the specified name, or null if the name is null */
    private Marker getMarkerOrNull(@Nullable String marker) {
        return marker != null ? MarkerFactory.getMarker(marker) : null;
    }

    /** {@inheritDoc} */
    @Override public boolean isTraceEnabled() {
        return impl.isTraceEnabled();
    }

    /** {@inheritDoc} */
    @Override public boolean isInfoEnabled() {
        return impl.isInfoEnabled();
    }

    /** {@inheritDoc} */
    @Override public boolean isDebugEnabled() {
        return impl.isDebugEnabled();
    }

    /** {@inheritDoc} */
    @Override public boolean isQuiet() {
        return quiet;
    }

    /** {@inheritDoc} */
    @Nullable @Override public String fileName() {
        return null;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(Slf4jLogger.class, this);
    }

    /**
     * Return the root logger instance.
     */
    private static java.util.logging.Logger getJulRootLogger() {
        return java.util.logging.LogManager.getLogManager().getLogger("");
    }
}
