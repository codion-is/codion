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
 * Copyright (c) 2019 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.rmi.server.exception;

import is.codion.common.resources.MessageBundle;

import static is.codion.common.resources.MessageBundle.messageBundle;
import static java.util.ResourceBundle.getBundle;

/**
 * An exception indicating that the server is not accepting new connections
 */
public final class ConnectionNotAvailableException extends ServerException {

	private static final MessageBundle MESSAGES =
					messageBundle(ConnectionNotAvailableException.class, getBundle(ConnectionNotAvailableException.class.getName()));

	/**
	 * Instantiates a new {@link ConnectionNotAvailableException}
	 */
	public ConnectionNotAvailableException() {
		super(MESSAGES.getString("connection_not_available"));
	}
}
