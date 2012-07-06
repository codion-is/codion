/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.db.pool.ConnectionPoolState;
import org.jminor.common.db.pool.ConnectionPoolStatistics;
import org.jminor.common.model.Event;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Events;
import org.jminor.common.model.User;

import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A ConnectionPoolMonitor
 */
public final class ConnectionPoolMonitor {

  private static final int THOUSAND = 1000;

  private final Event evtStatisticsUpdated = Events.event();
  private final Event evtStatisticsUpdateIntervalChanged = Events.event();
  private final Event evtCollectFineGrainedStatsChanged = Events.event();
  private final Event evtRefresh = Events.event();

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

  private long lastStatisticsUpdateTime = 0;

  private Timer updateTimer;
  private int statisticsUpdateInterval;

  public ConnectionPoolMonitor(final ConnectionPool connectionPool) throws RemoteException {
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
    setStatisticsUpdateInterval(3);
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
    evtCollectFineGrainedStatsChanged.fire();
  }

  public boolean isCollectFineGrainedStatistics() {
    return connectionPool.isCollectFineGrainedStatistics();
  }

  public void setStatisticsUpdateInterval(final int value) {
    if (value != this.statisticsUpdateInterval) {
      statisticsUpdateInterval = value;
      evtStatisticsUpdateIntervalChanged.fire();
      startUpdateTimer(value * 1000);
    }
  }

  public int getStatisticsUpdateInterval() {
    return statisticsUpdateInterval;
  }

  public void shutdown() {
    if (updateTimer != null) {
      updateTimer.cancel();
    }
  }

  public void addStatisticsListener(final EventListener listener) {
    evtStatisticsUpdated.addListener(listener);
  }

  public void removeStatisticsListener(final EventListener listener) {
    evtStatisticsUpdated.removeListener(listener);
  }

  public EventObserver getCollectFineGrainedStatisticsObserver() {
    return evtCollectFineGrainedStatsChanged.getObserver();
  }

  public EventObserver getRefreshObserver() {
    return evtRefresh.getObserver();
  }

  public EventObserver getStatisticsObserver() {
    return evtStatisticsUpdated.getObserver();
  }

  public EventObserver getStatisticsUpdateIntervalObserver() {
    return evtStatisticsUpdateIntervalChanged.getObserver();
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
      for (final ConnectionPoolState inPool : stats) {
        fineGrainedInPoolSeries.add(inPool.getTimestamp(), inPool.getSize());
        fineGrainedInUseSeries.add(inPool.getTimestamp(), inPool.getInUse());
      }

      this.fineGrainedStatisticsCollection.removeAllSeries();
      this.fineGrainedStatisticsCollection.addSeries(fineGrainedInPoolSeries);
      this.fineGrainedStatisticsCollection.addSeries(fineGrainedInUseSeries);
    }
    evtStatisticsUpdated.fire();
  }

  private static List<ConnectionPoolState> sortAndRemoveDuplicates(final List<ConnectionPoolState> stats) {
    final List<ConnectionPoolState> poolStates = new ArrayList<ConnectionPoolState>(stats.size());
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

  private void startUpdateTimer(final int delay) {
    if (delay <= 0) {
      return;
    }

    if (updateTimer != null) {
      updateTimer.cancel();
    }
    updateTimer = new Timer(true);
    updateTimer.schedule(new TimerTask() {
      @Override
      public void run() {
        updateStatistics();
      }
    }, delay, delay);
  }

  private static final class StateComparator implements Comparator<ConnectionPoolState>, Serializable {
    private static final long serialVersionUID = 1;
    /** {@inheritDoc} */
    @Override
    public int compare(final ConnectionPoolState o1, final ConnectionPoolState o2) {
      return ((Long) o1.getTimestamp()).compareTo(o2.getTimestamp());
    }
  }
}
