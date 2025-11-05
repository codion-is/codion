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
package is.codion.tools.monitor.model;

import is.codion.common.db.pool.ConnectionPoolState;
import is.codion.common.db.pool.ConnectionPoolStatistics;
import is.codion.common.db.pool.ConnectionPoolWrapper;
import is.codion.common.event.Event;
import is.codion.common.observer.Observer;
import is.codion.common.state.State;
import is.codion.common.utilities.scheduler.TaskScheduler;
import is.codion.common.value.Value;

import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

/**
 * A ConnectionPoolMonitor
 */
public final class ConnectionPoolMonitor {

	private final Event<?> statisticsUpdated = Event.event();

	private final String username;
	private final ConnectionPoolWrapper connectionPool;
	private ConnectionPoolStatistics poolStatistics;

	private final Value<Integer> pooledConnectionTimeoutValue;
	private final Value<Integer> pooledCleanupIntervalValue;
	private final Value<Integer> minimumPoolSizeValue;
	private final Value<Integer> maximumPoolSizeValue;
	private final Value<Integer> maximumValue;
	private final State collectSnapshotStatisticsState;
	private final State collectCheckOutTimesState;

	private final XYSeries poolSizeSeries = new XYSeries("Size");
	private final XYSeries minimumPoolSizeSeries = new XYSeries("Min. size");
	private final XYSeries maximumPoolSizeSeries = new XYSeries("Max. size");
	private final XYSeries inPoolSeries = new XYSeries("Available");
	private final XYSeries inUseSeries = new XYSeries("In use");
	private final XYSeriesCollection snapshotStatisticsCollection = new XYSeriesCollection();
	private final XYSeriesCollection statisticsCollection = new XYSeriesCollection();
	private final XYSeries failedRequestsPerSecond = new XYSeries("Failed requests/sec");
	private final XYSeries connectionRequestsPerSecond = new XYSeries("Requests/sec");
	private final XYSeriesCollection connectionRequestsPerSecondCollection = new XYSeriesCollection();
	private final YIntervalSeries averageTime = new YIntervalSeries("Average check out time (μs)");
	private final YIntervalSeriesCollection checkOutTimeCollection = new YIntervalSeriesCollection();

	private final TaskScheduler updateScheduler;

	private long lastStatisticsUpdateTime = 0;

	/**
	 * Instantiates a new {@link ConnectionPoolMonitor}
	 * @param connectionPool the connection pool to monitor
	 * @param updateRate the initial statistics update rate in seconds
	 */
	public ConnectionPoolMonitor(ConnectionPoolWrapper connectionPool, int updateRate) {
		this.username = requireNonNull(connectionPool).user().username();
		this.connectionPool = connectionPool;
		this.pooledConnectionTimeoutValue = Value.builder()
						.nonNull(0)
						.value(connectionPool.getIdleTimeout())
						.consumer(connectionPool::setIdleTimeout)
						.build();
		this.pooledCleanupIntervalValue = Value.builder()
						.nonNull(0)
						.value(connectionPool.getCleanupInterval())
						.consumer(connectionPool::setCleanupInterval)
						.build();
		this.minimumPoolSizeValue = Value.builder()
						.nonNull(0)
						.value(connectionPool.getMinimumPoolSize())
						.consumer(connectionPool::setMinimumPoolSize)
						.build();
		this.maximumPoolSizeValue = Value.builder()
						.nonNull(0)
						.value(connectionPool.getMaximumPoolSize())
						.consumer(connectionPool::setMaximumPoolSize)
						.build();
		this.maximumValue = Value.builder()
						.nonNull(0)
						.value(connectionPool.getMaximumCheckOutTime())
						.consumer(connectionPool::setMaximumCheckOutTime)
						.build();
		this.collectSnapshotStatisticsState = State.state(connectionPool.isCollectSnapshotStatistics());
		this.collectSnapshotStatisticsState.addConsumer(connectionPool::setCollectSnapshotStatistics);
		this.collectCheckOutTimesState = State.state(connectionPool.isCollectCheckOutTimes());
		this.collectCheckOutTimesState.addConsumer(connectionPool::setCollectCheckOutTimes);

		this.statisticsCollection.addSeries(inPoolSeries);
		this.statisticsCollection.addSeries(inUseSeries);
		this.statisticsCollection.addSeries(poolSizeSeries);
		this.statisticsCollection.addSeries(minimumPoolSizeSeries);
		this.statisticsCollection.addSeries(maximumPoolSizeSeries);
		this.connectionRequestsPerSecondCollection.addSeries(connectionRequestsPerSecond);
		this.connectionRequestsPerSecondCollection.addSeries(failedRequestsPerSecond);
		this.checkOutTimeCollection.addSeries(averageTime);
		this.updateScheduler = TaskScheduler.builder()
						.task(this::updateStatistics)
						.interval(updateRate, TimeUnit.SECONDS)
						.start();
	}

	/**
	 * @return the user the connection pool is based on
	 */
	public String username() {
		return username;
	}

	/**
	 * @return the latest pool statistics
	 */
	public ConnectionPoolStatistics connectionPoolStatistics() {
		return poolStatistics;
	}

	/**
	 * @return the pool connection timeout in milliseconds
	 */
	public Value<Integer> pooledConnectionTimeout() {
		return pooledConnectionTimeoutValue;
	}

	/**
	 * @return the pool maintenance interval in seconds
	 */
	public Value<Integer> poolCleanupInterval() {
		return pooledCleanupIntervalValue;
	}

