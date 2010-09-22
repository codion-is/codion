/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import org.jminor.common.model.User;

/**
 * A factory class providing ConnectionPool instances
 */
public final class ConnectionPools {

  /**
   * Instantiates a new ConnectionPoolImpl.
   * @param connectionProvider the connection provider
   * @param user the user this pool is based on
   * @return a new connection pool
   */
  public static ConnectionPool createPool(final PoolableConnectionProvider connectionProvider, final User user) {
    return new ConnectionPoolImpl(connectionProvider, user);
  }
}
