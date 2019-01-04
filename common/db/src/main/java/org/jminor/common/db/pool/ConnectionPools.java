/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import org.jminor.common.User;
import org.jminor.common.db.Database;
import org.jminor.common.db.DatabaseConnectionProvider;
import org.jminor.common.db.exception.DatabaseException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A factory class for providing and managing ConnectionPool instances
 */
public final class ConnectionPools {

  /**
   * The available connection pools
   */
  private static final Map<User, ConnectionPool> CONNECTION_POOLS = Collections.synchronizedMap(new HashMap<>());

  private ConnectionPools() {}

  /**
   * Initializes connection pools for the given users
   * @param connectionPoolProvider the ConnectionPoolProvider implementation to use
   * @param database the underlying database
   * @param users the users to initialize connection pools for
   * @throws DatabaseException in case of a database exception
   */
  public static synchronized void initializeConnectionPools(final ConnectionPoolProvider connectionPoolProvider,
                                                            final Database database, final Collection<User> users) throws DatabaseException {
    Objects.requireNonNull(connectionPoolProvider, "connectionPoolProvider");
    Objects.requireNonNull(database, "database");
    Objects.requireNonNull(users, "users");
    for (final User user : users) {
      CONNECTION_POOLS.put(user, connectionPoolProvider.createConnectionPool(user, database));
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
   * @see #containsConnectionPool(User)
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
}