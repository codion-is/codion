/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import org.jminor.common.db.DatabaseConnection;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.User;

/**
 * Defines a simple connection pool.
 */
public interface ConnectionPool {

  /**
   * Fetches a connection from the pool.
   * @return a database connection retrieved from the pool
   * @throws ConnectionPoolException.NoConnectionAvailable in case the maximum check out time is exceeded
   * @throws ClassNotFoundException in case the JDBC driver class is not found
   * @throws DatabaseException in case of a database exception
   * @throws IllegalStateException if the pool is closed
   * @see #setMaximumCheckOutTime(int)
   */
  DatabaseConnection getConnection() throws ClassNotFoundException, DatabaseException;

  /**
   * Return the given connection to the pool.
   * If the pool has been closed the connection is disconnected and discarded.
   * @param connection the database connection to return to the pool
   */
  void returnConnection(final DatabaseConnection connection);

  /**
   * @return the user this connection pool is based on.
   */
  User getUser();

  /**
   * Closes this connection pool, connections subsequently checked in are disconnected
   */
  void close();

  /**
   * Retrives usage statistics for the connection pool since time <code>since</code>.
   * @param since the time from which statistics should be retrieved
   * @return connection pool usage statistics
   */
  ConnectionPoolStatistics getStatistics(final long since);

  /**
   * Resets the collected usage statistics
   */
  void resetStatistics();

  /**
   * @return true if fine grained pool usage statistics should be collected.
   * @see #getStatistics(long)
   * @see ConnectionPoolStatistics#getFineGrainedStatistics()
   */
  boolean isCollectFineGrainedStatistics();

  /**
   * Specifies whether or not fine grained usage statistics should be collected.
   * @param value the value
   * @see #getStatistics(long)
   * @see ConnectionPoolStatistics#getFineGrainedStatistics()
   */
  void setCollectFineGrainedStatistics(final boolean value);

  /**
   * @return true if this pool is enabled, false otherwise
   */
  boolean isEnabled();

  /**
   * @param enabled true to enable this pool, false to disable
   */
  void setEnabled(final boolean enabled);

  /**
   * @return the pool cleanup interval in milliseconds
   */
  int getCleanupInterval();

  /**
   * @param poolCleanupInterval the pool cleanup interval in milliseconds
   */
  void setCleanupInterval(final int poolCleanupInterval);

  /**
   * @return the connection timeout in milliseconds
   */
  int getConnectionTimeout();

  /**
   * @param timeout the connection timeout in milliseconds
   */
  void setConnectionTimeout(final int timeout);

  /**
   * @return the maximum number of milliseconds the pool waits between checkout retries
   */
  int getMaximumRetryWaitPeriod();

  /**
   * @param maximumRetryWaitPeriod the maximum number of milliseconds the pool waits between checkout retries
   */
  void setMaximumRetryWaitPeriod(final int maximumRetryWaitPeriod);

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

  /**
   * @return the maximum number of milliseconds to retry connection checkout before throwing an exception
   * @see org.jminor.common.db.pool.ConnectionPoolException.NoConnectionAvailable
   */
  int getMaximumCheckOutTime();

  /**
   * @param value the maximum number of milliseconds to retry connection checkout before throwing an exception
   * @throws IllegalArgumentException if value is less than 0
   */
  void setMaximumCheckOutTime(final int value);

  /**
   * @return the time to wait before a new connection is created
   */
  int getNewConnectionThreshold();

  /**
   * @param value the time to wait before creating a new connection in ms
   * @throws IllegalArgumentException in case value is negative or larger than <code>maximumCheckOutTime</code>
   */
  void setNewConnectionThreshold(final int value);
}
