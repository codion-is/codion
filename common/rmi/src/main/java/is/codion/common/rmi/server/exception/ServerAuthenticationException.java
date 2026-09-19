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
 * Copyright (c) 2019 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.rmi.server.exception;

import org.jspecify.annotations.Nullable;

import java.io.Serial;

import static java.util.Objects.requireNonNull;

/**
 * An exception indicating that a login has failed due to an authentication error,
 * invalid username or password.
 * <p>A login failure with a cause presents the message of its cause, as read, see {@link #getMessage()}.
 * A cause providing its message in the language of the reader thereby
 * provides a client with a message in its own language, not that of the server.
 */
public final class ServerAuthenticationException extends LoginException {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new {@link ServerAuthenticationException}
	 * @param message the exception message
	 */
	public ServerAuthenticationException(String message) {
		super(message);
	}

	/**
	 * Instantiates a new {@link ServerAuthenticationException}, presenting the message of its cause.
	 * Note that the cause travels to the client, along with its own causes, if any.
	 * @param cause the cause of the authentication failure
	 */
	public ServerAuthenticationException(Throwable cause) {
		super(requireNonNull(cause).getMessage());
		initCause(cause);
	}

	/**
	 * @return the message of the cause, if there is one and it has a message, otherwise the message of this exception
	 */
	@Override
	public @Nullable String getMessage() {
		Throwable cause = getCause();
		if (cause != null && cause.getMessage() != null) {
			return cause.getMessage();
		}

		return super.getMessage();
	}
}
