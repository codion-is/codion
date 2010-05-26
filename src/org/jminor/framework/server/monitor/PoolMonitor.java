/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.db.DbConnection;
import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.db.pool.ConnectionPoolSettings;
import org.jminor.common.db.pool.ConnectionPoolStatistics;
import org.jminor.common.model.User;
import org.jminor.framework.server.EntityDbServerAdmin;

import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * User: Bjorn Darri<br>
 * Date: 10.12.2007<br>
 * Time: 15:27:24<br>
 */
public class PoolMonitor {

  private final EntityDbServerAdmin server;

  private Collection<ConnectionPoolMonitor> connectionPoolMonitors = new ArrayList<ConnectionPoolMonitor>();

  public PoolMonitor(final EntityDbServerAdmin server) throws RemoteException {
    this.server = server;
    refresh();
  }

  public void refresh() throws RemoteException {
    for (final ConnectionPoolSettings settings : server.getEnabledConnectionPools())
      connectionPoolMonitors.add(new ConnectionPoolMonitor(settings.getUser(), new ConnectionPool() {
        public ConnectionPoolSettings getConnectionPoolSettings() {
          try {
            return server.getConnectionPoolSettings(settings.getUser());
          }
          catch (RemoteException e) {
            throw new RuntimeException(e);
          }
        }

        public ConnectionPoolStatistics getConnectionPoolStatistics(long since) {
          try {
            return server.getConnectionPoolStatistics(settings.getUser(), since);
          }
          catch (RemoteException e) {
            throw new RuntimeException(e);
          }
        }

        public User getUser() {
          return settings.getUser();
        }

        public boolean isCollectFineGrainedStatistics() {
          try {
            return server.isCollectFineGrainedPoolStatistics(settings.getUser());
          }
          catch (RemoteException e) {
            throw new RuntimeException(e);
          }
        }

        public void resetPoolStatistics() {
          try {
            server.resetConnectionPoolStatistics(settings.getUser());
          }
          catch (RemoteException e) {
            throw new RuntimeException(e);
          }
        }

        public void setCollectFineGrainedStatistics(boolean value) {
          try {
            server.setCollectFineGrainedPoolStatistics(settings.getUser(), value);
          }
          catch (RemoteException e) {
            throw new RuntimeException(e);
          }
        }

        public void setConnectionPoolSettings(ConnectionPoolSettings settings)  {
          try {
            server.setConnectionPoolSettings(settings);
          }
          catch (RemoteException e) {
            throw new RuntimeException(e);
          }
        }
        public void checkInConnection(DbConnection dbConnection) {}

        public DbConnection checkOutConnection() throws ClassNotFoundException, SQLException {return null;}
      }));
  }

  public void setConnectionPoolSettings(final ConnectionPoolSettings settings) throws RemoteException {
    server.setConnectionPoolSettings(settings);
    refresh();
  }

  public void addConnectionPools(final String[] strings) throws RemoteException {
    for (final String username : strings)
      setConnectionPoolSettings(ConnectionPoolSettings.getDefault(new User(username.trim(), null)));
  }

  public Collection<ConnectionPoolMonitor> getConnectionPoolInstanceMonitors() {
    return connectionPoolMonitors;
  }

  public void shutdown() {
    System.out.println("PoolMonitor shutdown");
    for (final ConnectionPoolMonitor monitor : connectionPoolMonitors)
      monitor.shutdown();
  }
}
