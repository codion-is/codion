/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.db.pool.ConnectionPoolState;
import org.jminor.common.db.pool.ConnectionPoolStatistics;
import org.jminor.common.model.Event;
import org.jminor.common.model.User;

import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * User: Bjorn Darri<br>
 * Date: 4.12.2007<br>
 * Time: 18:20:24<br>
 */
public class ConnectionPoolMonitor {

  private final Event evtStatsUpdated = new Event();
  private final Event evtStatsUpdateIntervalChanged = new Event();
  private final Event evtCollectFineGrainedStatsChanged = new Event();
  private final Event evtRefresh = new Event();

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
  private final XYSeries delayedRequestsPerSecond = new XYSeries("Delayed requests per second");
  private final XYSeries connectionRequestsPerSecond = new XYSeries("Connection requests per second");
  private final XYSeriesCollection connectionRequestsPerSecondCollection = new XYSeriesCollection();
  private final XYSeries averageCheckOutTime = new XYSeries("Average check out time");
  private final XYSeriesCollection checkOutTimeCollection = new XYSeriesCollection();

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
    return pool.getPooledConnectionTimeout() / 1000;
  }

  public void setPooledConnectionTimeout(final int value) throws RemoteException {
    pool.setPooledConnectionTimeout(value * 1000);
  }

  public int getPoolCleanupInterval() throws RemoteException {
    return pool.getPoolCleanupInterval() / 1000;
  }

  public void setPoolCleanupInterval(final int value) throws RemoteException {
    pool.setPoolCleanupInterval(value);
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

  public XYSeriesCollection getCheckOutTimeCollection() {
    return checkOutTimeCollection;
  }

  public void resetStats() throws RemoteException {
    pool.resetPoolStatistics();
  }

  public void resetInPoolStats() {
    inPoolSeriesMacro.clear();
    inUseSeriesMacro.clear();
    connectionRequestsPerSecond.clear();
    delayedRequestsPerSecond.clear();
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

  public Event eventCollectFineGrainedStatsChanged() {
    return evtCollectFineGrainedStatsChanged;
  }

  public Event eventRefresh() {
    return evtRefresh;
  }

  public Event eventStatsUpdated() {
    return evtStatsUpdated;
  }

  public Event eventStatsUpdateIntervalChanged() {
    return evtStatsUpdateIntervalChanged;
  }

  private void updateStats() throws RemoteException {
    poolStats = pool.getConnectionPoolStatistics(lastStatsUpdateTime);
    lastStatsUpdateTime = poolStats.getTimestamp();
    poolSizeSeries.add(poolStats.getTimestamp(), poolStats.getLiveConnectionCount());
    minimumPoolSizeSeries.add(poolStats.getTimestamp(), pool.getMinimumPoolSize());
    maximumPoolSizeSeries.add(poolStats.getTimestamp(), pool.getMaximumPoolSize());
    inPoolSeriesMacro.add(poolStats.getTimestamp(), poolStats.getAvailableInPool());
    inUseSeriesMacro.add(poolStats.getTimestamp(), poolStats.getConnectionsInUse());
    connectionRequestsPerSecond.add(poolStats.getTimestamp(), poolStats.getRequestsPerSecond());
    delayedRequestsPerSecond.add(poolStats.getTimestamp(), poolStats.getRequestsDelayedPerSecond());
    averageCheckOutTime.add(poolStats.getTimestamp(), poolStats.getAverageCheckOutTime());
    final List<ConnectionPoolState> stats = sortAndRemoveDuplicates(poolStats.getPoolStatistics());
    if (stats.size() > 0) {
      final XYSeries inPoolSeries = new XYSeries("Connections available in pool");
      final XYSeries inUseSeries = new XYSeries("Connections in active use");
      for (final ConnectionPoolState inPool : stats) {
        inPoolSeries.add(inPool.getTime(), inPool.getConnectionCount());
        inUseSeries.add(inPool.getTime(), inPool.getConnectionsInUse());
      }

      this.statsCollection.removeAllSeries();
      this.statsCollection.addSeries(inPoolSeries);
      this.statsCollection.addSeries(inUseSeries);
    }
    evtStatsUpdated.fire();
  }

  private List<ConnectionPoolState> sortAndRemoveDuplicates(final List<ConnectionPoolState> stats) {
    final List<ConnectionPoolState> poolStates = new ArrayList<ConnectionPoolState>(stats.size());
    Collections.sort(poolStates, new Comparator<ConnectionPoolState>() {
      public int compare(final ConnectionPoolState o1, final ConnectionPoolState o2) {
        return ((Long) o1.getTime()).compareTo(o2.getTime());
      }
    });
    long time = -1;
    for (int i = stats.size()-1; i >= 0; i--) {
      final ConnectionPoolState state = stats.get(i);
      if (state.getTime() != time) {
        poolStates.add(state);
      }

      time = state.getTime();
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
}
