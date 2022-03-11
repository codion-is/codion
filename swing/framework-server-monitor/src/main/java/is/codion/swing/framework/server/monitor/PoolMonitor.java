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
  public PoolMonitor(EntityServerAdmin server, int updateRate) throws RemoteException {
    this.server = server;
    for (String username : this.server.getConnectionPoolUsernames()) {
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
    for (ConnectionPoolMonitor monitor : connectionPoolMonitors) {
      monitor.shutdown();
    }
  }

  private static final class MonitorPoolWrapper implements ConnectionPoolWrapper {

    private final EntityServerAdmin server;
    private final User user;

    private MonitorPoolWrapper(String username, EntityServerAdmin server) {
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
    public void setMaximumPoolSize(int maximumPoolSize) {
      if (maximumPoolSize < 0) {
        throw new IllegalArgumentException("Maximum pool size must be a positive integer");
      }
      if (maximumPoolSize < getMinimumPoolSize()) {
        throw new IllegalArgumentException("Maximum pool size must be equal to or exceed minimum pool size");
      }
      try {
        server.setMaximumConnectionPoolSize(user.getUsername(), maximumPoolSize);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void setMinimumPoolSize(int minimumPoolSize) {
      if (minimumPoolSize < 0) {
        throw new IllegalArgumentException("Minimum pool size must be a positive integer");
      }
      if (minimumPoolSize > getMaximumPoolSize()) {
        throw new IllegalArgumentException("Minimum pool size equal to or below maximum pool size time");
      }
      try {
        server.setMinimumConnectionPoolSize(user.getUsername(), minimumPoolSize);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void setConnectionTimeout(int timeout) {
      if (timeout < 0) {
        throw new IllegalArgumentException("Timeout must be a positive integer");
      }
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
    public void setMaximumCheckOutTime(int maximumCheckOutTime) {
      if (maximumCheckOutTime < 0) {
        throw new IllegalArgumentException("Maximum check out time must be a positive integer");
      }
      try {
        server.setMaximumPoolCheckOutTime(user.getUsername(), maximumCheckOutTime);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void setCleanupInterval(int poolCleanupInterval) {
      if (poolCleanupInterval < 0) {
        throw new IllegalArgumentException("Cleanup interval must be a positive integer");
      }
      try {
        server.setConnectionPoolCleanupInterval(user.getUsername(), poolCleanupInterval);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public ConnectionPoolStatistics getStatistics(long since) {
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
    public void setCollectSnapshotStatistics(boolean collectSnapshotStatistics) {
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
    public void setCollectCheckOutTimes(boolean collectCheckOutTimes) {
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
    public Connection getConnection(User user) {
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
