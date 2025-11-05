/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2010 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.db.pool;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.utilities.property.PropertyValue;
import is.codion.common.utilities.user.User;

import java.sql.Connection;

import static is.codion.common.utilities.Configuration.booleanValue;
import static is.codion.common.utilities.Configuration.integerValue;

/**
 * A connection pool wrapper, providing statistics from the underlying pool and allowing some configuration.
 */
public interface ConnectionPoolWrapper {

	/**
	 * Specifies the default maximum connection pool size.
	 * This determines the maximum number of concurrent database connections that can be created.
	 * Higher values allow more concurrent operations but consume more database resources.
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 8
	 * <li>Property name: codion.db.pool.maximumPoolSize
	 * <li>Valid range: 1-1000 (typically 5-50 for most applications)
	 * </ul>
	 */
	PropertyValue<Integer> MAXIMUM_POOL_SIZE = integerValue("codion.db.pool.maximumPoolSize", 8);

	/**
	 * Specifies the default minimum connection pool size.
	 * This determines the number of connections that remain open even when idle.
	 * Higher values reduce connection creation overhead but consume more database resources.
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 4
	 * <li>Property name: codion.db.pool.minimumPoolSize
	 * <li>Valid range: 0 to maximum pool size (typically 2-10 for most applications)
	 * </ul>
	 */
	PropertyValue<Integer> MINIMUM_POOL_SIZE = integerValue("codion.db.pool.minimumPoolSize", 4);

	/**
	 * Specifies the default idle timeout in milliseconds.
	 * This determines how long a connection can remain idle before being eligible for eviction.
	 * Lower values free up database resources faster but may cause more connection churn.
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 60.000 (1 minute)
	 * <li>Property name: codion.db.pool.idleTimeout
	 * <li>Valid range: 1000-3600000 (1 second to 1 hour, typically 30-300 seconds)
	 * </ul>
	 */
	PropertyValue<Integer> IDLE_TIMEOUT = integerValue("codion.db.pool.idleTimeout", 60_000);

	/**
	 * Specifies the default maximum connection check out timeout in milliseconds.
	 * This determines how long to wait for an available connection before throwing an exception.
	 * Lower values fail faster but may cause premature timeouts under load.
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 30.000 (30 seconds)
	 * <li>Property name: codion.db.pool.checkOutTimeout
	 * <li>Valid range: 1000-300000 (1 second to 5 minutes, typically 10-60 seconds)
	 * </ul>
	 */
	PropertyValue<Integer> CHECK_OUT_TIMEOUT = integerValue("codion.db.pool.checkOutTimeout", 30_000);

	/**
	 * Specifies whether connections should be validated when checked out from the pool.
	 * Enables additional safety by checking connection validity before use, at the cost of performance.
	 * Recommended for production environments with unreliable network connections.
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: false (disabled for performance)
	 * <li>Property name: codion.db.pool.validateConnectionsOnCheckout
	 * <li>Performance impact: Small overhead per connection checkout (~1-5ms)
	 * </ul>
	 */
	PropertyValue<Boolean> VALIDATE_CONNECTIONS_ON_CHECKOUT = booleanValue("codion.db.pool.validateConnectionsOnCheckout", false);

	/**
	 * Fetches a connection from the pool. Close the connection to return it to the pool.
	 * @param user the user credentials
	 * @return a database connection retrieved from the pool
	 * @throws DatabaseException in case of an exception while fetching the connection
	 * @throws IllegalStateException if the pool is closed
	 * @see #setMaximumCheckOutTime(int)
	 * @see Connection#close()
	 */
	Connection connection(User user);

	/**
	 * @return the user this connection pool is based on.
	 */
	User user();

	/**
	 * Closes this connection pool, connections subsequently checked in are disconnected
	 */
	void close();

	/**
	 * Retrieves usage statistics for the connection pool since time {@code since}.
	 * @param since the time from which statistics should be retrieved
	 * @return connection pool usage statistics
	 */
	ConnectionPoolStatistics statistics(long since);

	/**
	 * Resets the collected usage statistics
	 */
	void resetStatistics();

	/**
	 * Returns true if snapshot statistics are being collected.
	 * @return true if pool usage statistics for a snapshot should be collected
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
	 * Returns true if connection check out times are being collected.
	 * @return true if connection check out times should be collected
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
	 * Returns the pool cleanup interval in milliseconds.
	 * This determines how often the pool checks for and removes idle/expired connections.
	 * @return the pool cleanup interval in milliseconds
	 */
	int getCleanupInterval();

	/**
	 * Sets the pool cleanup interval in milliseconds.
	 * Controls how frequently the pool performs maintenance operations like removing expired connections.
	 * @param poolCleanupInterval the pool cleanup interval in milliseconds (typically 30000-300000)
	 * @throws IllegalArgumentException if the value is less than 1000
	 */
	void setCleanupInterval(int poolCleanupInterval);

	/**
	 * Returns the connection idle timeout in milliseconds.
	 * This is the time a connection can remain idle before being eligible for eviction.
	 * @return the pooled connection timeout in milliseconds
	 */
	int getIdleTimeout();

	/**
	 * Sets the connection idle timeout in milliseconds.
	 * Connections idle longer than this timeout may be closed to free up database resources.
	 * @param idleTimeout the pooled connection timeout in milliseconds (typically 30000-300000)
	 * @throws IllegalArgumentException if the value is less than 1000
	 */
	void setIdleTimeout(int idleTimeout);

	/**
	 * Returns the minimum number of connections to keep in the pool.
	 * These connections remain open even when idle to reduce connection creation overhead.
	 * @return the minimum number of connections to keep in the pool
	 */
	int getMinimumPoolSize();

	/**
	 * Sets the minimum number of connections to keep in the pool.
	 * Higher values reduce latency but consume more database resources.
	 * @param minimumPoolSize the minimum number of connections to keep in the pool (typically 1-10)
	 * @throws IllegalArgumentException if the value is less than 0 or larger than maximum pool size
	 */
	void setMinimumPoolSize(int minimumPoolSize);

	/**
	 * Returns the maximum number of connections this pool can create.
	 * This limits the total number of concurrent database connections to prevent resource exhaustion.
	 * @return the maximum number of connections this pool can create
	 */
	int getMaximumPoolSize();

	/**
	 * Sets the maximum number of connections to keep in this pool.
	 * Higher values support more concurrent operations but consume more database resources.
	 * Note that if the current number of connections exceeds this value when set, excess connections
	 * are not actively discarded.
	 * @param maximumPoolSize the maximum number of connections this pool can create (typically 5-50)
	 * @throws IllegalArgumentException if the value is less than 1 or less than minimum pool size
	 */
	void setMaximumPoolSize(int maximumPoolSize);

	/**
	 * Returns the maximum time to wait for a connection checkout in milliseconds.
	 * This prevents threads from waiting indefinitely when the pool is exhausted.
	 * @return the maximum number of milliseconds to retry connection checkout before throwing an exception
	 */
	int getMaximumCheckOutTime();

	/**
	 * Sets the maximum time to wait for a connection checkout in milliseconds.
	 * Lower values fail faster under load, higher values may cause application timeouts.
	 * Note that this also modifies the new connection threshold, keeping its value to 1/4 of this one.
	 * @param maximumCheckOutTime the maximum number of milliseconds to retry connection checkout (typically 10000-60000)
	 * @throws IllegalArgumentException if the value is less than 0
	 */
	void setMaximumCheckOutTime(int maximumCheckOutTime);
}
