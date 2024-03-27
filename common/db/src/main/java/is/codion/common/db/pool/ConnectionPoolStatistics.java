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
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.db.pool;

import java.util.List;

/**
 * An interface encapsulating database connection pool statistics
 */
public interface ConnectionPoolStatistics {

	/**
	 * @return the connection pool username
	 */
	String username();

	/**
	 * Returns a list containing a snapshot of connection pool states.
	 * @return a snapshot of pool states associated with this statistics object
	 */
	List<ConnectionPoolState> snapshot();

	/**
	 * @return the number of connections being managed by the pool
	 */
	int size();

	/**
	 * @return the number of available connections
	 */
	int available();

	/**
	 * @return the number of connections in use
	 */
	int inUse();

	/**
	 * @return the timestamp
	 */
	long timestamp();

	/**
	 * @return the time at which this statistics object was created
	 */
	long creationDate();

	/**
	 * @return the number of connections created by the pool
	 */
	int created();

	/**
	 * @return the number of idle connections destroyed by the pool
	 */
	int destroyed();

	/**
	 * @return the last time stats were reset
	 */
	long resetTime();

	/**
	 * @return the number of connection requests since last reset
	 */
	int requests();

	/**
	 * @return the number of connection requests per second
	 */
	int requestsPerSecond();

	/**
	 * @return the number of failed connection requests since last reset
	 */
	int failedRequests();

	/**
	 * @return the number of failed connection requests per second
	 */
	int failedRequestsPerSecond();

	/**
	 * @return the avarage check out time in ms
	 */
	long averageGetTime();

	/**
	 * @return the minimum check out time in ms
	 */
	long minimumCheckOutTime();

	/**
	 * @return the maximum check out time in ms
	 */
	long maximumCheckOutTime();
}