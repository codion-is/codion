/*
 * Copyright (c) 2009, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.db.DatabaseStatistics;
import org.jminor.common.model.Event;
import org.jminor.framework.server.IEntityDbRemoteServerAdmin;

import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.rmi.RemoteException;
import java.util.Timer;
import java.util.TimerTask;

public class DatabaseMonitor {

  public final Event evtStatsUpdateIntervalChanged = new Event("DatabaseMonitor.evtStatsUpdateIntervalChanged");
  private final IEntityDbRemoteServerAdmin server;

  private final ConnectionPoolMonitor connectionPoolMonitor;
  private final XYSeries queriesPerSecond = new XYSeries("Queries per second");
  private final XYSeries cachedQueriesPerSecond = new XYSeries("Cached queries per second");
  private final XYSeriesCollection queriesPerSecondCollection = new XYSeriesCollection();
  private Timer updateTimer;
  private int statsUpdateInterval;

  public DatabaseMonitor(final IEntityDbRemoteServerAdmin server) throws RemoteException {
    this.server = server;
    this.connectionPoolMonitor = new ConnectionPoolMonitor(server);
    this.queriesPerSecondCollection.addSeries(queriesPerSecond);
    this.queriesPerSecondCollection.addSeries(cachedQueriesPerSecond);
    updateStats();
    setStatsUpdateInterval(3);
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

  public ConnectionPoolMonitor getConnectionPoolMonitor() {
    return connectionPoolMonitor;
  }

  public void shutdown() {
    if (updateTimer != null)
      updateTimer.cancel();
    connectionPoolMonitor.shutdown();
  }

  public void resetStats() {
    queriesPerSecond.clear();
    cachedQueriesPerSecond.clear();
  }

  public void updateStats() throws RemoteException {
    final DatabaseStatistics dbStats = server.getDatabaseStatistics();
    queriesPerSecond.add(dbStats.getTimestamp(), dbStats.getQueriesPerSecond());
    cachedQueriesPerSecond.add(dbStats.getTimestamp(), dbStats.getCachedQueriesPerSecond());
  }

  public XYSeriesCollection getQueriesPerSecondCollection() {
    return queriesPerSecondCollection;
  }

  private void startUpdateTimer(final int delay) {
    if (delay <= 0)
      return;

    if (updateTimer != null)
      updateTimer.cancel();
    updateTimer = new Timer(false);
    updateTimer.schedule(new TimerTask() {
      @Override
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
