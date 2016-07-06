/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.server.monitor;

import org.jminor.common.Event;
import org.jminor.common.EventObserver;
import org.jminor.common.Events;
import org.jminor.common.TaskScheduler;
import org.jminor.common.User;
import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.db.pool.ConnectionPoolState;
import org.jminor.common.db.pool.ConnectionPoolStatistics;
import org.jminor.framework.Configuration;

import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A ConnectionPoolMonitor
 */
public final class ConnectionPoolMonitor {

  private static final int THOUSAND = 1000;

  private final Event statisticsUpdatedEvent = Events.event();
  private final Event<Boolean> collectFineGrainedStatisticsChangedEvent = Events.event();

  private final User user;
  private final ConnectionPool connectionPool;
  private ConnectionPoolStatistics poolStatistics;

  private final XYSeries poolSizeSeries = new XYSeries("Size");
  private final XYSeries minimumPoolSizeSeries = new XYSeries("Min");
  private final XYSeries maximumPoolSizeSeries = new XYSeries("Max");
  private final XYSeries inPoolSeries = new XYSeries("In pool");
  private final XYSeries inUseSeries = new XYSeries("In use");
  private final XYSeriesCollection fineGrainedStatisticsCollection = new XYSeriesCollection();
  private final XYSeriesCollection statisticsCollection = new XYSeriesCollection();
  private final XYSeries delayedRequestsPerSecond = new XYSeries("Delayed");
  private final XYSeries failedRequestsPerSecond = new XYSeries("Failed");
  private final XYSeries connectionRequestsPerSecond = new XYSeries("Requests");
  private final XYSeriesCollection connectionRequestsPerSecondCollection = new XYSeriesCollection();
  private final YIntervalSeries averageCheckOutTime = new YIntervalSeries("Average check out time");
  private final YIntervalSeriesCollection checkOutTimeCollection = new YIntervalSeriesCollection();

  private final TaskScheduler updateScheduler = new TaskScheduler(this::updateStatistics,
          Configuration.getIntValue(Configuration.SERVER_MONITOR_UPDATE_RATE), 2, TimeUnit.SECONDS).start();

  private long lastStatisticsUpdateTime = 0;

  public ConnectionPoolMonitor(final ConnectionPool connectionPool) {
    this.user = connectionPool.getUser();
    this.connectionPool = connectionPool;
    this.statisticsCollection.addSeries(inPoolSeries);
    this.statisticsCollection.addSeries(inUseSeries);
    this.statisticsCollection.addSeries(poolSizeSeries);
    this.statisticsCollection.addSeries(minimumPoolSizeSeries);
    this.statisticsCollection.addSeries(maximumPoolSizeSeries);
    this.connectionRequestsPerSecondCollection.addSeries(connectionRequestsPerSecond);
    this.connectionRequestsPerSecondCollection.addSeries(delayedRequestsPerSecond);
    this.connectionRequestsPerSecondCollection.addSeries(failedRequestsPerSecond);
    this.checkOutTimeCollection.addSeries(averageCheckOutTime);
    updateStatistics();
  }

  public User getUser() {
    return user;
  }

  public ConnectionPoolStatistics getConnectionPoolStatistics() {
    return poolStatistics;
  }

  public int getPooledConnectionTimeout() {
    return connectionPool.getConnectionTimeout() / THOUSAND;
  }

  public void setPooledConnectionTimeout(final int value) {
    connectionPool.setConnectionTimeout(value * THOUSAND);
  }

  public int getPoolCleanupInterval() {
    return connectionPool.getCleanupInterval() / THOUSAND;
  }

  public void setPoolCleanupInterval(final int value) {
    connectionPool.setCleanupInterval(value);
  }

  public int getMinimumPoolSize() {
    return connectionPool.getMinimumPoolSize();
  }

  public void setMinimumPoolSize(final int value) {
    connectionPool.setMinimumPoolSize(value);
  }

  public int getMaximumPoolSize() {
    return connectionPool.getMaximumPoolSize();
  }

  public void setMaximumPoolSize(final int value) {
    connectionPool.setMaximumPoolSize(value);
  }

  public int getMaximumRetryWaitPeriod() {
    return connectionPool.getMaximumRetryWaitPeriod();
  }

  public void setMaximumRetryWaitPeriod(final int value) {
    connectionPool.setMaximumRetryWaitPeriod(value);
  }

  public int getMaximumCheckOutTime() {
    return connectionPool.getMaximumCheckOutTime();
  }

  public void setMaximumCheckOutTime(final int value) {
    connectionPool.setMaximumCheckOutTime(value);
  }

  public int getNewConnectionThreshold() {
    return connectionPool.getNewConnectionThreshold();
  }

  public void setNewConnectionThreshold(final int value) {
    connectionPool.setNewConnectionThreshold(value);
  }

  public boolean datasetContainsData() {
    return fineGrainedStatisticsCollection.getSeriesCount() > 0
            && fineGrainedStatisticsCollection.getSeries(0).getItemCount() > 0
            && fineGrainedStatisticsCollection.getSeries(1).getItemCount() > 0;
  }

