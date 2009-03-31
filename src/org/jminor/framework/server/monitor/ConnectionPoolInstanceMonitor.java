/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.db.ConnectionPoolSettings;
import org.jminor.common.db.ConnectionPoolState;
import org.jminor.common.db.User;
import org.jminor.common.model.Event;
import org.jminor.framework.server.IEntityDbRemoteServerAdmin;

import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.tree.DefaultMutableTreeNode;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * User: Björn Darri
 * Date: 4.12.2007
 * Time: 18:20:24
 */
public class ConnectionPoolInstanceMonitor extends DefaultMutableTreeNode {

  public final Event evtStatsUpdated = new Event("ConnectionPoolInstanceMonitor.evtStatsUpdated");
  public final Event evtStatsUpdateIntervalChanged = new Event("ConnectionPoolInstanceMonitor.statsUpdateIntervalChanged");
  public final Event evtRefresh = new Event("ConnectionPoolInstanceMonitor.evtRefresh");

  private final User user;
  private final IEntityDbRemoteServerAdmin server;
  private ConnectionPoolSettings poolStats;

  private final XYSeries poolSizeSeries = new XYSeries("Pool size");
  private final XYSeries minimumPoolSizeSeries = new XYSeries("Minimum size");
  private final XYSeries maximumPoolSizeSeries = new XYSeries("Maximum size");
  private final XYSeries inPoolSeriesMacro = new XYSeries("In pool");
  private final XYSeries inUseSeriesMacro = new XYSeries("In use");
  private final XYSeriesCollection statsCollection = new XYSeriesCollection();
  private final XYSeriesCollection macroStatsCollection = new XYSeriesCollection();
  private final XYSeries delayedRequestsPerSecond = new XYSeries("Delayed requests per second");
  private final XYSeries connectionRequestsPerSecond = new XYSeries("Connection requests per second");
  private final XYSeries queriesPerSecond = new XYSeries("Queries per second");
  private final XYSeries cachedQueriesPerSecond = new XYSeries("Cached queries per second");
  private final XYSeriesCollection connectionRequestsPerSecondCollection = new XYSeriesCollection();
  private long lastStatsUpdateTime = 0;

  private Timer updateTimer;
  private int statsUpdateInterval;

  public ConnectionPoolInstanceMonitor(final User user, final IEntityDbRemoteServerAdmin server) throws RemoteException {
    this.user = user;
    this.server = server;
    this.macroStatsCollection.addSeries(inPoolSeriesMacro);
    this.macroStatsCollection.addSeries(inUseSeriesMacro);
    this.macroStatsCollection.addSeries(poolSizeSeries);
    this.macroStatsCollection.addSeries(minimumPoolSizeSeries);
    this.macroStatsCollection.addSeries(maximumPoolSizeSeries);
    this.connectionRequestsPerSecondCollection.addSeries(queriesPerSecond);
    this.connectionRequestsPerSecondCollection.addSeries(cachedQueriesPerSecond);
    this.connectionRequestsPerSecondCollection.addSeries(connectionRequestsPerSecond);
    this.connectionRequestsPerSecondCollection.addSeries(delayedRequestsPerSecond);
    updateStats();
    setStatsUpdateInterval(3);
  }

  public ConnectionPoolSettings getConnectionPoolStats() {
    return poolStats;
  }

  public int getPooledConnectionTimout() throws RemoteException {
    return server.getConnectionPoolSettings(user, -1).getPooledConnectionTimeout()/1000;
  }

  public void setPooledConnectionTimout(final int value) throws RemoteException {
    final ConnectionPoolSettings settings = server.getConnectionPoolSettings(user, -1);
    settings.setPooledConnectionTimeout(value*1000);
    server.setConnectionPoolSettings(settings);
  }

  public int getMinimumPoolSize() {
    return poolStats.getMinimumPoolSize();
  }

  public void setMinimumPoolSize(final int value) throws RemoteException {
    final ConnectionPoolSettings settings = server.getConnectionPoolSettings(user, -1);
    settings.setMinimumPoolSize(value);
    server.setConnectionPoolSettings(settings);
  }

  public int getMaximumPoolSize() {
    return poolStats.getMaximumPoolSize();
  }

  public void setMaximumPoolSize(final int value) throws RemoteException {
    final ConnectionPoolSettings settings = server.getConnectionPoolSettings(user, -1);
    settings.setMaximumPoolSize(value);
    server.setConnectionPoolSettings(settings);
  }

