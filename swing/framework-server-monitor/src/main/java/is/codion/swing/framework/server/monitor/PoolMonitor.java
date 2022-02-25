/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.server.monitor;

import is.codion.common.db.pool.ConnectionPoolStatistics;
import is.codion.common.db.pool.ConnectionPoolWrapper;
import is.codion.common.user.User;
import is.codion.framework.server.EntityServerAdmin;

import javax.sql.DataSource;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;

/**
 * A class responsible for monitoring the connection pools of a given EntityServer.
 */
public final class PoolMonitor {

  private final EntityServerAdmin server;

  private final Collection<ConnectionPoolMonitor> connectionPoolMonitors = new ArrayList<>();

  /**
   * Instantiates a new {@link PoolMonitor}
   * @param server the server
   * @param updateRate the initial statistics update rate in seconds
   * @throws RemoteException in case of an exception
   */
  public PoolMonitor(final EntityServerAdmin server, final int updateRate) throws RemoteException {
    this.server = server;
    for (final String username : this.server.getConnectionPoolUsernames()) {
      connectionPoolMonitors.add(new ConnectionPoolMonitor(new MonitorPoolWrapper(username, this.server), updateRate));
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

  private static final class MonitorPoolWrapper implements ConnectionPoolWrapper {

    private final EntityServerAdmin server;
    private final User user;

    private MonitorPoolWrapper(final String username, final EntityServerAdmin server) {
      this.user = User.user(username);
      this.server = server;
    }

    @Override
    public int getMaximumPoolSize() {
      try {
        return server.getMaximumConnectionPoolSize(user.getUsername());
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public int getMinimumPoolSize() {
      try {
        return server.getMinimumConnectionPoolSize(user.getUsername());
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public int getCleanupInterval() {
      try {
        return server.getConnectionPoolCleanupInterval(user.getUsername());
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public int getConnectionTimeout() {
      try {
        return server.getPooledConnectionTimeout(user.getUsername());
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void setMaximumPoolSize(final int value) {
      try {
        server.setMaximumConnectionPoolSize(user.getUsername(), value);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void setMinimumPoolSize(final int value) {
      try {
        server.setMinimumConnectionPoolSize(user.getUsername(), value);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void setConnectionTimeout(final int timeout) {
      try {
        server.setPooledConnectionTimeout(user.getUsername(), timeout);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public int getMaximumCheckOutTime() {
      try {
        return server.getMaximumPoolCheckOutTime(user.getUsername());
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void setMaximumCheckOutTime(final int value) {
      try {
        server.setMaximumPoolCheckOutTime(user.getUsername(), value);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void setCleanupInterval(final int poolCleanupInterval) {
      try {
        server.setConnectionPoolCleanupInterval(user.getUsername(), poolCleanupInterval);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public ConnectionPoolStatistics getStatistics(final long since) {
      try {
        return server.getConnectionPoolStatistics(user.getUsername(), since);
      }
      catch (RemoteException e) {
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
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void setCollectSnapshotStatistics(final boolean collectSnapshotStatistics) {
      try {
        server.setCollectPoolSnapshotStatistics(user.getUsername(), collectSnapshotStatistics);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public boolean isCollectCheckOutTimes() {
      try {
        return server.isCollectPoolCheckOutTimes(user.getUsername());
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void setCollectCheckOutTimes(final boolean collectCheckOutTimes) {
      try {
        server.setCollectPoolCheckOutTimes(user.getUsername(), collectCheckOutTimes);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void resetStatistics() {
      try {
        server.resetConnectionPoolStatistics(user.getUsername());
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public Connection getConnection(final User user) {
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
