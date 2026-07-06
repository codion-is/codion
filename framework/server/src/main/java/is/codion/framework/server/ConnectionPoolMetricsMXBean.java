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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.server;

/**
 * Exposes a single connection pool's metrics as a JMX MXBean, registered as
 * {@code is.codion:type=ConnectionPool,username=<username>} on the platform MBean server when
 * {@link EntityServerConfiguration#JMX} is enabled.
 * <p>
 * The counters ({@link #getRequests()}, {@link #getFailedRequests()}, {@link #getCreated()} and
 * {@link #getDestroyed()}) are cumulative but are reset by
 * {@link EntityServerAdmin#resetConnectionPoolStatistics(String)}, so a consumer deriving rates
 * must tolerate counter resets (the Prometheus {@code rate()} function does).
 */
public interface ConnectionPoolMetricsMXBean {

	/**
	 * @return the number of connections in the pool ({@link #getInUse()} + {@link #getAvailable()})
	 */
	int getSize();

	/**
	 * @return the number of available connections
	 */
	int getAvailable();

	/**
	 * @return the number of connections currently in use
	 */
	int getInUse();

	/**
	 * @return the cumulative number of connection requests since the last reset
	 */
	int getRequests();

	/**
	 * @return the cumulative number of failed connection requests since the last reset
	 */
	int getFailedRequests();

	/**
	 * @return the cumulative number of connections created since the last reset
	 */
	int getCreated();

	/**
	 * @return the cumulative number of connections destroyed since the last reset
	 */
	int getDestroyed();

	/**
	 * The average connection check-out time in microseconds, 0 unless check-out time collection is
	 * enabled via {@link EntityServerAdmin#setCollectPoolCheckOutTimes(String, boolean)}.
	 * @return the average check-out time in microseconds
	 */
	long getAverageCheckOutTime();
}
