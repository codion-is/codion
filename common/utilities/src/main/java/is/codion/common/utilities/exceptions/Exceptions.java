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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.utilities.exceptions;

import java.util.Collection;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * Exception handling utilities.
 */
public final class Exceptions {

	private Exceptions() {}

	/**
	 * <p>Returns the given {@link Throwable} in case it is a RuntimeException or else a RuntimeException wrapping it.
	 * <p>Note that if a new wrapper RuntimeException is created, it has the stacktrace of the original throwable,
	 * as well as its message, while keeping the original throwable as its cause.
	 * @param throwable the throwable
	 */
	public static RuntimeException runtime(Throwable throwable) {
		if (requireNonNull(throwable) instanceof RuntimeException) {
			return (RuntimeException) throwable;
		}

		RuntimeException exception = new RuntimeException(throwable.getMessage(), throwable);
		exception.setStackTrace(throwable.getStackTrace());

		return exception;
	}

	/**
	 * <p>Returns the given {@link Throwable} in case it is a RuntimeException or else a RuntimeException wrapping it.
	 * <p>Note that if a new wrapper RuntimeException is created, it has the stacktrace of the unwrapped throwable,
	 * as well as its message, while keeping the unwrapped throwable as its cause.
	 * @param throwable the throwable
	 * @param unwrap unwraps these exceptions
	 */
	public static RuntimeException runtime(Throwable throwable, Class<? extends Throwable>... unwrap) {
		return runtime(unwrap(throwable, asList(requireNonNull(unwrap))));
	}

	/**
	 * Unwraps the given throwable.
	 * @param throwable the exception to unwrap
	 * @param unwrap the exception types to unwrap
	 * @return the unwrapped exception
	 */
	public static Throwable unwrap(Throwable throwable, Collection<Class<? extends Throwable>> unwrap) {
		requireNonNull(throwable);
		requireNonNull(unwrap);
		Throwable cause = throwable;
		while (unwrap.contains(cause.getClass())) {
			Throwable parent = cause;
			cause = cause.getCause();
			if (cause == parent) {
				return parent;
			}
			if (cause == null) {
				return parent;
			}
		}

		return cause;
	}
}
