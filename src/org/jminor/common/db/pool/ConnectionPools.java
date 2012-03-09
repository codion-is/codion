/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import org.jminor.common.db.DatabaseConnectionProvider;
import org.jminor.common.db.exception.DatabaseException;

/**
 * A factory class providing ConnectionPool instances
 */
public final class ConnectionPools {

  private ConnectionPools() {}

  /**
   * Instantiates a new ConnectionPool.
   * @param connectionProvider the connection provider
   * @return a new connection pool
   * @throws ClassNotFoundException in case the jdbc class is not found when constructing the initial connections
   * @throws DatabaseException in case of an exception while constructing the initial connections
   */
  public static ConnectionPool createPool(final DatabaseConnectionProvider connectionProvider) throws ClassNotFoundException, DatabaseException {
    return new ConnectionPoolImpl(connectionProvider);
  }
}
