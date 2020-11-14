/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.server.monitor;

import is.codion.common.TaskScheduler;
import is.codion.common.db.pool.ConnectionPool;
import is.codion.common.db.pool.ConnectionPoolState;
import is.codion.common.db.pool.ConnectionPoolStatistics;
import is.codion.common.event.Event;
import is.codion.common.event.EventObserver;
import is.codion.common.event.Events;
import is.codion.common.value.Value;
import is.codion.common.value.Values;

import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A ConnectionPoolMonitor
 */
public final class ConnectionPoolMonitor {

  private static final int THOUSAND = 1000;

  private final Event<?> statisticsUpdatedEvent = Events.event();
  private final Event<Boolean> collectSnapshotStatisticsChangedEvent = Events.event();

  private final String username;
  private final ConnectionPool connectionPool;
  private ConnectionPoolStatistics poolStatistics;

  private final Value<Integer> pooledConnectionTimeoutValue;
  private final Value<Integer> pooledCleanupIntervalValue;
  private final Value<Integer> minimumPoolSizeValue;
  private final Value<Integer> maximumPoolSizeValue;
  private final Value<Integer> maximumCheckoutTimeValue;

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
  private final Value<Integer> updateIntervalValue;

  private long lastStatisticsUpdateTime = 0;

  /**
   * Instantiates a new {@link ConnectionPoolMonitor}
   * @param connectionPool the connection pool to monitor
   * @param updateRate the initial statistics update rate in seconds
   */
  public ConnectionPoolMonitor(final ConnectionPool connectionPool, final int updateRate) {
    this.username = connectionPool.getUser().getUsername();
    this.connectionPool = connectionPool;
    this.pooledConnectionTimeoutValue = Values.value(connectionPool.getConnectionTimeout() / THOUSAND);
    this.pooledCleanupIntervalValue = Values.value(connectionPool.getCleanupInterval() / THOUSAND);
    this.minimumPoolSizeValue = Values.value(connectionPool.getMinimumPoolSize());
    this.maximumPoolSizeValue = Values.value(connectionPool.getMinimumPoolSize());
    this.maximumCheckoutTimeValue = Values.value(connectionPool.getMaximumCheckOutTime());
    this.statisticsCollection.addSeries(inPoolSeries);
    this.statisticsCollection.addSeries(inUseSeries);
    this.statisticsCollection.addSeries(poolSizeSeries);
    this.statisticsCollection.addSeries(minimumPoolSizeSeries);
    this.statisticsCollection.addSeries(maximumPoolSizeSeries);
    this.connectionRequestsPerSecondCollection.addSeries(connectionRequestsPerSecond);
    this.connectionRequestsPerSecondCollection.addSeries(failedRequestsPerSecond);
    this.checkOutTimeCollection.addSeries(averageCheckOutTime);
    this.updateScheduler = new TaskScheduler(this::updateStatistics, updateRate, 0, TimeUnit.SECONDS).start();
    this.updateIntervalValue = new IntervalValue(updateScheduler);
    bindEvents();
  }

  /**
   * @return the user the connection pool is based on
   */
  public String getUsername() {
    return username;
  }

  /**
   * @return the latest pool statistics
   */
  public ConnectionPoolStatistics getConnectionPoolStatistics() {
    return poolStatistics;
  }

  /**
   * @return the pool connection timeout in seconds
   */
  public Value<Integer> getPooledConnectionTimeoutValue() {
    return pooledConnectionTimeoutValue;
  }

  /**
   * @return the pool maintenance interval in seconds
   */
  public Value<Integer> getPoolCleanupIntervalValue() {
    return pooledCleanupIntervalValue;
  }

  /**
   * @return the minimum pool size to maintain
   */
  public Value<Integer> getMinimumPoolSizeValue() {
    return minimumPoolSizeValue;
  }

  /**
   * @return the maximum allowed pool size
   */
  public Value<Integer> getMaximumPoolSizeValue() {
    return maximumPoolSizeValue;
  }

