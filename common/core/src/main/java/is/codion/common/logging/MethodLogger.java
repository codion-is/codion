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
package is.codion.common.logging;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

/**
 * A method call logger allowing logging of nested method calls.
 * @see #methodLogger(int)
 * @see #methodLogger(int, ArgumentToString)
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
	@Nullable Entry exit(String method);

	/**
	 * @param method the method being exited
	 * @param exception the exception, if any
	 * @return the Entry
	 */
	@Nullable Entry exit(String method, @Nullable Exception exception);

	/**
	 * @param method the method being exited
	 * @param exception the exception, if any
	 * @param exitMessage the message to associate with exiting the method
	 * @return the Entry, or null if this logger is not enabled
	 */
	@Nullable Entry exit(String method, @Nullable Exception exception, @Nullable String exitMessage);

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
	List<Entry> entries();

	/**
	 * Creates a new MethodLogger, disabled by default.
	 * @param maxSize the maximum log size
	 * @return a new MethodLogger instance
	 */
	static MethodLogger methodLogger(int maxSize) {
		return methodLogger(maxSize, new DefaultArgumentFormatter());
	}

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
	 * A method logger entry.
	 */
	interface Entry {

		/**
		 * @return the child log entries
		 */
		List<Entry> children();

		/**
		 * @return the name of the method logged by this entry
		 */
		String method();

		/**
		 * @return the entry message
		 */
		@Nullable String message();

		/**
		 * Returns the duration of the method call this entry represents in nanoseconds,
		 * this value is 0 or undefined until the exit time has been set.
		 * @return the duration of the method call this entry represents
		 */
		long duration();

		/**
		 * Appends this logger entry along with any child entries to the given StringBuilder.
		 * @param builder the StringBuilder to append to.
		 */
		void appendTo(StringBuilder builder);

		/**
		 * Returns a string representation of this log entry.
		 * @param indentation the number of tab indents to prefix the string with
		 * @return a string representation of this log entry
		 */
		String toString(int indentation);
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

	/**
	 * Provides String representations of method arguments.
	 */
	class DefaultArgumentFormatter implements ArgumentFormatter {

		private static final String BRACKET_OPEN = "[";
		private static final String BRACKET_CLOSE = "]";

		@Override
		public final String format(String methodName, @Nullable Object object) {
			return toString(methodName, object);
		}

		/**
		 * Returns a String representation of the given object.
		 * @param methodName the method name
		 * @param object the object
		 * @return a String representation of the given object
		 */
		protected String toString(String methodName, @Nullable Object object) {
			return toString(object);
		}

		/**
		 * Returns a String representation of the given object.
		 * @param object the object
		 * @return a String representation of the given object
		 */
		protected String toString(@Nullable Object object) {
			if (object == null) {
				return "";
			}
			if (object instanceof List) {
				return toString((List<?>) object);
			}
			if (object instanceof Collection) {
				return toString((Collection<?>) object);
			}
			if (object instanceof byte[]) {
				return "byte[" + ((byte[]) object).length + "]";
			}
			if (object.getClass().isArray()) {
				return toString((Object[]) object);
			}

			return object.toString();
		}

		private String toString(List<?> arguments) {
			if (arguments.isEmpty()) {
				return "";
			}
			if (arguments.size() == 1) {
				return toString(arguments.get(0));
			}

			return arguments.stream()
							.map(this::toString)
							.collect(joining(", ", BRACKET_OPEN, BRACKET_CLOSE));
		}

		private String toString(Collection<?> arguments) {
			if (arguments.isEmpty()) {
				return "";
			}

			return arguments.stream()
							.map(this::toString)
							.collect(joining(", ", BRACKET_OPEN, BRACKET_CLOSE));
		}

		private String toString(Object[] arguments) {
			if (arguments.length == 0) {
				return "";
			}
			if (arguments.length == 1) {
				return toString(arguments[0]);
			}

			return stream(arguments)
							.map(this::toString)
							.collect(joining(", ", BRACKET_OPEN, BRACKET_CLOSE));
		}
	}
}
