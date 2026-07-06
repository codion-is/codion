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

import is.codion.common.utilities.scheduler.TaskScheduler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.System.currentTimeMillis;

/**
 * The single in-server source of the server-wide operation metrics, shared by all remote connections:
 * the cumulative request count, the current request rate and the per-operation latency. Both the
 * {@link is.codion.common.rmi.server.ServerAdmin} RMI feed and, when enabled, the JMX MBeans project
 * from this one model, so nothing is computed twice.
 */
final class ServerMetrics {

	static final ServerMetrics INSTANCE = new ServerMetrics();

	private static final int REQUESTS_PER_SECOND_UPDATE_INTERVAL = 2_500;
	private static final double THOUSAND = 1_000d;

	private final AtomicLong requestCount = new AtomicLong();
	private final AtomicLong requestsPerSecondTime = new AtomicLong(currentTimeMillis());
	private final AtomicInteger requestsPerSecond = new AtomicInteger();
	private final AtomicInteger requestsPerSecondCounter = new AtomicInteger();
	private final Map<String, OperationLatency> latencies = new ConcurrentHashMap<>();

	private ServerMetrics() {
		TaskScheduler.builder()
						.task(this::updateRequestsPerSecond)
						.interval(REQUESTS_PER_SECOND_UPDATE_INTERVAL, TimeUnit.MILLISECONDS)
						.name("ServerMetrics request counter")
						.start();
	}

	/**
	 * Records a completed operation.
	 * @param operation the operation name
	 * @param nanoseconds the server-side duration in nanoseconds
	 */
	void record(String operation, long nanoseconds) {
		requestCount.incrementAndGet();
		requestsPerSecondCounter.incrementAndGet();
		latencies.computeIfAbsent(operation, name -> new OperationLatency()).record(nanoseconds);
	}

	/**
	 * @return the cumulative number of requests served since server startup
	 */
	long requestCount() {
		return requestCount.get();
	}

	/**
	 * @return the number of requests served per second
	 */
	int requestsPerSecond() {
		return requestsPerSecond.get();
	}

	/**
	 * @return the per-operation latency histograms, keyed by operation name
	 */
	Map<String, OperationLatency> latencies() {
		return latencies;
	}

	private void updateRequestsPerSecond() {
		long current = currentTimeMillis();
		double seconds = (current - requestsPerSecondTime.getAndSet(current)) / THOUSAND;
		if (seconds > 0) {
			requestsPerSecond.set((int) (requestsPerSecondCounter.getAndSet(0) / seconds));
		}
	}
}
