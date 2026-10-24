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
package is.codion.common.utilities.logging;

import is.codion.common.utilities.exceptions.Exceptions;

import org.jspecify.annotations.Nullable;

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
	 * Returns the level set for the given logger itself, null where it has none, the logger then inheriting
	 * the level of its nearest ancestor with one. The {@link #root()} logger always has a level.
	 * @param logger the logger name
	 * @return the level set for the given logger, null if none is set
	 */
	@Nullable Object level(String logger);

	/**
	 * Sets the level of the given logger
	 * @param logger the logger name
	 * @param level the level, one of {@link #levels()}
	 * @throws IllegalArgumentException in case the level is not of the type of {@link #levels()}
	 */
	void level(String logger, Object level);

	/**
	 * @return the available log levels, ordered from the most to the least verbose, OFF last
	 */
	List<Object> levels();

	/**
	 * @return the name of the root logger
	 */
	String root();

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
			throw Exceptions.runtime(e, ServiceConfigurationError.class);
		}
	}
}
