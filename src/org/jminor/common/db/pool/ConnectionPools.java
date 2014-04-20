/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import org.jminor.common.db.Database;
import org.jminor.common.db.DatabaseConnectionProvider;
import org.jminor.common.db.DatabaseConnections;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.framework.Configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A factory class for providing and managing ConnectionPool instances
 */
public final class ConnectionPools {

  /**
   * The available connection pools
   */
  private static final Map<User, ConnectionPool> CONNECTION_POOLS = Collections.synchronizedMap(new HashMap<User, ConnectionPool>());

  private ConnectionPools() {}

  /**
   * Initializes connection pools for the given users
   * @param database the underlying database
   * @param users the users to initialize connection pools for
   * @param validityCheckTimeout the number of seconds specified when checking if a connection is valid
   * @throws DatabaseException in case of a database exception
   * @throws ClassNotFoundException in case the specified connection pool provider class in not on the classpath
   * @see Configuration#SERVER_CONNECTION_POOL_PROVIDER_CLASS
   */
  public static synchronized void initializeConnectionPools(final Database database, final Collection<User> users,
                                                            final int validityCheckTimeout) throws DatabaseException, ClassNotFoundException {
    Util.rejectNullValue(database, "database");
    Util.rejectNullValue(users, "users");
    for (final User user : users) {
      final ConnectionPoolProvider poolProvider = initializeConnectionPoolProvider(database, user, validityCheckTimeout);
      CONNECTION_POOLS.put(user, poolProvider.createConnectionPool(user, database));
    }
  }

  /**
   * Instantiates a new ConnectionPool and associates it with the given user
   * @param connectionProvider the connection provider
   * @return a new connection pool
   * @throws DatabaseException in case of an exception while constructing the initial connections
   */
  public static synchronized ConnectionPool createDefaultConnectionPool(final DatabaseConnectionProvider connectionProvider) throws DatabaseException {
    final ConnectionPool connectionPool = new DefaultConnectionPool(connectionProvider);
    CONNECTION_POOLS.put(connectionProvider.getUser(), connectionPool);

    return connectionPool;
  }

  /**
   * Closes and removes all available connection pools
   */
  public static synchronized void closeConnectionPools() {
    for (final ConnectionPool pool : getConnectionPools()) {
      removeConnectionPool(pool.getUser());
    }
  }

  /**
   * Closes and removes the pool associated with the given user
   * @param user the user whos pool should be removed
   */
  public static synchronized void removeConnectionPool(final User user) {
    if (containsConnectionPool(user)) {
      CONNECTION_POOLS.remove(user).close();
    }
  }

  /**
   * @param user the user
   * @return the connection pool for the given user, null if none exists
   * @see #containsConnectionPool(org.jminor.common.model.User)
   */
  public static synchronized ConnectionPool getConnectionPool(final User user) {
    return CONNECTION_POOLS.get(user);
  }

  /**
   * @param user user
   * @return true if a connection pool is available for the given user
   */
  public static synchronized boolean containsConnectionPool(final User user) {
    return CONNECTION_POOLS.containsKey(user);
  }

  /**
   * @return all available connection pools
   */
  public static synchronized Collection<ConnectionPool> getConnectionPools() {
    return new ArrayList<>(CONNECTION_POOLS.values());
  }

  private static ConnectionPoolProvider initializeConnectionPoolProvider(final Database database, final User user,
                                                                         final int validityCheckTimeout) throws ClassNotFoundException {
    final String connectionPoolProviderClassName = Configuration.getStringValue(Configuration.SERVER_CONNECTION_POOL_PROVIDER_CLASS);
    if (!Util.nullOrEmpty(connectionPoolProviderClassName)) {
      final Class<?> providerClass = Class.forName(connectionPoolProviderClassName);
      try {
        return (ConnectionPoolProvider) providerClass.getConstructor().newInstance();
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    return createDefaultConnectionPoolProvider(DatabaseConnections.connectionProvider(database, user, validityCheckTimeout));
  }

  /**
   * Creates a connection pool based on the the default internal implementation
   * @param connectionProvider the connection provider
   * @return a default connection pool provider
   */
  private static ConnectionPoolProvider createDefaultConnectionPoolProvider(final DatabaseConnectionProvider connectionProvider) {
    return new ConnectionPoolProvider() {
      @Override
      public ConnectionPool createConnectionPool(final User user, final Database database) throws DatabaseException {
        return new DefaultConnectionPool(connectionProvider);
      }
    };
  }
}