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
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractOutputStreamAppender;
import org.apache.logging.log4j.core.appender.FileManager;
import org.apache.logging.log4j.core.appender.MemoryMappedFileManager;
import org.apache.logging.log4j.core.appender.OutputStreamManager;
import org.apache.logging.log4j.core.appender.RandomAccessFileManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * A Log4j LoggerProxy implementation
 */
public final class Log4jProxy implements LoggerProxy {

	@Override
	public @Nullable Object level(String logger) {
		LoggerContext context = (LoggerContext) LogManager.getContext(false);
		LoggerConfig loggerConfig = context.getConfiguration().getLoggerConfig(requireNonNull(logger));
		if (logger.equals(LogManager.ROOT_LOGGER_NAME)) {
			return loggerConfig.getLevel();
		}

		// the config found may be that of an ancestor
		return loggerConfig.getName().equals(logger) ? loggerConfig.getExplicitLevel() : null;
	}

	@Override
	public void level(String logger, Object level) {
		if (!(level instanceof Level)) {
			throw new IllegalArgumentException("level should be of type " + Level.class.getName());
		}
		Configurator.setLevel(requireNonNull(logger), (Level) level);
	}

	@Override
	public List<Object> levels() {
		return unmodifiableList(asList(Level.ALL, Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR, Level.FATAL, Level.OFF));
	}

	@Override
	public String root() {
		return LogManager.ROOT_LOGGER_NAME;
	}

	@Override
	public Collection<String> loggers() {
		return unmodifiableList(((LoggerContext) LogManager.getContext(false)).getLoggers().stream()
						.map(Logger::getName)
						.collect(toList()));
	}

	@Override
	public Collection<String> files() {
		LoggerContext context = (LoggerContext) LogManager.getContext(false);

		return unmodifiableList(context.getConfiguration().getAppenders().values().stream()
						.filter(AbstractOutputStreamAppender.class::isInstance)
						.map(appender -> ((AbstractOutputStreamAppender<?>) appender).getManager())
						.map(Log4jProxy::fileName)
						.filter(Objects::nonNull)
						.collect(toList()));
	}

	private static @Nullable String fileName(OutputStreamManager manager) {
		if (manager instanceof FileManager) {
			return ((FileManager) manager).getFileName();
		}
		if (manager instanceof RandomAccessFileManager) {
			return ((RandomAccessFileManager) manager).getFileName();
		}
		if (manager instanceof MemoryMappedFileManager) {
			return ((MemoryMappedFileManager) manager).getFileName();
		}

		return null;
	}
}
