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
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.db.connection;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;

import java.sql.Connection;

/**
 * Provides new {@link Connection} instances.
 */
public interface ConnectionFactory {

	/**
	 * Returns the jdbc database url for this connection factory.
	 * @return the jdbc database url for this connection factory.
	 */
	String url();

	/**
	 * Creates a connection for the given user.
	 * @param user the user for which to create a connection
	 * @return a new JDBC connection
	 * @throws DatabaseException in case of a connection error
	 * @throws is.codion.common.db.exception.AuthenticationException in case of an authentication error
	 */
	Connection createConnection(User user) throws DatabaseException;

	/**
	 * Validates the given connection.
	 * @param connection the connection to validate
	 * @return true if the connection is valid
	 */
	boolean connectionValid(Connection connection);
}