  /**
   * @return the maximum wait time for a connection
   */
  public Value<Integer> getMaximumCheckOutTimeValue() {
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
  public XYDataset getSnapshotDataset() {
    final XYSeriesCollection poolDataset = new XYSeriesCollection();
    poolDataset.addSeries(snapshotStatisticsCollection.getSeries(0));
    poolDataset.addSeries(snapshotStatisticsCollection.getSeries(1));
    poolDataset.addSeries(snapshotStatisticsCollection.getSeries(2));

    return poolDataset;
  }

  /**
   * @return the dataset for the number of connections in the pool
   */
  public XYDataset getInPoolDataset() {
    return statisticsCollection;
  }

  /**
   * @return the dataset for the number of connection requests per second
   */
  public XYDataset getRequestsPerSecondDataset() {
    return connectionRequestsPerSecondCollection;
  }

  /**
   * @return the dataset for the connection check out time
   */
  public IntervalXYDataset getCheckOutTimeCollection() {
    return checkOutTimeCollection;
  }

  /**
   * Resets all collected pool statistics
   */
  public void clearStatistics() {
    connectionPool.resetStatistics();
  }

  /**
   * Clears all graph data sets
   */
  public void clearInPoolStatistics() {
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
   * @param collectSnapshotStatistics true if snapshot stats should be collected
   */
  public void setCollectSnapshotStatistics(final boolean collectSnapshotStatistics) {
    connectionPool.setCollectSnapshotStatistics(collectSnapshotStatistics);
    collectSnapshotStatisticsChangedEvent.onEvent(collectSnapshotStatistics);
  }

  /**
   * @return true if snapshot stats are being collected
   */
  public boolean isCollectSnapshotStatistics() {
    return connectionPool.isCollectSnapshotStatistics();
  }

  /**
   * @return EventObserver notified when snapshot stats collection status is changed
   */
  public EventObserver<Boolean> getCollectSnapshotStatisticsObserver() {
    return collectSnapshotStatisticsChangedEvent.getObserver();
  }

  /**
   * @return EventObserver notified when statistics have been updated
   */
  public EventObserver<?> getStatisticsObserver() {
    return statisticsUpdatedEvent.getObserver();
  }

  /**
   * @return the value controlling the update interval
   */
  public Value<Integer> getUpdateIntervalValue() {
    return updateIntervalValue;
  }

  /**
   * Shuts down this pool monitor
   */
  public void shutdown() {
    updateScheduler.stop();
  }

  private void setPooledConnectionTimeout(final int value) {
    connectionPool.setConnectionTimeout(value * THOUSAND);
  }

  private void setPoolCleanupInterval(final int value) {
    connectionPool.setCleanupInterval(value * THOUSAND);
  }

  private void setMinimumPoolSize(final int value) {
    connectionPool.setMinimumPoolSize(value);
  }

  private void setMaximumPoolSize(final int value) {
    connectionPool.setMaximumPoolSize(value);
  }

  private void setMaximumCheckOutTime(final int value) {
    connectionPool.setMaximumCheckOutTime(value);
  }

  private void updateStatistics() {
    poolStatistics = connectionPool.getStatistics(lastStatisticsUpdateTime);
    final long timestamp = poolStatistics.getTimestamp();
    lastStatisticsUpdateTime = timestamp;
    poolSizeSeries.add(timestamp, poolStatistics.getSize());
    minimumPoolSizeSeries.add(timestamp, connectionPool.getMinimumPoolSize());
    maximumPoolSizeSeries.add(timestamp, connectionPool.getMaximumPoolSize());
    inPoolSeries.add(timestamp, poolStatistics.getAvailable());
    inUseSeries.add(timestamp, poolStatistics.getInUse());
    connectionRequestsPerSecond.add(timestamp, poolStatistics.getRequestsPerSecond());
    failedRequestsPerSecond.add(timestamp, poolStatistics.getFailedRequestsPerSecond());
    averageCheckOutTime.add(timestamp, poolStatistics.getAverageGetTime(),
            poolStatistics.getMinimumCheckOutTime(), poolStatistics.getMaximumCheckOutTime());
    final List<ConnectionPoolState> snapshotStatistics = poolStatistics.getSnapshot();
    if (!snapshotStatistics.isEmpty()) {
      final XYSeries snapshotInPoolSeries = new XYSeries("In pool");
      final XYSeries snapshotInUseSeries = new XYSeries("In use");
      final XYSeries snapshotWaitingSeries = new XYSeries("Waiting");
      for (final ConnectionPoolState inPool : snapshotStatistics) {
        snapshotInPoolSeries.add(inPool.getTimestamp(), inPool.getSize());
        snapshotInUseSeries.add(inPool.getTimestamp(), inPool.getInUse());
        snapshotWaitingSeries.add(inPool.getTimestamp(), inPool.getWaiting());
      }

      this.snapshotStatisticsCollection.removeAllSeries();
      this.snapshotStatisticsCollection.addSeries(snapshotInPoolSeries);
      this.snapshotStatisticsCollection.addSeries(snapshotInUseSeries);
      this.snapshotStatisticsCollection.addSeries(snapshotWaitingSeries);
    }
    pooledConnectionTimeoutValue.set(connectionPool.getConnectionTimeout() / THOUSAND);
    pooledCleanupIntervalValue.set(connectionPool.getCleanupInterval() / THOUSAND);
    minimumPoolSizeValue.set(connectionPool.getMinimumPoolSize());
    maximumPoolSizeValue.set(connectionPool.getMaximumPoolSize());
    maximumCheckoutTimeValue.set(connectionPool.getMaximumCheckOutTime());
    statisticsUpdatedEvent.onEvent();
  }

  private void bindEvents() {
    pooledConnectionTimeoutValue.addDataListener(this::setPooledConnectionTimeout);
    pooledCleanupIntervalValue.addDataListener(this::setPoolCleanupInterval);
    minimumPoolSizeValue.addDataListener(this::setMinimumPoolSize);
    maximumPoolSizeValue.addDataListener(this::setMaximumPoolSize);
    maximumCheckoutTimeValue.addDataListener(this::setMaximumCheckOutTime);
  }
}
