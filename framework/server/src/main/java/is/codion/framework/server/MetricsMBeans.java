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

import is.codion.common.db.database.Database;
import is.codion.common.db.pool.ConnectionPoolStatistics;
import is.codion.common.db.pool.ConnectionPoolWrapper;
import is.codion.common.utilities.scheduler.TaskScheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.lang.System.currentTimeMillis;

/**
 * Registers and unregisters the Codion metric MXBeans on the platform MBean server. The MBeans are
 * thin projections of {@link ServerMetrics} and the connection pool counters, the same sources the
 * {@link is.codion.common.rmi.server.ServerAdmin} RMI feed reads from.
 * @see EntityServerConfiguration#JMX
 */
final class MetricsMBeans {

	private static final Logger LOG = LoggerFactory.getLogger(MetricsMBeans.class);

	private static final String ENTITY_SERVER = "is.codion:type=EntityServer";
	private static final String CONNECTION_POOL = "is.codion:type=ConnectionPool,username=";
	private static final String OPERATION_LATENCY = "is.codion:type=OperationLatency,operation=";

	/**
	 * The connection pool statistics are cached for this many milliseconds so that reading all the
	 * attributes of a pool MBean during a single scrape triggers a single {@link ConnectionPoolWrapper#statistics(long)}
	 * call, which shares its per-second counters and check-out time buffer with the RMI admin feed.
	 */
	private static final long STATISTICS_CACHE = 1_000;

	/**
	 * Operation types are discovered as they are first served, so a scheduled task registers an MBean
	 * for each newly encountered operation at this interval, in seconds.
	 */
	private static final int OPERATION_RECONCILE_INTERVAL = 2;

	private final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
	private final List<ObjectName> registered = new ArrayList<>();
	private final TaskScheduler operationReconciler = TaskScheduler.builder()
					.task(this::registerOperations)
					.interval(OPERATION_RECONCILE_INTERVAL, TimeUnit.SECONDS)
					.name("Metric MBean operation reconciler")
					.build();

	private boolean closed = false;

	private MetricsMBeans() {}

	static MetricsMBeans register(EntityServer server) {
		MetricsMBeans mBeans = new MetricsMBeans();
		try {
			mBeans.registerServer(server);
			mBeans.registerOperations();
			mBeans.operationReconciler.start();
			LOG.info("Metric MBeans registered on the platform MBean server");

			return mBeans;
		}
		catch (Exception e) {
			mBeans.unregister();
			throw new RuntimeException("Unable to register metric MBeans", e);
		}
	}

	synchronized void unregister() {
		closed = true;
		operationReconciler.stop();
		for (ObjectName objectName : registered) {
			try {
				mBeanServer.unregisterMBean(objectName);
			}
			catch (Exception e) {
				LOG.error("Unable to unregister MBean: {}", objectName, e);
			}
		}
		registered.clear();
	}

	private void registerServer(EntityServer server) throws Exception {
		register(new ObjectName(ENTITY_SERVER), new EntityServerMetrics(server), EntityServerMetricsMXBean.class);
		Database database = server.database();
		for (String username : database.connectionPoolUsernames()) {
			register(new ObjectName(CONNECTION_POOL + username),
							new ConnectionPoolMetrics(database.connectionPool(username)), ConnectionPoolMetricsMXBean.class);
		}
	}

	synchronized void registerOperations() {
		if (closed) {
			return;
		}
		try {
			for (Map.Entry<String, OperationLatency> operation : ServerMetrics.INSTANCE.latencies().entrySet()) {
				ObjectName objectName = new ObjectName(OPERATION_LATENCY + operation.getKey());
				if (!registered.contains(objectName)) {
					register(objectName, new OperationLatencyMetrics(operation.getValue()), OperationLatencyMXBean.class);
				}
			}
		}
		catch (Exception e) {
			LOG.error("Unable to register operation latency MBeans", e);
		}
	}

	private <T> void register(ObjectName objectName, T implementation, Class<T> mBeanInterface) throws Exception {
		mBeanServer.registerMBean(new StandardMBean(implementation, mBeanInterface, true), objectName);
		registered.add(objectName);
	}

	private static final class EntityServerMetrics implements EntityServerMetricsMXBean {

		private final EntityServer server;

		private EntityServerMetrics(EntityServer server) {
			this.server = server;
		}

		@Override
		public long getRequestCount() {
			return ServerMetrics.INSTANCE.requestCount();
		}

		@Override
		public int getConnectionCount() {
			return server.connectionCount();
		}

		@Override
		public int getConnectionLimit() {
			return server.getConnectionLimit();
		}
	}

	private static final class ConnectionPoolMetrics implements ConnectionPoolMetricsMXBean {

		private final ConnectionPoolWrapper connectionPool;

		private ConnectionPoolStatistics statistics;
		private long statisticsTime;

		private ConnectionPoolMetrics(ConnectionPoolWrapper connectionPool) {
			this.connectionPool = connectionPool;
		}

		@Override
		public int getSize() {
			return statistics().size();
		}

		@Override
		public int getAvailable() {
			return statistics().available();
		}

		@Override
		public int getInUse() {
			return statistics().inUse();
		}

		@Override
		public int getRequests() {
			return statistics().requests();
		}

		@Override
		public int getFailedRequests() {
			return statistics().failedRequests();
		}

		@Override
		public int getCreated() {
			return statistics().created();
		}

		@Override
		public int getDestroyed() {
			return statistics().destroyed();
		}

		@Override
		public long getAverageCheckOutTime() {
			return statistics().averageTime();
		}

		private synchronized ConnectionPoolStatistics statistics() {
			long now = currentTimeMillis();
			if (statistics == null || now - statisticsTime > STATISTICS_CACHE) {
				statistics = connectionPool.statistics(-1);
				statisticsTime = now;
			}

			return statistics;
		}
	}

	private static final class OperationLatencyMetrics implements OperationLatencyMXBean {

		private static final double NANOSECONDS_IN_SECOND = 1_000_000_000d;
		private static final double MILLISECONDS_IN_SECOND = 1_000d;
		private static final String INFINITE_BUCKET = "+Inf";

		private final OperationLatency latency;

		private OperationLatencyMetrics(OperationLatency latency) {
			this.latency = latency;
		}

		@Override
		public long getCount() {
			return latency.count();
		}

		@Override
		public double getSum() {
			return latency.totalNanoseconds() / NANOSECONDS_IN_SECOND;
		}

		@Override
		public Map<String, Long> getBuckets() {
			long[] bounds = OperationLatency.bucketBounds();
			long[] counts = latency.bucketCounts();
			Map<String, Long> buckets = new LinkedHashMap<>();
			for (int i = 0; i < bounds.length; i++) {
				buckets.put(String.valueOf(bounds[i] / MILLISECONDS_IN_SECOND), counts[i]);
			}
			buckets.put(INFINITE_BUCKET, counts[bounds.length]);

			return buckets;
		}
	}
}
