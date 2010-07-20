/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import org.jminor.common.model.User;

import java.sql.SQLException;

/**
 * Defines a simple connection pool.<br>
 * User: Bjorn Darri<br>
 * Date: 28.3.2010<br>
 * Time: 13:06:20<br>
 */
public interface ConnectionPool {

  /**
   * Return the given connection to the pool.
   * @param dbConnection the database connection to return to the pool
   */
  void checkInConnection(final PoolableConnection dbConnection);

  /**
   * Fetches a connection from the pool.
   * @return a database connection retrieved from the pool
   * @throws ClassNotFoundException in case the JDBC driver class is not found
   * @throws SQLException in case of a database exception
   */
  PoolableConnection checkOutConnection() throws ClassNotFoundException, SQLException;

  /**
   * @return the user this connection pool is based on.
   */
  User getUser();

  /**
   * Retrives usage statistics for the connection pool since time <code>since</code>.
   * @param since the time from which statistics should be retrieved
   * @return connection pool usage statistics
   */
  ConnectionPoolStatistics getConnectionPoolStatistics(final long since);

  /**
   * Resets the collected usage statistics
   */
  void resetPoolStatistics();

  /**
   * @return true if fine grained pool usage statistics should be collected.
   */
  boolean isCollectFineGrainedStatistics();

  /**
   * Specifies whether or not fine grained usage statistics should be collected.
   * @param value the value
   */
  void setCollectFineGrainedStatistics(final boolean value);

  void setEnabled(final boolean enabled);

  boolean isEnabled();

  void setPoolCleanupInterval(final int poolCleanupInterval);

  int getPooledConnectionTimeout();

  void setPooledConnectionTimeout(final int timeout);

  int getPoolCleanupInterval();

  int getMinimumPoolSize();

  void setMinimumPoolSize(final int value);

  int getMaximumPoolSize();

  void setMaximumPoolSize(final int value);
}
