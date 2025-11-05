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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.utilities.logging;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A method trace.
 */
public interface MethodTrace {

	/**
	 * @return the name of the method being traced
	 */
	String method();

	/**
	 * @return the trace message
	 */
	@Nullable String message();

	/**
	 * Returns the duration of the method call this entry represents in nanoseconds,
	 * this value is 0 or undefined until the trace has been completed.
	 * @return the duration of the method call this trace represents
	 */
	long duration();

	/**
	 * @return the child method traces
	 */
	List<MethodTrace> children();

	/**
	 * Appends this method trace along with any children to the given StringBuilder.
	 * @param builder the StringBuilder to append to.
	 */
	void appendTo(StringBuilder builder);

	/**
	 * Returns a string representation of this method trace.
	 * @param indentation the number of tab indents to prefix the string with
	 * @return a string representation of this method trace
	 */
	String toString(int indentation);

	/**
	 * @param trace the child trace to add
	 * @throws IllegalStateException in case this trace is already completed
	 */
	void addChild(MethodTrace trace);

	/**
	 * Completes this method trace.
	 * @param exception the exception, if any
	 * @param message the exit message, if any
	 * @throws IllegalStateException in case this trace is completed
	 */
	void complete(@Nullable Exception exception, @Nullable String message);

	/**
	 * @param method the method to trace
	 * @param message the message, if any
	 * @return a new {@link MethodTrace} instance
	 */
	static MethodTrace methodTrace(String method, @Nullable String message) {
		return new DefaultMethodTrace(method, message);
	}
}
