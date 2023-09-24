/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson.
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

import static java.util.Objects.requireNonNull;

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
  private final XYSeries otherPerSecond = new XYSeries("Other per second");
  private final XYSeriesCollection queriesPerSecondCollection = new XYSeriesCollection();
  private final TaskScheduler updateScheduler;

  /**
   * Instantiates a new {@link DatabaseMonitor} for the given server
   * @param server the server
   * @param updateRate the initial statistics update rate in seconds
   * @throws RemoteException in case of an exception
   */
  public DatabaseMonitor(EntityServerAdmin server, int updateRate) throws RemoteException {
    this.server = requireNonNull(server);
    this.poolMonitor = new PoolMonitor(server, updateRate);
    this.queriesPerSecondCollection.addSeries(queriesPerSecond);
    this.queriesPerSecondCollection.addSeries(selectsPerSecond);
    this.queriesPerSecondCollection.addSeries(insertsPerSecond);
    this.queriesPerSecondCollection.addSeries(updatesPerSecond);
    this.queriesPerSecondCollection.addSeries(deletesPerSecond);
    this.queriesPerSecondCollection.addSeries(otherPerSecond);
    this.updateScheduler = TaskScheduler.builder(this::doUpdateStatistics)
            .interval(updateRate, TimeUnit.SECONDS)
            .start();
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
    otherPerSecond.clear();
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
    otherPerSecond.add(dbStats.timestamp(), dbStats.otherPerSecond());
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
  public Value<Integer> updateInterval() {
    return updateScheduler.interval();
  }

  private void doUpdateStatistics() {
    try {
      updateStatistics();
    }
    catch (RemoteException ignored) {/*ignored*/}
  }
}
