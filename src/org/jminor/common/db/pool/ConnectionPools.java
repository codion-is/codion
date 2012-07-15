/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import org.jminor.common.db.DatabaseConnectionProvider;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.User;

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
   * Instantiates a new ConnectionPool and associates it with the given user
   * @param connectionProvider the connection provider
   * @return a new connection pool
   * @throws ClassNotFoundException in case the jdbc class is not found when constructing the initial connections
   * @throws DatabaseException in case of an exception while constructing the initial connections
   */
  public static ConnectionPool createPool(final DatabaseConnectionProvider connectionProvider) throws ClassNotFoundException, DatabaseException {
    final ConnectionPool connectionPool = new ConnectionPoolImpl(connectionProvider);
    CONNECTION_POOLS.put(connectionProvider.getUser(), connectionPool);

    return connectionPool;
  }

  /**
   * Closes and removes all available connection pools
   */
  public static void closeConnectionPools() {
    for (final ConnectionPool pool : new ArrayList<ConnectionPool>(ConnectionPools.getConnectionPools())) {
      removeConnectionPool(pool.getUser());
    }
  }

  /**
   * Closes and removes the pool associated with the given user
   * @param user the user whos pool should be removed
   */
  public static void removeConnectionPool(final User user) {
    if (containsConnectionPool(user)) {
      CONNECTION_POOLS.remove(user).close();
    }
  }

  /**
   * @param user the user
   * @return the connection pool for the given user, null if none exists
   * @see #containsConnectionPool(org.jminor.common.model.User)
   */
  public static ConnectionPool getConnectionPool(final User user) {
    return CONNECTION_POOLS.get(user);
  }

  /**
   * @param user user
   * @return true if a connection pool is available for the given user
   */
  public static boolean containsConnectionPool(final User user) {
    return CONNECTION_POOLS.containsKey(user);
  }

  /**
   * @return all available connection pools
   */
  public static Collection<ConnectionPool> getConnectionPools() {
    return CONNECTION_POOLS.values();
  }
}
