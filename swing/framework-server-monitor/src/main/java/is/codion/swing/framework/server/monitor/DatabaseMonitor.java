/*
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.server.monitor;

import is.codion.common.db.database.Database;
import is.codion.common.scheduler.TaskScheduler;
import is.codion.common.value.Value;
import is.codion.framework.server.EntityServerAdmin;

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
  public DatabaseMonitor(EntityServerAdmin server, int updateRate) throws RemoteException {
    this.server = server;
    this.poolMonitor = new PoolMonitor(server, updateRate);
    this.queriesPerSecondCollection.addSeries(queriesPerSecond);
    this.queriesPerSecondCollection.addSeries(selectsPerSecond);
    this.queriesPerSecondCollection.addSeries(insertsPerSecond);
    this.queriesPerSecondCollection.addSeries(updatesPerSecond);
    this.queriesPerSecondCollection.addSeries(deletesPerSecond);
    this.updateScheduler = TaskScheduler.builder(this::doUpdateStatistics)
            .interval(updateRate)
            .timeUnit(TimeUnit.SECONDS)
            .start();
    this.updateIntervalValue = Value.value(updateScheduler::getInterval, updateScheduler::setInterval, 0);
  }

  /**
   * @return the connection pool monitor
   */
  public PoolMonitor connectionPoolMonitor() {
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
    Database.Statistics dbStats = server.databaseStatistics();
    queriesPerSecond.add(dbStats.timestamp(), dbStats.queriesPerSecond());
    selectsPerSecond.add(dbStats.timestamp(), dbStats.selectsPerSecond());
    insertsPerSecond.add(dbStats.timestamp(), dbStats.insertsPerSecond());
    updatesPerSecond.add(dbStats.timestamp(), dbStats.updatesPerSecond());
    deletesPerSecond.add(dbStats.timestamp(), dbStats.deletesPerSecond());
  }

  /**
   * @return the graph series collection for the number of queries
   */
  public XYDataset queriesPerSecondCollection() {
    return queriesPerSecondCollection;
  }

  /**
   * @return the value controlling the update interval
   */
  public Value<Integer> updateIntervalValue() {
    return updateIntervalValue;
  }

  private void doUpdateStatistics() {
    try {
      updateStatistics();
    }
    catch (RemoteException ignored) {/*ignored*/}
  }
}
