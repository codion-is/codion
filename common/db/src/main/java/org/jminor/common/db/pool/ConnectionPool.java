/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import org.jminor.common.Configuration;
import org.jminor.common.db.Database;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.user.User;
import org.jminor.common.value.PropertyValue;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * A connection pool wrapper, providing statistics from the underlying pool and allowing some configuration.
 */
public interface ConnectionPool {

  /**
   * Specifies the default maximum connection pool size.
   * Value type: Integer<br>
   * Default value: 8
   */
  PropertyValue<Integer> DEFAULT_MAXIMUM_POOL_SIZE = Configuration.integerValue("jminor.db.pool.defaultMaximumPoolSize", 8);

  /**
   * Specifies the default minimum connection pool size.
   * Value type: Integer<br>
   * Default value: 4
   */
  PropertyValue<Integer> DEFAULT_MINIMUM_POOL_SIZE = Configuration.integerValue("jminor.db.pool.defaultMinimumPoolSize", 4);

  /**
   * Specifies the default idle timeout in milliseconds.
   * Value type: Integer<br>
   * Default value: 60.000
   */
  PropertyValue<Integer> DEFAULT_IDLE_TIMEOUT = Configuration.integerValue("jminor.db.pool.defaultIdleTimeout", 60000);

  /**
   * Fetches a connection from the pool. Close the connection to return it to the pool.
   * @return a database connection retrieved from the pool
   * @throws ConnectionPoolException.NoConnectionAvailable in case the maximum check out time is exceeded
   * @throws DatabaseException in case of a database exception
   * @throws IllegalStateException if the pool is closed
   * @see #setMaximumCheckOutTime(int)
   * @see Connection#close()
   */
  Connection getConnection() throws DatabaseException;

  /**
   * @return the underlying database
   */
  Database getDatabase();

  /**
   * @return the user this connection pool is based on.
   */
  User getUser();

  /**
   * @return the DataSource used by this connection pool
   */
  DataSource getPoolDataSource();

  /**
   * Closes this connection pool, connections subsequently checked in are disconnected
   */
  void close();

  /**
   * Retrives usage statistics for the connection pool since time {@code since}.
   * @param since the time from which statistics should be retrieved
   * @return connection pool usage statistics
   */
  ConnectionPoolStatistics getStatistics(long since);

  /**
   * Resets the collected usage statistics
   */
  void resetStatistics();

  /**
   * @return true if pool usage statistics for a snapshot should be collected.
   * @see #getStatistics(long)
   * @see ConnectionPoolStatistics#getSnapshot()
   */
  boolean isCollectSnapshotStatistics();

  /**
   * Specifies whether to collect usage statistics for a snapshot.
   * @param collectSnapshotStatistics the value
   * @see #getStatistics(long)
   * @see ConnectionPoolStatistics#getSnapshot()
   */
  void setCollectSnapshotStatistics(boolean collectSnapshotStatistics);

  /**
   * @return the pool cleanup interval in milliseconds
   */
  int getCleanupInterval();

  /**
   * @param poolCleanupInterval the pool cleanup interval in milliseconds
   */
  void setCleanupInterval(int poolCleanupInterval);

  /**
   * @return the pooled connection timeout in milliseconds, that is, the time that needs
   * to pass before an idle connection can be harvested
   */
  int getConnectionTimeout();

  /**
   * @param timeout the pooled connection timeout in milliseconds, that is, the time that needs
   * to pass before an idle connection can be harvested
   */
  void setConnectionTimeout(int timeout);

  /**
   * @return the minimum number of connections to keep in the pool
   */
  int getMinimumPoolSize();

  /**
   * @param value the minimum number of connections to keep in the pool
   * @throws IllegalArgumentException if value is less than 0 or larger than maximum pool size
   */
  void setMinimumPoolSize(int value);

  /**
   * @return the maximum number of connections this pool can create
   */
  int getMaximumPoolSize();

  /**
   * Sets the maximum number of connections to keep in this pool.
   * Note that if the current number of connections exceeds this value when set, excess connections
   * are not actively discarded.
   * @param value the maximum number of connections this pool can create
   * @throws IllegalArgumentException if value is less than 1 or less than minimum pool size
   */
  void setMaximumPoolSize(int value);

  /**
   * @return the maximum number of milliseconds to retry connection checkout before throwing an exception
   * @see ConnectionPoolException.NoConnectionAvailable
   */
  int getMaximumCheckOutTime();

  /**
   * @param value the maximum number of milliseconds to retry connection checkout before throwing an exception,
   * note that this also modifies the new connection threshold, keeping its value to 1/4 of this one
   * @throws IllegalArgumentException if value is less than 0
   */
  void setMaximumCheckOutTime(int value);
}