  public XYDataset getFineGrainedInPoolDataset() {
    final XYSeriesCollection poolDataset = new XYSeriesCollection();
    poolDataset.addSeries(fineGrainedStatisticsCollection.getSeries(0));
    poolDataset.addSeries(fineGrainedStatisticsCollection.getSeries(1));
    poolDataset.addSeries(fineGrainedStatisticsCollection.getSeries(2));

    return poolDataset;
  }

  public XYDataset getInPoolDataset() {
    return statisticsCollection;
  }

  public XYDataset getRequestsPerSecondDataset() {
    return connectionRequestsPerSecondCollection;
  }

  public YIntervalSeriesCollection getCheckOutTimeCollection() {
    return checkOutTimeCollection;
  }

  public void resetStatistics() {
    connectionPool.resetStatistics();
  }

  public void resetInPoolStatistics() {
    inPoolSeries.clear();
    inUseSeries.clear();
    connectionRequestsPerSecond.clear();
    delayedRequestsPerSecond.clear();
    failedRequestsPerSecond.clear();
    poolSizeSeries.clear();
    minimumPoolSizeSeries.clear();
    maximumPoolSizeSeries.clear();
    averageCheckOutTime.clear();
  }

  public void setCollectFineGrainedStatistics(final boolean value) {
    connectionPool.setCollectFineGrainedStatistics(value);
    collectFineGrainedStatisticsChangedEvent.fire(value);
  }

  public boolean isCollectFineGrainedStatistics() {
    return connectionPool.isCollectFineGrainedStatistics();
  }

  public void shutdown() {
    updateScheduler.stop();
  }

  public EventObserver<Boolean> getCollectFineGrainedStatisticsObserver() {
    return collectFineGrainedStatisticsChangedEvent.getObserver();
  }

  public EventObserver getStatisticsObserver() {
    return statisticsUpdatedEvent.getObserver();
  }

  public TaskScheduler getUpdateScheduler() {
    return updateScheduler;
  }

  private void updateStatistics() {
    poolStatistics = connectionPool.getStatistics(lastStatisticsUpdateTime);
    lastStatisticsUpdateTime = poolStatistics.getTimestamp();
    poolSizeSeries.add(poolStatistics.getTimestamp(), poolStatistics.getSize());
    minimumPoolSizeSeries.add(poolStatistics.getTimestamp(), connectionPool.getMinimumPoolSize());
    maximumPoolSizeSeries.add(poolStatistics.getTimestamp(), connectionPool.getMaximumPoolSize());
    inPoolSeries.add(poolStatistics.getTimestamp(), poolStatistics.getAvailable());
    inUseSeries.add(poolStatistics.getTimestamp(), poolStatistics.getInUse());
    connectionRequestsPerSecond.add(poolStatistics.getTimestamp(), poolStatistics.getRequestsPerSecond());
    delayedRequestsPerSecond.add(poolStatistics.getTimestamp(), poolStatistics.getDelayedRequestsPerSecond());
    failedRequestsPerSecond.add(poolStatistics.getTimestamp(), poolStatistics.getFailedRequestsPerSecond());
    averageCheckOutTime.add(poolStatistics.getTimestamp(), poolStatistics.getAverageGetTime(),
            poolStatistics.getMinimumCheckOutTime(), poolStatistics.getMaximumCheckOutTime());
    final List<ConnectionPoolState> stats = sortAndRemoveDuplicates(poolStatistics.getFineGrainedStatistics());
    if (!stats.isEmpty()) {
      final XYSeries fineGrainedInPoolSeries = new XYSeries("In pool");
      final XYSeries fineGrainedInUseSeries = new XYSeries("In use");
      final XYSeries fineGrainedWaitingSeries = new XYSeries("Waiting");
      for (final ConnectionPoolState inPool : stats) {
        fineGrainedInPoolSeries.add(inPool.getTimestamp(), inPool.getSize());
        fineGrainedInUseSeries.add(inPool.getTimestamp(), inPool.getInUse());
        fineGrainedWaitingSeries.add(inPool.getTimestamp(), inPool.getWaiting());
      }

      this.fineGrainedStatisticsCollection.removeAllSeries();
      this.fineGrainedStatisticsCollection.addSeries(fineGrainedInPoolSeries);
      this.fineGrainedStatisticsCollection.addSeries(fineGrainedInUseSeries);
      this.fineGrainedStatisticsCollection.addSeries(fineGrainedWaitingSeries);
    }
    statisticsUpdatedEvent.fire();
  }

  private static List<ConnectionPoolState> sortAndRemoveDuplicates(final List<ConnectionPoolState> stats) {
    final List<ConnectionPoolState> poolStates = new ArrayList<>(stats.size());
    Collections.sort(poolStates, new StateComparator());
    long time = -1;
    for (int i = stats.size()-1; i >= 0; i--) {
      final ConnectionPoolState state = stats.get(i);
      if (state.getTimestamp() != time) {
        poolStates.add(state);
      }

      time = state.getTimestamp();
    }

    return poolStates;
  }

  private static final class StateComparator implements Comparator<ConnectionPoolState>, Serializable {
    private static final long serialVersionUID = 1;
    @Override
    public int compare(final ConnectionPoolState o1, final ConnectionPoolState o2) {
      return ((Long) o1.getTimestamp()).compareTo(o2.getTimestamp());
    }
  }
}
