/*
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.pool;

import is.codion.common.Configuration;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.property.PropertyValue;
import is.codion.common.user.User;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * A connection pool wrapper, providing statistics from the underlying pool and allowing some configuration.
 */
public interface ConnectionPoolWrapper {

  /**
   * Specifies the default maximum connection pool size.
   * Value type: Integer<br>
   * Default value: 8
   */
  PropertyValue<Integer> DEFAULT_MAXIMUM_POOL_SIZE = Configuration.integerValue("codion.db.pool.defaultMaximumPoolSize", 8);

  /**
   * Specifies the default minimum connection pool size.
   * Value type: Integer<br>
   * Default value: 4
   */
  PropertyValue<Integer> DEFAULT_MINIMUM_POOL_SIZE = Configuration.integerValue("codion.db.pool.defaultMinimumPoolSize", 4);

  /**
   * Specifies the default idle timeout in milliseconds.
   * Value type: Integer<br>
   * Default value: 60.000
   */
  PropertyValue<Integer> DEFAULT_IDLE_TIMEOUT = Configuration.integerValue("codion.db.pool.defaultIdleTimeout", 60000);

  /**
   * Fetches a connection from the pool. Close the connection to return it to the pool.
   * @param user the user credentials
   * @return a database connection retrieved from the pool
   * @throws DatabaseException in case of an exception while fetching the connection
   * @throws IllegalStateException if the pool is closed
   * @see #setMaximumCheckOutTime(int)
   * @see Connection#close()
   */
  Connection connection(User user) throws DatabaseException;

  /**
   * @return the user this connection pool is based on.
   */
  User user();

  /**
   * @return the DataSource used by this connection pool
   */
  DataSource poolDataSource();

  /**
   * Closes this connection pool, connections subsequently checked in are disconnected
   */
  void close();

  /**
   * Retrives usage statistics for the connection pool since time {@code since}.
   * @param since the time from which statistics should be retrieved
   * @return connection pool usage statistics
   */
  ConnectionPoolStatistics statistics(long since);

  /**
   * Resets the collected usage statistics
   */
  void resetStatistics();

  /**
   * @return true if pool usage statistics for a snapshot should be collected.
   * @see #statistics(long)
   * @see ConnectionPoolStatistics#snapshot()
   */
  boolean isCollectSnapshotStatistics();

  /**
   * Specifies whether to collect usage statistics for a snapshot.
   * @param collectSnapshotStatistics the value
   * @see #statistics(long)
   * @see ConnectionPoolStatistics#snapshot()
   */
  void setCollectSnapshotStatistics(boolean collectSnapshotStatistics);

  /**
   * @return true if connection check out times should be collected.
   * @see #statistics(long)
   */
  boolean isCollectCheckOutTimes();

  /**
   * Specifies whether to collect connection check out times.
   * @param collectCheckOutTimes the value
   * @see #statistics(long)
   */
  void setCollectCheckOutTimes(boolean collectCheckOutTimes);

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
  int getIdleConnectionTimeout();

  /**
   * @param idleConnectionTimeout the pooled connection timeout in milliseconds, that is, the time that needs
   * to pass before an idle connection can be harvested
   */
  void setIdleConnectionTimeout(int idleConnectionTimeout);

  /**
   * @return the minimum number of connections to keep in the pool
   */
  int getMinimumPoolSize();

  /**
   * @param minimumPoolSize the minimum number of connections to keep in the pool
   * @throws IllegalArgumentException if the value is less than 0 or larger than maximum pool size
   */
  void setMinimumPoolSize(int minimumPoolSize);

  /**
   * @return the maximum number of connections this pool can create
   */
  int getMaximumPoolSize();

  /**
   * Sets the maximum number of connections to keep in this pool.
   * Note that if the current number of connections exceeds this value when set, excess connections
   * are not actively discarded.
   * @param maximumPoolSize the maximum number of connections this pool can create
   * @throws IllegalArgumentException if the value is less than 1 or less than minimum pool size
   */
  void setMaximumPoolSize(int maximumPoolSize);

  /**
   * @return the maximum number of milliseconds to retry connection checkout before throwing an exception
   */
  int getMaximumCheckOutTime();

  /**
   * @param maximumCheckOutTime the maximum number of milliseconds to retry connection checkout before throwing an exception,
   * note that this also modifies the new connection threshold, keeping its value to 1/4 of this one
   * @throws IllegalArgumentException if the value is less than 0
   */
  void setMaximumCheckOutTime(int maximumCheckOutTime);
}
