/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import org.jminor.common.User;
import org.jminor.common.db.Database;
import org.jminor.common.db.exception.DatabaseException;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * Provides connection pool implementations
 */
public interface ConnectionPoolProvider {

  /**
   * @param user the user to base the pooled connections on
   * @param database the underlying database
   * @return a connection pool based on the given user
   * @throws DatabaseException in case of an exception
   */
  ConnectionPool createConnectionPool(final User user, final Database database) throws DatabaseException;

  static ConnectionPoolProvider getConnectionPoolProvider(final String classname) {
    final ServiceLoader<ConnectionPoolProvider> loader = ServiceLoader.load(ConnectionPoolProvider.class);
    for (final ConnectionPoolProvider provider : loader) {
      if (provider.getClass().getName().equals(classname)) {
        return provider;
      }
    }

    throw new IllegalArgumentException("No connection pool provider of type: " + classname + " available");
  }

  static ConnectionPoolProvider getConnectionPoolProvider() {
    final ServiceLoader<ConnectionPoolProvider> loader = ServiceLoader.load(ConnectionPoolProvider.class);
    final Iterator<ConnectionPoolProvider> iterator = loader.iterator();
    if (iterator.hasNext()) {
      return iterator.next();
    }

    throw new IllegalStateException("No connection pool provider available");
  }
}
