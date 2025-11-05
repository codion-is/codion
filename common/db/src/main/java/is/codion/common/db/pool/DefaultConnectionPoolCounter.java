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
 * Copyright (c) 2013 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.db.pool;

import is.codion.common.utilities.scheduler.TaskScheduler;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

/**
 * A connection pool statistics collector.
 */
final class DefaultConnectionPoolCounter {

	private static final double THOUSAND = 1000d;
	private static final int SNAPSHOT_STATS_SIZE = 1000;
	private static final int SNAPSHOT_COLLECTION_INTERVAL_MS = 10;
	private static final int CHECK_OUT_TIMES_MAX_SIZE = 10000;

	private final AbstractConnectionPoolWrapper<?> connectionPool;
	private final LinkedList<Integer> checkOutTimes = new LinkedList<>();
	private final LinkedList<DefaultConnectionPoolState> snapshotStatistics = new LinkedList<>();
	private volatile boolean collectSnapshotStatistics = false;
	private volatile boolean collectCheckOutTimes = false;
	private final TaskScheduler snapshotStatisticsCollector = TaskScheduler.builder()
					.task(new StatisticsCollector())
					.interval(SNAPSHOT_COLLECTION_INTERVAL_MS, TimeUnit.MILLISECONDS)
					.name("Connection pool counter")
					.build();

	private final AtomicLong resetDate = new AtomicLong(System.currentTimeMillis());
	private final AtomicLong requestsPerSecondTime = new AtomicLong(resetDate.get());
	private final AtomicInteger connectionsCreated = new AtomicInteger();
	private final AtomicInteger connectionsDestroyed = new AtomicInteger();
	private final AtomicInteger connectionRequests = new AtomicInteger();
	private final AtomicInteger requestsPerSecondCounter = new AtomicInteger();
	private final AtomicInteger connectionRequestsFailed = new AtomicInteger();
	private final AtomicInteger requestsFailedPerSecondCounter = new AtomicInteger();

	DefaultConnectionPoolCounter(AbstractConnectionPoolWrapper<?> connectionPool) {
		this.connectionPool = connectionPool;
	}

	boolean isCollectSnapshotStatistics() {
		return collectSnapshotStatistics;
	}

	void setCollectSnapshotStatistics(boolean collectSnapshotStatistics) {
		synchronized (snapshotStatistics) {
			if (collectSnapshotStatistics) {
				IntStream.range(0, SNAPSHOT_STATS_SIZE).forEach(i -> snapshotStatistics.add(new DefaultConnectionPoolState()));
				snapshotStatisticsCollector.start();
			}
			else {
				snapshotStatisticsCollector.stop();
				snapshotStatistics.clear();
			}
			this.collectSnapshotStatistics = collectSnapshotStatistics;
		}
	}

	boolean isCollectCheckOutTimes() {
		return collectCheckOutTimes;
	}

	void setCollectCheckOutTimes(boolean collectCheckOutTimes) {
		synchronized (checkOutTimes) {
			if (!collectCheckOutTimes) {
				checkOutTimes.clear();
			}
			this.collectCheckOutTimes = collectCheckOutTimes;
		}
	}

	void addCheckOutTime(int time) {
		if (collectCheckOutTimes) {
			synchronized (checkOutTimes) {
				checkOutTimes.add(time);
				if (checkOutTimes.size() > CHECK_OUT_TIMES_MAX_SIZE) {
					checkOutTimes.removeFirst();
				}
			}
		}
	}

	void incrementConnectionsDestroyedCounter() {
		connectionsDestroyed.incrementAndGet();
	}

	void incrementConnectionsCreatedCounter() {
		connectionsCreated.incrementAndGet();
	}

	void incrementFailedRequestCounter() {
		connectionRequestsFailed.incrementAndGet();
		requestsFailedPerSecondCounter.incrementAndGet();
	}

	void incrementRequestCounter() {
		connectionRequests.incrementAndGet();
		requestsPerSecondCounter.incrementAndGet();
	}

	void resetStatistics() {
		connectionsCreated.set(0);
		connectionsDestroyed.set(0);
		connectionRequests.set(0);
		connectionRequestsFailed.set(0);
		synchronized (checkOutTimes) {
			checkOutTimes.clear();
		}
		resetDate.set(System.currentTimeMillis());
	}

	ConnectionPoolStatistics collectStatistics(long since) {
		DefaultConnectionPoolStatistics statistics = new DefaultConnectionPoolStatistics();
		long current = System.currentTimeMillis();
		statistics.timestamp(current);
		statistics.resetTime(resetDate.get());
		statistics.available(connectionPool.available());
		statistics.inUse(connectionPool.inUse());
		statistics.created(connectionsCreated.get());
		statistics.destroyed(connectionsDestroyed.get());
		statistics.requests(connectionRequests.get());
		statistics.failedRequests(connectionRequestsFailed.get());
		double seconds = (current - requestsPerSecondTime.get()) / THOUSAND;
		requestsPerSecondTime.set(current);
		statistics.requestsPerSecond((int) (requestsPerSecondCounter.get() / seconds));
		requestsPerSecondCounter.set(0);
		statistics.failedRequestsPerSecond((int) (requestsFailedPerSecondCounter.get() / seconds));
		requestsFailedPerSecondCounter.set(0);
		if (!checkOutTimes.isEmpty()) {
			populateCheckOutTime(statistics);
		}
		if (collectSnapshotStatistics && since >= 0) {
			synchronized (snapshotStatistics) {
				statistics.snapshot(snapshotStatistics.stream()
								.filter(state -> state.timestamp() >= since)
								.collect(toList()));
			}
		}

		return statistics;
	}

	private void populateCheckOutTime(DefaultConnectionPoolStatistics statistics) {
		int total = 0;
		int min = -1;
		int max = -1;
		synchronized (checkOutTimes) {
			for (Integer time : checkOutTimes) {
				total += time;
				if (min == -1) {
					min = time;
					max = time;
				}
				else {
					min = Math.min(min, time);
					max = Math.max(max, time);
				}
			}
			statistics.averageTime(total / checkOutTimes.size());
			statistics.minimumTime(min);
			statistics.maximumTime(max);
			checkOutTimes.clear();
		}
	}

	/**
	 * Cleans up resources associated with statistics collection.
	 * Should be called when the connection pool is closed to prevent resource leaks.
	 */
	void close() {
		snapshotStatisticsCollector.stop();
		synchronized (snapshotStatistics) {
			snapshotStatistics.clear();
		}
		synchronized (checkOutTimes) {
			checkOutTimes.clear();
		}
	}

	private final class StatisticsCollector implements Runnable {

		/**
		 * Adds the current state of the pool to the snapshot connection pool log
		 */
		@Override
		public void run() {
			synchronized (snapshotStatistics) {
				snapshotStatistics.addLast(connectionPool.updateState(snapshotStatistics.removeFirst()));
			}
		}
	}
}
