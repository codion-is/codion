/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.server.monitor;

import org.jminor.common.db.database.Database;
import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.db.pool.ConnectionPoolStatistics;
import org.jminor.common.user.User;
import org.jminor.common.user.Users;
import org.jminor.framework.server.EntityConnectionServerAdmin;

import javax.sql.DataSource;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;

/**
 * A class responsible for monitoring the connection pools of a given EntityConnectionServer.
 */
public final class PoolMonitor {

  private final EntityConnectionServerAdmin server;

  private final Collection<ConnectionPoolMonitor> connectionPoolMonitors = new ArrayList<>();

  /**
   * Instantiates a new {@link PoolMonitor}
   * @param server the server
   * @throws RemoteException in case of an exception
   */
  public PoolMonitor(final EntityConnectionServerAdmin server) throws RemoteException {
    this.server = server;
    refresh();
  }

  /**
   * Refreshes the the connection pools
   * @throws RemoteException in case of an exception
   */
  public void refresh() throws RemoteException {
    for (final String username : server.getConnectionPools()) {
      connectionPoolMonitors.add(new ConnectionPoolMonitor(new MonitorPool(username, server)));
    }
  }

  /**
   * @return the avilable {@link ConnectionPoolMonitor} instances
   */
  public Collection<ConnectionPoolMonitor> getConnectionPoolInstanceMonitors() {
    return connectionPoolMonitors;
  }

  /**
   * Shuts down this pool monitor
   */
  public void shutdown() {
    for (final ConnectionPoolMonitor monitor : connectionPoolMonitors) {
      monitor.shutdown();
    }
  }

  private static final class MonitorPool implements ConnectionPool {

    private final EntityConnectionServerAdmin server;
    private final User user;

    private MonitorPool(final String username, final EntityConnectionServerAdmin server) {
      this.user = Users.user(username);
      this.server = server;
    }

    @Override
    public int getMaximumPoolSize() {
      try {
        return server.getMaximumConnectionPoolSize(user.getUsername());
      }
      catch (final RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public int getMinimumPoolSize() {
      try {
        return server.getMinimumConnectionPoolSize(user.getUsername());
      }
      catch (final RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public int getCleanupInterval() {
      try {
        return server.getConnectionPoolCleanupInterval(user.getUsername());
      }
      catch (final RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public int getConnectionTimeout() {
      try {
        return server.getPooledConnectionTimeout(user.getUsername());
      }
      catch (final RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void setMaximumPoolSize(final int value) {
      try {
        server.setMaximumConnectionPoolSize(user.getUsername(), value);
      }
      catch (final RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void setMinimumPoolSize(final int value) {
      try {
        server.setMinimumConnectionPoolSize(user.getUsername(), value);
      }
      catch (final RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void setConnectionTimeout(final int timeout) {
      try {
        server.setPooledConnectionTimeout(user.getUsername(), timeout);
      }
      catch (final RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public int getMaximumCheckOutTime() {
      try {
        return server.getMaximumPoolCheckOutTime(user.getUsername());
      }
      catch (final RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void setMaximumCheckOutTime(final int value) {
      try {
        server.setMaximumPoolCheckOutTime(user.getUsername(), value);
      }
      catch (final RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void setCleanupInterval(final int poolCleanupInterval) {
      try {
        server.setConnectionPoolCleanupInterval(user.getUsername(), poolCleanupInterval);
      }
      catch (final RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public ConnectionPoolStatistics getStatistics(final long since) {
      try {
        return server.getConnectionPoolStatistics(user.getUsername(), since);
      }
      catch (final RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public User getUser() {
      return user;
    }

    @Override
    public boolean isCollectSnapshotStatistics() {
      try {
        return server.isCollectPoolSnapshotStatistics(user.getUsername());
      }
      catch (final RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void resetStatistics() {
      try {
        server.resetConnectionPoolStatistics(user.getUsername());
      }
      catch (final RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void setCollectSnapshotStatistics(final boolean collectSnapshotStatistics) {
      try {
        server.setCollectPoolSnapshotStatistics(user.getUsername(), collectSnapshotStatistics);
      }
      catch (final RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public Database getDatabase() {
      return null;
    }

    @Override
    public Connection getConnection() {
      return null;
    }

    @Override
    public DataSource getPoolDataSource() {
      return null;
    }

    @Override
    public void close() {/*Not required*/}
  }
}
