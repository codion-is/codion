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

import java.util.Map;

/**
 * Exposes the server-side latency of a single operation type as a JMX MXBean, registered as
 * {@code is.codion:type=OperationLatency,operation=<operation>} on the platform MBean server when
 * {@link EntityServerConfiguration#JMX} is enabled. One MBean is registered per operation as it is
 * first encountered. This is the always-on duration histogram folded per operation type, distinct
 * from the per-client method traces which remain an on-demand RMI debugging tool.
 * <p>
 * The values are cumulative and map directly onto a Prometheus histogram: {@link #getCount()} is the
 * {@code +Inf} bucket, {@link #getBuckets()} provides the finite buckets and {@link #getSum()} the sum.
 */
public interface OperationLatencyMXBean {

	/**
	 * @return the total number of times the operation has been served since server startup
	 */
	long getCount();

	/**
	 * @return the sum of all observed durations, in seconds
	 */
	double getSum();

	/**
	 * The cumulative observation counts keyed by the bucket upper bound in seconds, in other words
	 * the number of observations at or below each bound, in Prometheus histogram layout. The final
	 * {@code +Inf} bucket equals {@link #getCount()}.
	 * @return the histogram buckets, keyed by upper bound in seconds (and {@code +Inf})
	 */
	Map<String, Long> getBuckets();
}