  public boolean datasetContainsData() {
    return statsCollection.getSeriesCount() > 0
            && statsCollection.getSeries(0).getItemCount() > 0
            && statsCollection.getSeries(1).getItemCount() > 0;
  }

  public XYDataset getInPoolDataSet() {
    final XYSeriesCollection ret = new XYSeriesCollection();
    ret.addSeries(statsCollection.getSeries(0));
    ret.addSeries(statsCollection.getSeries(1));

    return ret;
  }

  public XYDataset getInPoolDataSetMacro() {
    return macroStatsCollection;
  }

  public XYDataset getRequestsPerSecondDataSet() {
    return connectionRequestsPerSecondCollection;
  }

  public void resetStats() throws RemoteException {
    server.resetConnectionPoolStatistics(user);
  }

  public void resetInPoolStats() {
    inPoolSeriesMacro.clear();
    inUseSeriesMacro.clear();
    connectionRequestsPerSecond.clear();
    delayedRequestsPerSecond.clear();
    queriesPerSecond.clear();
    cachedQueriesPerSecond.clear();
    poolSizeSeries.clear();
    minimumPoolSizeSeries.clear();
    maximumPoolSizeSeries.clear();
  }

  public void setStatsUpdateInterval(final int value) {
    if (value != this.statsUpdateInterval) {
      this.statsUpdateInterval = value;
      evtStatsUpdateIntervalChanged.fire();
      startUpdateTimer(value*1000);
    }
  }

  public int getStatsUpdateInterval() {
    return statsUpdateInterval;
  }

  public String toString() {
    return "Connection pool: " + user.toString();
  }

  public void updateStats() throws RemoteException {
    poolStats = server.getConnectionPoolSettings(user, lastStatsUpdateTime);
    lastStatsUpdateTime = poolStats.getTimestamp();
    poolSizeSeries.add(poolStats.getTimestamp(), poolStats.getLiveConnectionCount());
    minimumPoolSizeSeries.add(poolStats.getTimestamp(), poolStats.getMinimumPoolSize());
    maximumPoolSizeSeries.add(poolStats.getTimestamp(), poolStats.getMaximumPoolSize());
    inPoolSeriesMacro.add(poolStats.getTimestamp(), poolStats.getAvailableInPool());
    inUseSeriesMacro.add(poolStats.getTimestamp(), poolStats.getConnectionsInUse());
    connectionRequestsPerSecond.add(poolStats.getTimestamp(), poolStats.getRequestsPerSecond());
    delayedRequestsPerSecond.add(poolStats.getTimestamp(), poolStats.getRequestsDelayedPerSecond());
    queriesPerSecond.add(poolStats.getTimestamp(), poolStats.getQueriesPerSecond());
    cachedQueriesPerSecond.add(poolStats.getTimestamp(), poolStats.getCachedQueriesPerSecond());
    final List<ConnectionPoolState> stats = sortAndRemoveDuplicates(poolStats.getPoolStatistics());
    if (stats.size() > 0) {
      final XYSeries inPoolSeries = new XYSeries("Connections available in pool");
      final XYSeries inUseSeries = new XYSeries("Connections in active use");
      for (final ConnectionPoolState inPool : stats) {
        inPoolSeries.add(inPool.time, inPool.connectionCount);
        inUseSeries.add(inPool.time, inPool.inUse);
      }

      this.statsCollection.removeAllSeries();
      this.statsCollection.addSeries(inPoolSeries);
      this.statsCollection.addSeries(inUseSeries);
    }
    evtStatsUpdated.fire();
  }

  private List<ConnectionPoolState> sortAndRemoveDuplicates(final List<ConnectionPoolState> stats) {
    final List<ConnectionPoolState> ret = new ArrayList<ConnectionPoolState>(stats.size());
    Collections.sort(ret);
    long time = -1;
    for (int i = stats.size()-1; i >= 0; i--) {
      final ConnectionPoolState state = stats.get(i);
      if (state.time != time)
        ret.add(state);

      time = state.time;
    }

    return ret;
  }

  private void startUpdateTimer(final int delay) {
    if (delay <= 0)
      return;

    if (updateTimer != null)
      updateTimer.cancel();
    updateTimer = new Timer(false);
    updateTimer.schedule(new TimerTask() {
      public void run() {
        try {
          updateStats();
        }
        catch (RemoteException e) {
          e.printStackTrace();
        }
      }
    }, delay, delay);
  }
}
