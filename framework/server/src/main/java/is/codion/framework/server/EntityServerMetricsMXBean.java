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
 * Exposes the server-wide Codion metrics as a JMX MXBean, registered as
 * {@code is.codion:type=EntityServer} on the platform MBean server when
 * {@link EntityServerConfiguration#JMX} is enabled. The JVM thread, garbage collection, CPU and
 * memory metrics are already available as platform MXBeans, so only the Codion-specific metrics
 * are exposed here.
 */
public interface EntityServerMetricsMXBean {

	/**
	 * The cumulative request count, a monotonically increasing counter suitable for
	 * deriving a request rate at the consumer (for example with the Prometheus {@code rate()} function).
	 * @return the number of requests served since server startup
	 */
	long getRequestCount();

	/**
	 * @return the current number of connected clients
	 */
	int getConnectionCount();

	/**
	 * @return the maximum number of concurrent connections, -1 if no limit is set
	 */
	int getConnectionLimit();
}
