/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.db.pool.ConnectionPoolState;
import org.jminor.common.db.pool.ConnectionPoolStatistics;
import org.jminor.common.model.Event;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Events;
import org.jminor.common.model.User;

import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;

import java.awt.event.ActionListener;
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

  private final Event evtStatsUpdated = Events.event();
  private final Event evtStatsUpdateIntervalChanged = Events.event();
  private final Event evtCollectFineGrainedStatsChanged = Events.event();
  private final Event evtRefresh = Events.event();

  private final User user;
  private final ConnectionPool pool;
  private ConnectionPoolStatistics poolStats;

  private final XYSeries poolSizeSeries = new XYSeries("Pool size");
  private final XYSeries minimumPoolSizeSeries = new XYSeries("Minimum size");
  private final XYSeries maximumPoolSizeSeries = new XYSeries("Maximum size");
  private final XYSeries inPoolSeriesMacro = new XYSeries("In pool");
  private final XYSeries inUseSeriesMacro = new XYSeries("In use");
  private final XYSeriesCollection statsCollection = new XYSeriesCollection();
  private final XYSeriesCollection macroStatsCollection = new XYSeriesCollection();
  private final XYSeries delayedRequestsPerSecond = new XYSeries("Delayed / second");
  private final XYSeries failedRequestsPerSecond = new XYSeries("Failed / second");
  private final XYSeries connectionRequestsPerSecond = new XYSeries("Requests / second");
  private final XYSeriesCollection connectionRequestsPerSecondCollection = new XYSeriesCollection();
  private final YIntervalSeries averageCheckOutTime = new YIntervalSeries("Average check out time");
  private final YIntervalSeriesCollection checkOutTimeCollection = new YIntervalSeriesCollection();

  private long lastStatsUpdateTime = 0;

  private Timer updateTimer;
  private int statsUpdateInterval;

  public ConnectionPoolMonitor(final User user, final ConnectionPool pool) throws RemoteException {
    this.user = user;
    this.pool = pool;
    this.macroStatsCollection.addSeries(inPoolSeriesMacro);
    this.macroStatsCollection.addSeries(inUseSeriesMacro);
    this.macroStatsCollection.addSeries(poolSizeSeries);
    this.macroStatsCollection.addSeries(minimumPoolSizeSeries);
    this.macroStatsCollection.addSeries(maximumPoolSizeSeries);
    this.connectionRequestsPerSecondCollection.addSeries(connectionRequestsPerSecond);
    this.connectionRequestsPerSecondCollection.addSeries(delayedRequestsPerSecond);
    this.connectionRequestsPerSecondCollection.addSeries(failedRequestsPerSecond);
    this.checkOutTimeCollection.addSeries(averageCheckOutTime);
    updateStats();
    setStatsUpdateInterval(3);
  }

  public User getUser() {
    return user;
  }

  public ConnectionPoolStatistics getConnectionPoolStats() {
    return poolStats;
  }

  public int getPooledConnectionTimeout() throws RemoteException {
    return pool.getConnectionTimeout() / 1000;
  }

  public void setPooledConnectionTimeout(final int value) throws RemoteException {
    pool.setConnectionTimeout(value * 1000);
  }

  public int getPoolCleanupInterval() throws RemoteException {
    return pool.getCleanupInterval() / 1000;
  }

  public void setPoolCleanupInterval(final int value) throws RemoteException {
    pool.setCleanupInterval(value);
  }

  public int getMinimumPoolSize() {
    return pool.getMinimumPoolSize();
  }

  public void setMinimumPoolSize(final int value) throws RemoteException {
    pool.setMinimumPoolSize(value);
  }

  public int getMaximumPoolSize() {
    return pool.getMaximumPoolSize();
  }

  public void setMaximumPoolSize(final int value) throws RemoteException {
    pool.setMaximumPoolSize(value);
  }

  public int getMaximumRetryWaitPeriod() {
    return pool.getMaximumRetryWaitPeriod();
  }

  public void setMaximumRetryWaitPeriod(final int value) throws RemoteException {
    pool.setMaximumRetryWaitPeriod(value);
  }

  public int getMaximumCheckOutTime() {
    return pool.getMaximumCheckOutTime();
  }

  public void setMaximumCheckOutTime(final int value) throws RemoteException {
    pool.setMaximumCheckOutTime(value);
  }

  public boolean datasetContainsData() {
    return statsCollection.getSeriesCount() > 0
            && statsCollection.getSeries(0).getItemCount() > 0
            && statsCollection.getSeries(1).getItemCount() > 0;
  }

  public XYDataset getInPoolDataSet() {
    final XYSeriesCollection poolDataSet = new XYSeriesCollection();
    poolDataSet.addSeries(statsCollection.getSeries(0));
    poolDataSet.addSeries(statsCollection.getSeries(1));

    return poolDataSet;
  }

  public XYDataset getInPoolDataSetMacro() {
    return macroStatsCollection;
  }

  public XYDataset getRequestsPerSecondDataSet() {
    return connectionRequestsPerSecondCollection;
  }

  public YIntervalSeriesCollection getCheckOutTimeCollection() {
    return checkOutTimeCollection;
  }

  public void resetStats() throws RemoteException {
    pool.resetStatistics();
  }

  public void resetInPoolStats() {
    inPoolSeriesMacro.clear();
    inUseSeriesMacro.clear();
    connectionRequestsPerSecond.clear();
    delayedRequestsPerSecond.clear();
    failedRequestsPerSecond.clear();
    poolSizeSeries.clear();
    minimumPoolSizeSeries.clear();
    maximumPoolSizeSeries.clear();
    averageCheckOutTime.clear();
  }

  public void setCollectFineGrainedStats(final boolean value) throws RemoteException {
    pool.setCollectFineGrainedStatistics(value);
    evtCollectFineGrainedStatsChanged.fire();
  }

  public boolean isCollectFineGrainedStats() throws RemoteException {
    return pool.isCollectFineGrainedStatistics();
  }

  public void setStatsUpdateInterval(final int value) {
    if (value != this.statsUpdateInterval) {
      statsUpdateInterval = value;
      evtStatsUpdateIntervalChanged.fire();
      startUpdateTimer(value * 1000);
    }
  }

  public int getStatsUpdateInterval() {
    return statsUpdateInterval;
  }

  public void shutdown() {
    if (updateTimer != null) {
      updateTimer.cancel();
    }
  }

  public void addStatsListener(final ActionListener listener) {
    evtStatsUpdated.addListener(listener);
  }

  public void removeStatsListener(final ActionListener listener) {
    evtStatsUpdated.removeListener(listener);
  }

  public EventObserver getCollectFineGrainedStatsObserver() {
    return evtCollectFineGrainedStatsChanged.getObserver();
  }

  public EventObserver getRefreshObserver() {
    return evtRefresh.getObserver();
  }

  public EventObserver getStatsObserver() {
    return evtStatsUpdated.getObserver();
  }

  public EventObserver getStatsUpdateIntervalObserver() {
    return evtStatsUpdateIntervalChanged.getObserver();
  }

  private void updateStats() throws RemoteException {
    poolStats = pool.getStatistics(lastStatsUpdateTime);
    lastStatsUpdateTime = poolStats.getTimestamp();
    poolSizeSeries.add(poolStats.getTimestamp(), poolStats.getSize());
    minimumPoolSizeSeries.add(poolStats.getTimestamp(), pool.getMinimumPoolSize());
    maximumPoolSizeSeries.add(poolStats.getTimestamp(), pool.getMaximumPoolSize());
    inPoolSeriesMacro.add(poolStats.getTimestamp(), poolStats.getAvailable());
    inUseSeriesMacro.add(poolStats.getTimestamp(), poolStats.getInUse());
    connectionRequestsPerSecond.add(poolStats.getTimestamp(), poolStats.getRequestsPerSecond());
    delayedRequestsPerSecond.add(poolStats.getTimestamp(), poolStats.getDelayedRequestsPerSecond());
    failedRequestsPerSecond.add(poolStats.getTimestamp(), poolStats.getFailedRequestsPerSecond());
    averageCheckOutTime.add(poolStats.getTimestamp(), poolStats.getAverageGetTime(),
            poolStats.getMininumCheckOutTime(), poolStats.getMaximumCheckOutTime());
    final List<ConnectionPoolState> stats = sortAndRemoveDuplicates(poolStats.getFineGrainedStatistics());
    if (!stats.isEmpty()) {
      final XYSeries inPoolSeries = new XYSeries("Connections available in pool");
      final XYSeries inUseSeries = new XYSeries("Connections in active use");
      for (final ConnectionPoolState inPool : stats) {
        inPoolSeries.add(inPool.getTimestamp(), inPool.getSize());
        inUseSeries.add(inPool.getTimestamp(), inPool.getInUse());
      }

      this.statsCollection.removeAllSeries();
      this.statsCollection.addSeries(inPoolSeries);
      this.statsCollection.addSeries(inUseSeries);
    }
    evtStatsUpdated.fire();
  }

  private List<ConnectionPoolState> sortAndRemoveDuplicates(final List<ConnectionPoolState> stats) {
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
        try {
          updateStats();
        }
        catch (RemoteException e) {/**/}
      }
    }, delay, delay);
  }

  private static final class StateComparator implements Comparator<ConnectionPoolState>, Serializable {
    private static final long serialVersionUID = 1;
    /** {@inheritDoc} */
    public int compare(final ConnectionPoolState o1, final ConnectionPoolState o2) {
      return ((Long) o1.getTimestamp()).compareTo(o2.getTimestamp());
    }
  }
}
