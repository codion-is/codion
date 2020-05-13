/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.server.monitor;

import org.jminor.common.TaskScheduler;
import org.jminor.common.db.database.Database;
import org.jminor.common.value.Value;
import org.jminor.framework.server.EntityServerAdmin;

import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;

/**
 * A DatabaseMonitor
 */
public final class DatabaseMonitor {

  private final EntityServerAdmin server;
  private final PoolMonitor poolMonitor;
  private final XYSeries queriesPerSecond = new XYSeries("Queries per second");
  private final XYSeries selectsPerSecond = new XYSeries("Selects per second");
  private final XYSeries insertsPerSecond = new XYSeries("Inserts per second");
  private final XYSeries updatesPerSecond = new XYSeries("Updates per second");
  private final XYSeries deletesPerSecond = new XYSeries("Deletes per second");
  private final XYSeriesCollection queriesPerSecondCollection = new XYSeriesCollection();
  private final TaskScheduler updateScheduler;
  private final Value<Integer> updateIntervalValue;

  /**
   * Instantiates a new {@link DatabaseMonitor} for the given server
   * @param server the server
   * @param updateRate the initial statistics update rate in seconds
   * @throws RemoteException in case of an exception
   */
  public DatabaseMonitor(final EntityServerAdmin server, final int updateRate) throws RemoteException {
    this.server = server;
    this.poolMonitor = new PoolMonitor(server, updateRate);
    this.queriesPerSecondCollection.addSeries(queriesPerSecond);
    this.queriesPerSecondCollection.addSeries(selectsPerSecond);
    this.queriesPerSecondCollection.addSeries(insertsPerSecond);
    this.queriesPerSecondCollection.addSeries(updatesPerSecond);
    this.queriesPerSecondCollection.addSeries(deletesPerSecond);
    this.updateScheduler = new TaskScheduler(() -> {
      try {
        updateStatistics();
      }
      catch (final RemoteException ignored) {/*ignored*/}
    }, updateRate, 0, TimeUnit.SECONDS).start();
    this.updateIntervalValue = new IntervalValue(updateScheduler);
  }

  /**
   * @return the connection pool monitor
   */
  public PoolMonitor getConnectionPoolMonitor() {
    return poolMonitor;
  }

  /**
   * Shuts down this database monitor
   */
  public void shutdown() {
    updateScheduler.stop();
    poolMonitor.shutdown();
  }

  /**
   * Resets all collected statistics
   */
  public void clearStatistics() {
    queriesPerSecond.clear();
    selectsPerSecond.clear();
    insertsPerSecond.clear();
    updatesPerSecond.clear();
    deletesPerSecond.clear();
  }

  /**
   * Updates the database usage statistics
   * @throws RemoteException in case of an exception
   */
  public void updateStatistics() throws RemoteException {
    final Database.Statistics dbStats = server.getDatabaseStatistics();
    queriesPerSecond.add(dbStats.getTimestamp(), dbStats.getQueriesPerSecond());
    selectsPerSecond.add(dbStats.getTimestamp(), dbStats.getSelectsPerSecond());
    insertsPerSecond.add(dbStats.getTimestamp(), dbStats.getInsertsPerSecond());
    updatesPerSecond.add(dbStats.getTimestamp(), dbStats.getUpdatesPerSecond());
    deletesPerSecond.add(dbStats.getTimestamp(), dbStats.getDeletesPerSecond());
  }

  /**
   * @return the graph series collection for the number of queries
   */
  public XYDataset getQueriesPerSecondCollection() {
    return queriesPerSecondCollection;
  }

  /**
   * @return the value controlling the update interval
   */
  public Value<Integer> getUpdateIntervalValue() {
    return updateIntervalValue;
  }
}
