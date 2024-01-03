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
 * Copyright (c) 2013 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.db.pool;

import is.codion.common.db.connection.ConnectionFactory;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;

import java.util.Iterator;
import java.util.ServiceLoader;

import static java.util.Objects.requireNonNull;

/**
 * Provides connection pool implementations
 */
public interface ConnectionPoolFactory {

  /**
   * Creates a connection pool wrapper based on the given database and user.
   * @param connectionFactory the connection factory
   * @param user the user to base the pooled connections on
   * @return a connection pool wrapper based on the given user
   * @throws DatabaseException in case of an exception
   */
  ConnectionPoolWrapper createConnectionPool(ConnectionFactory connectionFactory, User user) throws DatabaseException;

  /**
   * Returns the {@link ConnectionPoolFactory} implementation found by the {@link ServiceLoader}
   * of the given type.
   * @param classname the classname of the required connection pool provider
   * @return a {@link ConnectionPoolFactory} implementation of the given type from the {@link ServiceLoader}.
   * @throws IllegalStateException in case no such {@link ConnectionPoolFactory} implementation is available.
   */
  static ConnectionPoolFactory instance(String classname) {
    requireNonNull(classname, "classname");
    ServiceLoader<ConnectionPoolFactory> loader = ServiceLoader.load(ConnectionPoolFactory.class);
    for (ConnectionPoolFactory factory : loader) {
      if (factory.getClass().getName().equals(classname)) {
        return factory;
      }
    }

    throw new IllegalStateException("No connection pool factory of type: " + classname + " available");
  }

  /**
   * Returns the first {@link ConnectionPoolFactory} implementation found by the {@link ServiceLoader}.
   * @return a {@link ConnectionPoolFactory} implementation from the {@link ServiceLoader}.
   * @throws IllegalStateException in case no {@link ConnectionPoolFactory} implementation is available.
   */
  static ConnectionPoolFactory instance() {
    ServiceLoader<ConnectionPoolFactory> loader = ServiceLoader.load(ConnectionPoolFactory.class);
    Iterator<ConnectionPoolFactory> iterator = loader.iterator();
    if (iterator.hasNext()) {
      return iterator.next();
    }

    throw new IllegalStateException("No connection pool factory available");
  }
}
