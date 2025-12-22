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
	 * Returns the given {@link Throwable} in case it is a RuntimeException or else a RuntimeException wrapping it.
	 * @param throwable the throwable
	 * @param unwrap unwraps these exceptions
	 */
	public static RuntimeException runtime(Throwable throwable, Class<? extends Throwable>... unwrap) {
		requireNonNull(throwable);
		Throwable cause = unwrap(throwable, asList(requireNonNull(unwrap)));
		if (cause instanceof RuntimeException) {
			return (RuntimeException) cause;
		}

		RuntimeException exception = new RuntimeException(cause.getMessage(), cause);
		exception.setStackTrace(cause.getStackTrace());

		return exception;
	}

	/**
	 * Unwraps the given throwable.
	 * @param throwable the exception to unwrap
	 * @param unwrap the exception types to unwrap
	 * @return the unwrapped exception
	 */
	public static Throwable unwrap(Throwable throwable, Collection<Class<? extends Throwable>> unwrap) {
		requireNonNull(unwrap);
		Throwable cause = requireNonNull(throwable);
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
