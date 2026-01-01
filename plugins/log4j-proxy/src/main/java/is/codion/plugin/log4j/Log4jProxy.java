/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2017 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.plugin.log4j;

import is.codion.common.utilities.logging.LoggerProxy;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * A Log4j LoggerProxy implementation
 */
public final class Log4jProxy implements LoggerProxy {

	@Override
	public Object getLogLevel(String logger) {
		LoggerContext context = (LoggerContext) LogManager.getContext(false);
		LoggerConfig loggerConfig = context.getConfiguration().getLoggerConfig(requireNonNull(logger));

		return loggerConfig.getLevel();
	}

	@Override
	public void setLogLevel(String logger, Object logLevel) {
		if (!(logLevel instanceof Level)) {
			throw new IllegalArgumentException("logLevel should be of type " + Level.class.getName());
		}
		LoggerContext context = (LoggerContext) LogManager.getContext(false);
		LoggerConfig loggerConfig = context.getConfiguration().getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
		loggerConfig.setLevel((Level) logLevel);
		context.updateLoggers();
	}

	@Override
	public List<Object> levels() {
		return asList(Level.OFF, Level.FATAL, Level.ERROR, Level.WARN, Level.INFO, Level.DEBUG, Level.TRACE, Level.ALL);
	}

	@Override
	public String rootLogger() {
		return LogManager.ROOT_LOGGER_NAME;
	}

	@Override
	public Collection<String> loggers() {
		return ((LoggerContext) LogManager.getContext(false)).getLoggers().stream()
						.map(Logger::getName)
						.collect(toList());
	}

	@Override
	public Collection<String> files() {
		Map<String, Appender> appenderMap = ((Logger) LogManager.getLogger()).getAppenders();

		return appenderMap.values().stream()
						.filter(RollingFileAppender.class::isInstance)
						.map(RollingFileAppender.class::cast)
						.map(RollingFileAppender::getFileName)
						.collect(toList());
	}
}
