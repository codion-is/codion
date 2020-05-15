/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
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
public interface ConnectionPoolProvider {

  /**
   * Creates a connection pool based on the given database and user.
   * @param connectionFactory the connection factory
   * @param user the user to base the pooled connections on
   * @return a connection pool based on the given user
   * @throws DatabaseException in case of an exception
   */
  ConnectionPool createConnectionPool(ConnectionFactory connectionFactory, User user) throws DatabaseException;

  /**
   * Returns the {@link ConnectionPoolProvider} implementation found by the {@link ServiceLoader}
   * of the given type.
   * @param classname the classname of the required connection pool provider
   * @return a {@link ConnectionPoolProvider} implementation of the given type from the {@link ServiceLoader}.
   * @throws IllegalStateException in case no such {@link ConnectionPoolProvider} implementation is available.
   */
  static ConnectionPoolProvider getConnectionPoolProvider(final String classname) {
    requireNonNull(classname, "classname");
    final ServiceLoader<ConnectionPoolProvider> loader = ServiceLoader.load(ConnectionPoolProvider.class);
    for (final ConnectionPoolProvider provider : loader) {
      if (provider.getClass().getName().equals(classname)) {
        return provider;
      }
    }

    throw new IllegalStateException("No connection pool provider of type: " + classname + " available");
  }

  /**
   * Returns the first {@link ConnectionPoolProvider} implementation found by the {@link ServiceLoader}.
   * @return a {@link ConnectionPoolProvider} implementation from the {@link ServiceLoader}.
   * @throws IllegalStateException in case no {@link ConnectionPoolProvider} implementation is available.
   */
  static ConnectionPoolProvider getConnectionPoolProvider() {
    final ServiceLoader<ConnectionPoolProvider> loader = ServiceLoader.load(ConnectionPoolProvider.class);
    final Iterator<ConnectionPoolProvider> iterator = loader.iterator();
    if (iterator.hasNext()) {
      return iterator.next();
    }

    throw new IllegalStateException("No connection pool provider available");
  }
}
