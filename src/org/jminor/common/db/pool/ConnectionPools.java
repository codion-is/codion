/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import org.jminor.common.db.PoolableConnectionProvider;

/**
 * A factory class providing ConnectionPool instances
 */
public final class ConnectionPools {

  private ConnectionPools() {}

  /**
   * Instantiates a new ConnectionPool.
   * @param connectionProvider the connection provider
   * @return a new connection pool
   */
  public static ConnectionPool createPool(final PoolableConnectionProvider connectionProvider) {
    return new ConnectionPoolImpl(connectionProvider);
  }
}
