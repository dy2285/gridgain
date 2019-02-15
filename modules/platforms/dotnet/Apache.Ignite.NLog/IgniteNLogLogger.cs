﻿/*
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

namespace Apache.Ignite.NLog
{
    using System;
    using Apache.Ignite.Core.Impl.Common;
    using global::NLog;
    using ILogger = Apache.Ignite.Core.Log.ILogger;
    using IgniteLogLevel = Apache.Ignite.Core.Log.LogLevel;
    using NLogLogLevel = global::NLog.LogLevel;

    /// <summary>
    /// Ignite NLog integration.
    /// </summary>
    public class IgniteNLogLogger : ILogger
    {
        /// <summary>
        /// The NLog logger.
        /// </summary>
        private readonly Logger _logger;

        /// <summary>
        /// Initializes a new instance of the <see cref="IgniteNLogLogger"/> class using the
        /// <see cref="LogManager.GetCurrentClassLogger()"/> to retrieve the NLog logger.
        /// </summary>
        public IgniteNLogLogger() : this(LogManager.GetCurrentClassLogger())
        {
            // No-op.
        }

        /// <summary>
        /// Initializes a new instance of the <see cref="IgniteNLogLogger"/> class.
        /// </summary>
        /// <param name="logger">The NLog logger instance.</param>
        public IgniteNLogLogger(Logger logger)
        {
            IgniteArgumentCheck.NotNull(logger, "logger");

            _logger = logger;
        }

        /// <summary>
        /// Logs the specified message.
        /// </summary>
        /// <param name="level">The level.</param>
        /// <param name="message">The message.</param>
        /// <param name="args">The arguments to format <paramref name="message" />.
        /// Can be null (formatting will not occur).</param>
        /// <param name="formatProvider">The format provider. Can be null if <paramref name="args" /> is null.</param>
        /// <param name="category">The logging category name.</param>
        /// <param name="nativeErrorInfo">The native error information.</param>
        /// <param name="ex">The exception. Can be null.</param>
        /// <exception cref="System.NotImplementedException"></exception>
        public void Log(IgniteLogLevel level, string message, object[] args, IFormatProvider formatProvider, 
            string category, string nativeErrorInfo, Exception ex)
        {
            var logEvent = new LogEventInfo
            {
                Level = ConvertLogLevel(level),
                Message = message,
                FormatProvider = formatProvider,
                Parameters = args,
                Exception = ex,
                LoggerName = category
            };

            if (nativeErrorInfo != null)
                logEvent.Properties.Add("nativeErrorInfo", nativeErrorInfo);

            _logger.Log(logEvent);
        }

        /// <summary>
        /// Determines whether the specified log level is enabled.
        /// </summary>
        /// <param name="level">The level.</param>
        /// <returns>
        /// Value indicating whether the specified log level is enabled
        /// </returns>
        /// <exception cref="System.NotImplementedException"></exception>
        public bool IsEnabled(IgniteLogLevel level)
        {
            return _logger.IsEnabled(ConvertLogLevel(level));
        }

        /// <summary>
        /// Converts the Ignite LogLevel to the NLog log level.
        /// </summary>
        /// <param name="level">The Ignite log level.</param>
        /// <returns>Corresponding NLog log level.</returns>
        public static NLogLogLevel ConvertLogLevel(IgniteLogLevel level)
        {
            switch (level)
            {
                case IgniteLogLevel.Trace:
                    return NLogLogLevel.Trace;
                case IgniteLogLevel.Debug:
                    return NLogLogLevel.Debug;
                case IgniteLogLevel.Info:
                    return NLogLogLevel.Info;
                case IgniteLogLevel.Warn:
                    return NLogLogLevel.Warn;
                case IgniteLogLevel.Error:
                    return NLogLogLevel.Error;
                default:
                    throw new ArgumentOutOfRangeException("level", level, "Invalid Ignite LogLevel.");
            }
        }
    }
}
