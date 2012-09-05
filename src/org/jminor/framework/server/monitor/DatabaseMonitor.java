/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.db.Database;
import org.jminor.common.model.Event;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Events;
import org.jminor.common.model.Util;
import org.jminor.framework.server.EntityConnectionServerAdmin;

import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.rmi.RemoteException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A DatabaseMonitor
 */
public final class DatabaseMonitor {

  private final Event evtStatisticsUpdateIntervalChanged = Events.event();

  private final EntityConnectionServerAdmin server;
  private final PoolMonitor poolMonitor;
  private final XYSeries queriesPerSecond = new XYSeries("Queries per second");
  private final XYSeries selectsPerSecond = new XYSeries("Selects per second");
  private final XYSeries insertsPerSecond = new XYSeries("Inserts per second");
  private final XYSeries updatesPerSecond = new XYSeries("Updates per second");
  private final XYSeries deletesPerSecond = new XYSeries("Deletes per second");
  private final XYSeriesCollection queriesPerSecondCollection = new XYSeriesCollection();

  private ScheduledExecutorService updateStatisticsService;
  private int statisticsUpdateInterval;

  public DatabaseMonitor(final EntityConnectionServerAdmin server) throws RemoteException {
    this.server = server;
    this.poolMonitor = new PoolMonitor(server);
    this.queriesPerSecondCollection.addSeries(queriesPerSecond);
    this.queriesPerSecondCollection.addSeries(selectsPerSecond);
    this.queriesPerSecondCollection.addSeries(insertsPerSecond);
    this.queriesPerSecondCollection.addSeries(updatesPerSecond);
    this.queriesPerSecondCollection.addSeries(deletesPerSecond);
    updateStatistics();
    setStatisticsUpdateInterval(3);
  }

  public void setStatisticsUpdateInterval(final int value) {
    if (value < 0) {
      throw new IllegalArgumentException("Statistics update interval must be a positive integer");
    }
    if (value != this.statisticsUpdateInterval) {
      this.statisticsUpdateInterval = value;
      evtStatisticsUpdateIntervalChanged.fire();
      startUpdateStatisticsService();
    }
  }

  public int getStatisticsUpdateInterval() {
    return statisticsUpdateInterval;
  }

  public PoolMonitor getConnectionPoolMonitor() {
    return poolMonitor;
  }

  public void shutdown() {
    if (updateStatisticsService != null) {
      updateStatisticsService.shutdownNow();
    }
    poolMonitor.shutdown();
  }

  public void resetStatistics() {
    queriesPerSecond.clear();
    selectsPerSecond.clear();
    insertsPerSecond.clear();
    updatesPerSecond.clear();
    deletesPerSecond.clear();
  }

  public void updateStatistics() throws RemoteException {
    final Database.Statistics dbStats = server.getDatabaseStatistics();
    queriesPerSecond.add(dbStats.getTimestamp(), dbStats.getQueriesPerSecond());
    selectsPerSecond.add(dbStats.getTimestamp(), dbStats.getSelectsPerSecond());
    insertsPerSecond.add(dbStats.getTimestamp(), dbStats.getInsertsPerSecond());
    updatesPerSecond.add(dbStats.getTimestamp(), dbStats.getUpdatesPerSecond());
    deletesPerSecond.add(dbStats.getTimestamp(), dbStats.getDeletesPerSecond());
  }

  public XYSeriesCollection getQueriesPerSecondCollection() {
    return queriesPerSecondCollection;
  }

  public EventObserver getStatisticsUpdateIntervalObserver() {
    return evtStatisticsUpdateIntervalChanged.getObserver();
  }

  private void startUpdateStatisticsService() {
    if (updateStatisticsService != null) {
      updateStatisticsService.shutdownNow();
    }
    updateStatisticsService = Executors.newSingleThreadScheduledExecutor(new Util.DaemonThreadFactory());
    updateStatisticsService.scheduleWithFixedDelay(new Runnable() {
      @Override
      public void run() {
        try {
          updateStatistics();
        }
        catch (RemoteException ignored) {}
      }
    }, this.statisticsUpdateInterval, this.statisticsUpdateInterval, TimeUnit.SECONDS);
  }
}
