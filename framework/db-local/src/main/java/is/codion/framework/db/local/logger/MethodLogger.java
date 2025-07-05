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
 * Copyright (c) 2020 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.local.logger;

import is.codion.common.logging.MethodTrace;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A method call logger allowing logging of nested method calls.
 * This is an internal class not for general usage.
 * @see #methodLogger(int)
 */
public interface MethodLogger {

	/**
	 * @param method the method being entered
	 */
	void enter(String method);

	/**
	 * @param method the method being entered
	 * @param argument the method argument, can be an Object, a collection or an array
	 */
	void enter(String method, @Nullable Object argument);

	/**
	 * @param method the method being exited
	 * @return the Entry
	 */
	@Nullable MethodTrace exit(String method);

	/**
	 * @param method the method being exited
	 * @param exception the exception, if any
	 * @return the Entry
	 */
	@Nullable MethodTrace exit(String method, @Nullable Exception exception);

	/**
	 * @param method the method being exited
	 * @param exception the exception, if any
	 * @param exitMessage the message to associate with exiting the method
	 * @return the Entry, or null if this logger is not enabled
	 */
	@Nullable MethodTrace exit(String method, @Nullable Exception exception, @Nullable String exitMessage);

	/**
	 * @return true if this logger is enabled
	 */
	boolean isEnabled();

	/**
	 * @param enabled true to enable this logger
	 */
	void setEnabled(boolean enabled);

	/**
	 * @return an unmodifiable view of the log entries
	 */
	List<MethodTrace> entries();

	/**
	 * Creates a new MethodLogger, disabled by default.
	 * @param maxSize the maximum log size
	 * @param formatter responsible for providing String representations of method arguments
	 * @return a new MethodLogger instance
	 */
	static MethodLogger methodLogger(int maxSize, ArgumentFormatter formatter) {
		return new DefaultMethodLogger(maxSize, formatter);
	}

	/**
	 * @return a no-op logger, always disabled
	 */
	static MethodLogger noOpLogger() {
		return DefaultMethodLogger.NO_OP;
	}

	/**
	 * Marks a class as having an internal method logger
	 */
	interface Loggable {

		/**
		 * @param methodLogger the MethodLogger to use, null to disable method logging
		 */
		void methodLogger(@Nullable MethodLogger methodLogger);
	}

	/**
	 * Provides String representations of method arguments for log display.
	 */
	interface ArgumentFormatter {

		/**
		 * @param methodName the method name
		 * @param argument the argument
		 * @return a String representation of the argument
		 */
		String format(String methodName, @Nullable Object argument);
	}
}
