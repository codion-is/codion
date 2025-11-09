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
 * Copyright (c) 2017 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.utilities.logging;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import static java.util.Collections.emptyList;

/**
 * A logging proxy facilitating the setting of log levels
 */
public interface LoggerProxy {

	/**
	 * The no-op logger proxy instance, zero functionality.
	 */
	LoggerProxy NONE = new NoOpLoggerProxy();

	/**
	 * @return the root log level
	 */
	Object getLogLevel();

	/**
	 * @param name the logger name
	 * @return the log level
	 */
	Object getLogLevel(String name);

	/**
	 * Sets the log level
	 * @param logLevel the log level
	 */
	void setLogLevel(Object logLevel);

	/**
	 * Sets the log level
	 * @param name the logger name
	 * @param logLevel the log level
	 */
	void setLogLevel(String name, Object logLevel);

	/**
	 * @return the available log levels
	 */
	List<Object> levels();

	/**
	 * @return all loggers
	 */
	Collection<String> loggers();

	/**
	 * @return the log file paths, if available
	 */
	default Collection<String> files() {
		return emptyList();
	}

	/**
	 * @return the first available LoggerProxy implementation found, {@link #NONE} if none is available.
	 */
	static LoggerProxy instance() {
		try {
			ServiceLoader<LoggerProxy> loader = ServiceLoader.load(LoggerProxy.class);
			Iterator<LoggerProxy> proxyIterator = loader.iterator();
			if (proxyIterator.hasNext()) {
				return proxyIterator.next();
			}

			System.err.println("No LoggerProxy service implementation found");
			return NONE;
		}
		catch (ServiceConfigurationError e) {
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException) {
				throw (RuntimeException) cause;
			}
			throw new RuntimeException(cause);
		}
	}
}
