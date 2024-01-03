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
package is.codion.common.db.database;

import is.codion.common.db.pool.ConnectionPoolWrapper;
import is.codion.common.user.User;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import static java.util.Objects.requireNonNull;

/**
 * Responsible for providing JDBC {@link Connection} instances. Usually this means a new connection,
 * but in some cases, for example when wrapping existing connections, an existing connection may be returned.
 * Note that when used in conjunction with a {@link ConnectionPoolWrapper} a new connection must be returned.
 */
public interface ConnectionProvider {

  /**
   * Returns a JDBC {@link Connection} instance based on the given database and user.
   * @param user the user
   * @param url the jdbc url
   * @return a JDBC {@link Connection} instance
   * @throws SQLException in case of an exception
   * @throws NullPointerException in case user or url is null
   */
  default Connection connection(User user, String url) throws SQLException {
    Properties connectionProperties = new Properties();
    connectionProperties.put(Database.USER_PROPERTY, requireNonNull(user, "user").username());
    connectionProperties.put(Database.PASSWORD_PROPERTY, String.valueOf(user.password()));

    return DriverManager.getConnection(requireNonNull(url, "url"), connectionProperties);
  }
}
