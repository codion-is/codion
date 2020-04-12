/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import org.jminor.common.db.Database;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.user.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * A factory class for providing and managing ConnectionPool instances
 */
public final class ConnectionPools {

  /**
   * The available connection pools mapped to their respective usernames.
   */
  private static final Map<String, ConnectionPool> CONNECTION_POOLS = Collections.synchronizedMap(new HashMap<>());

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
    requireNonNull(connectionPoolProvider, "connectionPoolProvider");
    requireNonNull(database, "database");
    requireNonNull(users, "users");
    for (final User user : users) {
      CONNECTION_POOLS.put(user.getUsername(), connectionPoolProvider.createConnectionPool(user, database));
    }
  }

  /**
   * Closes and removes all available connection pools
   */
  public static synchronized void closeConnectionPools() {
    for (final ConnectionPool pool : getConnectionPools()) {
      removeConnectionPool(pool.getUser().getUsername());
    }
  }

  /**
   * Closes and removes the pool associated with the given user
   * @param username the username of the pool that should be removed
   */
  public static synchronized void removeConnectionPool(final String username) {
    if (containsConnectionPool(username)) {
      CONNECTION_POOLS.remove(username).close();
    }
  }

  /**
   * @param username the username
   * @return the connection pool for the given user, null if none exists
   * @see #containsConnectionPool(String)
   */
  public static synchronized ConnectionPool getConnectionPool(final String username) {
    return CONNECTION_POOLS.get(username);
  }

  /**
   * @param username the username
   * @return true if a connection pool is available for the given user
   */
  public static synchronized boolean containsConnectionPool(final String username) {
    return CONNECTION_POOLS.containsKey(username);
  }

  /**
   * @return all available connection pools
   */
  public static synchronized Collection<ConnectionPool> getConnectionPools() {
    return new ArrayList<>(CONNECTION_POOLS.values());
  }
}