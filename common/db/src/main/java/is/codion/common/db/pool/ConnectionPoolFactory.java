/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.pool;

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
   * Creates a connection pool based on the given database and user.
   * @param connectionFactory the connection factory
   * @param user the user to base the pooled connections on
   * @return a connection pool based on the given user
   * @throws DatabaseException in case of an exception
   */
  ConnectionPool createConnectionPool(ConnectionFactory connectionFactory, User user) throws DatabaseException;

  /**
   * Returns the {@link ConnectionPoolFactory} implementation found by the {@link ServiceLoader}
   * of the given type.
   * @param classname the classname of the required connection pool provider
   * @return a {@link ConnectionPoolFactory} implementation of the given type from the {@link ServiceLoader}.
   * @throws IllegalStateException in case no such {@link ConnectionPoolFactory} implementation is available.
   */
  static ConnectionPoolFactory connectionPoolFactory(final String classname) {
    requireNonNull(classname, "classname");
    final ServiceLoader<ConnectionPoolFactory> loader = ServiceLoader.load(ConnectionPoolFactory.class);
    for (final ConnectionPoolFactory factory : loader) {
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
  static ConnectionPoolFactory connectionPoolFactory() {
    final ServiceLoader<ConnectionPoolFactory> loader = ServiceLoader.load(ConnectionPoolFactory.class);
    final Iterator<ConnectionPoolFactory> iterator = loader.iterator();
    if (iterator.hasNext()) {
      return iterator.next();
    }

    throw new IllegalStateException("No connection pool factory available");
  }
}
