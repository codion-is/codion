/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.server.monitor;

import org.jminor.common.User;
import org.jminor.common.db.Database;
import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.db.pool.ConnectionPoolStatistics;
import org.jminor.framework.server.EntityConnectionServerAdmin;

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
    for (final User user : server.getConnectionPools()) {
      connectionPoolMonitors.add(new ConnectionPoolMonitor(new MonitorPool(user, server)));
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

    private MonitorPool(final User user, final EntityConnectionServerAdmin server) {
      this.user = user;
      this.server = server;
    }

    @Override
    public int getMaximumPoolSize() {
      try {
        return server.getMaximumConnectionPoolSize(user);
      }
      catch (final RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public int getMinimumPoolSize() {
      try {
        return server.getMinimumConnectionPoolSize(user);
      }
      catch (final RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public int getCleanupInterval() {
      try {
        return server.getConnectionPoolCleanupInterval(user);
      }
      catch (final RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public int getMaximumRetryWaitPeriod() {
      try {
        return server.getMaximumPoolRetryWaitPeriod(user);
      }
      catch (final RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public int getConnectionTimeout() {
      try {
        return server.getPooledConnectionTimeout(user);
      }
      catch (final RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void setMaximumPoolSize(final int value) {
      try {
        server.setMaximumConnectionPoolSize(user, value);
      }
      catch (final RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void setMinimumPoolSize(final int value) {
      try {
        server.setMinimumConnectionPoolSize(user, value);
      }
      catch (final RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void setConnectionTimeout(final int timeout) {
      try {
        server.setPooledConnectionTimeout(user, timeout);
      }
      catch (final RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void setMaximumRetryWaitPeriod(final int maximumRetryWaitPeriod) {
      try {
        server.setMaximumPoolRetryWaitPeriod(user, maximumRetryWaitPeriod);
      }
      catch (final RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public int getMaximumCheckOutTime() {
      try {
        return server.getMaximumPoolCheckOutTime(user);
      }
      catch (final RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void setMaximumCheckOutTime(final int value) {
      try {
        server.setMaximumPoolCheckOutTime(user, value);
      }
      catch (final RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public int getNewConnectionThreshold() {
      try {
        return server.getPoolConnectionThreshold(user);
      }
      catch (final RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void setNewConnectionThreshold(final int value) {
      try {
        server.setPoolConnectionThreshold(user, value);
      }
      catch (final RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void setCleanupInterval(final int poolCleanupInterval) {
      try {
        server.setConnectionPoolCleanupInterval(user, poolCleanupInterval);
      }
      catch (final RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public ConnectionPoolStatistics getStatistics(final long since) {
      try {
        return server.getConnectionPoolStatistics(user, since);
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
    public boolean isCollectFineGrainedStatistics() {
      try {
        return server.isCollectFineGrainedPoolStatistics(user);
      }
      catch (final RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void resetStatistics() {
      try {
        server.resetConnectionPoolStatistics(user);
      }
      catch (final RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void setCollectFineGrainedStatistics(final boolean value) {
      try {
        server.setCollectFineGrainedPoolStatistics(user, value);
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
    public void returnConnection(final Connection connection) {/*Not required*/}

    @Override
    public Connection getConnection() {return null;}

    @Override
    public void close() {/*Not required*/}
  }
}
