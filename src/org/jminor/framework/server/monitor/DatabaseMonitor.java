/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.db.DatabaseStatistics;
import org.jminor.common.model.Event;
import org.jminor.framework.server.EntityDbServerAdmin;

import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.rmi.RemoteException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * User: Bjorn Darri
 */
public class DatabaseMonitor {

  private final Event evtStatsUpdateIntervalChanged = new Event();

  private final EntityDbServerAdmin server;
  private final ConnectionPoolMonitor connectionPoolMonitor;
  private final XYSeries queriesPerSecond = new XYSeries("Total per second");
  private final XYSeries cachedQueriesPerSecond = new XYSeries("Cached selects per second");
  private final XYSeries selectsPerSecond = new XYSeries("Selects per second");
  private final XYSeries insertsPerSecond = new XYSeries("Inserts per second");
  private final XYSeries updatesPerSecond = new XYSeries("Updates per second");
  private final XYSeries deletesPerSecond = new XYSeries("Deletes per second");
  private final XYSeriesCollection queriesPerSecondCollection = new XYSeriesCollection();

  private Timer updateTimer;
  private int statsUpdateInterval;

  public DatabaseMonitor(final EntityDbServerAdmin server) throws RemoteException {
    this.server = server;
    this.connectionPoolMonitor = new ConnectionPoolMonitor(server);
    this.queriesPerSecondCollection.addSeries(queriesPerSecond);
    this.queriesPerSecondCollection.addSeries(cachedQueriesPerSecond);
    this.queriesPerSecondCollection.addSeries(selectsPerSecond);
    this.queriesPerSecondCollection.addSeries(insertsPerSecond);
    this.queriesPerSecondCollection.addSeries(updatesPerSecond);
    this.queriesPerSecondCollection.addSeries(deletesPerSecond);
    updateStats();
    setStatsUpdateInterval(3);
  }

  public void setStatsUpdateInterval(final int value) {
    if (value != this.statsUpdateInterval) {
      this.statsUpdateInterval = value;
      evtStatsUpdateIntervalChanged.fire();
      startUpdateTimer(value * 1000);
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
    selectsPerSecond.clear();
    insertsPerSecond.clear();
    updatesPerSecond.clear();
    deletesPerSecond.clear();
  }

  public void updateStats() throws RemoteException {
    final DatabaseStatistics dbStats = server.getDatabaseStatistics();
    queriesPerSecond.add(dbStats.getTimestamp(), dbStats.getQueriesPerSecond());
    cachedQueriesPerSecond.add(dbStats.getTimestamp(), dbStats.getCachedQueriesPerSecond());
    selectsPerSecond.add(dbStats.getTimestamp(), dbStats.getSelectsPerSecond());
    insertsPerSecond.add(dbStats.getTimestamp(), dbStats.getInsertsPerSecond());
    updatesPerSecond.add(dbStats.getTimestamp(), dbStats.getUpdatesPerSecond());
    deletesPerSecond.add(dbStats.getTimestamp(), dbStats.getDeletesPerSecond());
  }

  public XYSeriesCollection getQueriesPerSecondCollection() {
    return queriesPerSecondCollection;
  }

  public Event eventStatsUpdateIntervalChanged() {
    return evtStatsUpdateIntervalChanged;
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
