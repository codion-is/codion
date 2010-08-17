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
   * If the pool has been closed the connection is disconnected and discarded.
   * @param dbConnection the database connection to return to the pool
   */
  void checkInConnection(final PoolableConnection dbConnection);

  /**
   * Fetches a connection from the pool.
   * @return a database connection retrieved from the pool
   * @throws ClassNotFoundException in case the JDBC driver class is not found
   * @throws SQLException in case of a database exception
   * @throws IllegalStateException if the pool is closed
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
   * @see #getConnectionPoolStatistics(long)
   * @see ConnectionPoolStatistics#getFineGrainedStatistics()
   */
  boolean isCollectFineGrainedStatistics();

  /**
   * Specifies whether or not fine grained usage statistics should be collected.
   * @param value the value
   * @see #getConnectionPoolStatistics(long)
   * @see ConnectionPoolStatistics#getFineGrainedStatistics()
   */
  void setCollectFineGrainedStatistics(final boolean value);

  /**
   * @param enabled true to enable this pool, false to disable
   */
  void setEnabled(final boolean enabled);

  /**
   * @return true if this pool is enabled, false otherwise
   */
  boolean isEnabled();

  /**
   * @param poolCleanupInterval the pool cleanup interval in milliseconds
   */
  void setPoolCleanupInterval(final int poolCleanupInterval);

  /**
   * @return the connection timeout in milliseconds
   */
  int getPooledConnectionTimeout();

  /**
   * @param timeout the connection timeout in milliseconds
   */
  void setPooledConnectionTimeout(final int timeout);

  /**
   * @return the maximum number of milliseconds the pool waits between checkout retries
   */
  int getMaximumRetryWaitPeriod();

  /**
   * @param maximumRetryWaitPeriod the maximum number of milliseconds the pool waits between checkout retries
   */
  void setMaximumRetryWaitPeriod(final int maximumRetryWaitPeriod);

  /**
   * @return the pool cleanup interval in milliseconds
   */
  int getPoolCleanupInterval();

  /**
   * @return the minimum number of connections to keep in the pool
   */
  int getMinimumPoolSize();

  /**
   * @param value the minimum number of connections to keep in the pool
   * @throws IllegalArgumentException if value is less than 0 or larger than maximum pool size
   */
  void setMinimumPoolSize(final int value);

  /**
   * @return the maximum number of connections this pool can create
   */
  int getMaximumPoolSize();

  /**
   * @param value the maximum number of connections this pool can create
   * @throws IllegalArgumentException if value is less than 1 or less than minimum pool size
   */
  void setMaximumPoolSize(final int value);
}