	/**
	 * @return the minimum pool size to maintain
	 */
	public Value<Integer> minimumPoolSize() {
		return minimumPoolSizeValue;
	}

	/**
	 * @return the maximum allowed pool size
	 */
	public Value<Integer> maximumPoolSize() {
		return maximumPoolSizeValue;
	}

	/**
	 * @return the maximum wait time for a connection
	 */
	public Value<Integer> maximumCheckOutTime() {
		return maximumValue;
	}

	/**
	 * @return true if the graph datasets contain data
	 */
	public boolean datasetContainsData() {
		return snapshotStatisticsCollection.getSeriesCount() > 0
						&& snapshotStatisticsCollection.getSeries(0).getItemCount() > 0
						&& snapshotStatisticsCollection.getSeries(1).getItemCount() > 0;
	}

	/**
	 * @return the dataset for snapshot pool stats
	 */
	public XYDataset snapshotDataset() {
		XYSeriesCollection poolDataset = new XYSeriesCollection();
		poolDataset.addSeries(snapshotStatisticsCollection.getSeries(0));
		poolDataset.addSeries(snapshotStatisticsCollection.getSeries(1));
		poolDataset.addSeries(snapshotStatisticsCollection.getSeries(2));

		return poolDataset;
	}

	/**
	 * @return the dataset for the number of connections in the pool
	 */
	public XYDataset inPoolDataset() {
		return statisticsCollection;
	}

	/**
	 * @return the dataset for the number of connection requests per second
	 */
	public XYDataset requestsPerSecondDataset() {
		return connectionRequestsPerSecondCollection;
	}

	/**
	 * @return the dataset for the connection check out time
	 */
	public IntervalXYDataset checkOutTimeCollection() {
		return checkOutTimeCollection;
	}

	/**
	 * Resets all collected pool statistics
	 */
	public void resetStatistics() {
		connectionPool.resetStatistics();
	}

	/**
	 * Clears all graph data sets
	 */
	public void clearStatistics() {
		inPoolSeries.clear();
		inUseSeries.clear();
		connectionRequestsPerSecond.clear();
		failedRequestsPerSecond.clear();
		poolSizeSeries.clear();
		minimumPoolSizeSeries.clear();
		maximumPoolSizeSeries.clear();
		averageTime.clear();
	}

	/**
	 * @return the {@link State} controlling whether snapshot statistics are collected
	 */
	public State collectSnapshotStatistics() {
		return collectSnapshotStatisticsState;
	}

	/**
	 * @return the {@link State} controlling whether checkout times are collected
	 */
	public State collectCheckOutTimes() {
		return collectCheckOutTimesState;
	}

	/**
	 * @return an observer notified each time the statistics are updated
	 */
	public Observer<?> statisticsUpdated() {
		return statisticsUpdated.observer();
	}

	/**
	 * @return the {@link Value} controlling the update interval
	 */
	public Value<Integer> updateInterval() {
		return updateScheduler.interval();
	}

	/**
	 * Shuts down this pool monitor
	 */
	public void shutdown() {
		updateScheduler.stop();
	}

	private void updateStatistics() {
		poolStatistics = connectionPool.statistics(lastStatisticsUpdateTime);
		long timestamp = poolStatistics.timestamp();
		lastStatisticsUpdateTime = timestamp;
		poolSizeSeries.add(timestamp, poolStatistics.size());
		minimumPoolSizeSeries.add(timestamp, connectionPool.getMinimumPoolSize());
		maximumPoolSizeSeries.add(timestamp, connectionPool.getMaximumPoolSize());
		inPoolSeries.add(timestamp, poolStatistics.available());
		inUseSeries.add(timestamp, poolStatistics.inUse());
		connectionRequestsPerSecond.add(timestamp, poolStatistics.requestsPerSecond());
		failedRequestsPerSecond.add(timestamp, poolStatistics.failedRequestsPerSecond());
		averageTime.add(timestamp, poolStatistics.averageTime(),
						poolStatistics.minimumTime(), poolStatistics.maximumTime());
		List<ConnectionPoolState> snapshotStatistics = poolStatistics.snapshot();
		if (!snapshotStatistics.isEmpty()) {
			XYSeries snapshotInPoolSeries = new XYSeries("In pool");
			XYSeries snapshotInUseSeries = new XYSeries("In use");
			XYSeries snapshotWaitingSeries = new XYSeries("Waiting");
			for (ConnectionPoolState inPool : snapshotStatistics) {
				snapshotInPoolSeries.add(inPool.timestamp(), inPool.size());
				snapshotInUseSeries.add(inPool.timestamp(), inPool.inUse());
				snapshotWaitingSeries.add(inPool.timestamp(), inPool.waiting());
			}

			this.snapshotStatisticsCollection.removeAllSeries();
			this.snapshotStatisticsCollection.addSeries(snapshotInPoolSeries);
			this.snapshotStatisticsCollection.addSeries(snapshotInUseSeries);
			this.snapshotStatisticsCollection.addSeries(snapshotWaitingSeries);
		}
		pooledConnectionTimeoutValue.set(connectionPool.getIdleTimeout());
		pooledCleanupIntervalValue.set(connectionPool.getCleanupInterval());
		minimumPoolSizeValue.set(connectionPool.getMinimumPoolSize());
		maximumPoolSizeValue.set(connectionPool.getMaximumPoolSize());
		maximumValue.set(connectionPool.getMaximumCheckOutTime());
		statisticsUpdated.run();
	}
}
