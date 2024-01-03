/*
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.server.monitor;

import is.codion.common.db.pool.ConnectionPoolState;
import is.codion.common.db.pool.ConnectionPoolStatistics;
import is.codion.common.db.pool.ConnectionPoolWrapper;
import is.codion.common.event.Event;
import is.codion.common.scheduler.TaskScheduler;
import is.codion.common.state.State;
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

  private static final int THOUSAND = 1000;

  private final Event<?> statisticsUpdatedEvent = Event.event();

  private final String username;
  private final ConnectionPoolWrapper connectionPool;
  private ConnectionPoolStatistics poolStatistics;

  private final Value<Integer> pooledConnectionTimeoutValue;
  private final Value<Integer> pooledCleanupIntervalValue;
  private final Value<Integer> minimumPoolSizeValue;
  private final Value<Integer> maximumPoolSizeValue;
  private final Value<Integer> maximumCheckoutTimeValue;
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
  private final YIntervalSeries averageCheckOutTime = new YIntervalSeries("Average check out time (ms)");
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
    this.pooledConnectionTimeoutValue = Value.value(connectionPool.getIdleConnectionTimeout(), 0);
    this.pooledConnectionTimeoutValue.addDataListener(connectionPool::setIdleConnectionTimeout);
    this.pooledCleanupIntervalValue = Value.value(connectionPool.getCleanupInterval(), 0);
    this.pooledCleanupIntervalValue.addDataListener(connectionPool::setCleanupInterval);
    this.minimumPoolSizeValue = Value.value(connectionPool.getMinimumPoolSize(), 0);
    this.minimumPoolSizeValue.addDataListener(connectionPool::setMinimumPoolSize);
    this.maximumPoolSizeValue = Value.value(connectionPool.getMaximumPoolSize(), 0);
    this.maximumPoolSizeValue.addDataListener(connectionPool::setMaximumPoolSize);
    this.maximumCheckoutTimeValue = Value.value(connectionPool.getMaximumCheckOutTime(), 0);
    this.maximumCheckoutTimeValue.addDataListener(connectionPool::setMaximumCheckOutTime);
    this.collectSnapshotStatisticsState = State.state(connectionPool.isCollectSnapshotStatistics());
    this.collectSnapshotStatisticsState.addDataListener(connectionPool::setCollectSnapshotStatistics);
    this.collectCheckOutTimesState = State.state(connectionPool.isCollectCheckOutTimes());
    this.collectCheckOutTimesState.addDataListener(connectionPool::setCollectCheckOutTimes);

    this.statisticsCollection.addSeries(inPoolSeries);
    this.statisticsCollection.addSeries(inUseSeries);
    this.statisticsCollection.addSeries(poolSizeSeries);
    this.statisticsCollection.addSeries(minimumPoolSizeSeries);
    this.statisticsCollection.addSeries(maximumPoolSizeSeries);
    this.connectionRequestsPerSecondCollection.addSeries(connectionRequestsPerSecond);
    this.connectionRequestsPerSecondCollection.addSeries(failedRequestsPerSecond);
    this.checkOutTimeCollection.addSeries(averageCheckOutTime);
    this.updateScheduler = TaskScheduler.builder(this::updateStatistics)
            .interval(updateRate, TimeUnit.SECONDS)
            .start();
    bindEvents();
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
    return maximumCheckoutTimeValue;
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
    averageCheckOutTime.clear();
  }

  /**
   * @return the State controlling whether snapshot statistics are collected
   */
  public State collectSnapshotStatistics() {
    return collectSnapshotStatisticsState;
  }

  /**
   * @return the State controlling whether checkout times are collected
   */
  public State collectCheckOutTimes() {
    return collectCheckOutTimesState;
  }

  /**
   * @param listener notified each time the statistics are updated
   */
  public void addStatisticsListener(Runnable listener) {
    statisticsUpdatedEvent.addListener(listener);
  }

  /**
   * @return the value controlling the update interval
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

  private void setPoolCleanupInterval(int poolCleanupInterval) {
    connectionPool.setCleanupInterval(poolCleanupInterval * THOUSAND);
  }

  private void setMinimumPoolSize(int minimumPoolSize) {
    connectionPool.setMinimumPoolSize(minimumPoolSize);
  }

  private void setMaximumPoolSize(int maximumPoolSize) {
    connectionPool.setMaximumPoolSize(maximumPoolSize);
  }

  private void setMaximumCheckOutTime(int maximumCheckOutTime) {
    connectionPool.setMaximumCheckOutTime(maximumCheckOutTime);
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
    averageCheckOutTime.add(timestamp, poolStatistics.averageGetTime(),
            poolStatistics.minimumCheckOutTime(), poolStatistics.maximumCheckOutTime());
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
    pooledConnectionTimeoutValue.set(connectionPool.getIdleConnectionTimeout());
    pooledCleanupIntervalValue.set(connectionPool.getCleanupInterval() / THOUSAND);
    minimumPoolSizeValue.set(connectionPool.getMinimumPoolSize());
    maximumPoolSizeValue.set(connectionPool.getMaximumPoolSize());
    maximumCheckoutTimeValue.set(connectionPool.getMaximumCheckOutTime());
    statisticsUpdatedEvent.run();
  }

  private void bindEvents() {
    pooledCleanupIntervalValue.addDataListener(this::setPoolCleanupInterval);
    minimumPoolSizeValue.addDataListener(this::setMinimumPoolSize);
    maximumPoolSizeValue.addDataListener(this::setMaximumPoolSize);
    maximumCheckoutTimeValue.addDataListener(this::setMaximumCheckOutTime);
    collectSnapshotStatisticsState.addDataListener(connectionPool::setCollectSnapshotStatistics);
    collectCheckOutTimesState.addDataListener(connectionPool::setCollectCheckOutTimes);
  }
}
