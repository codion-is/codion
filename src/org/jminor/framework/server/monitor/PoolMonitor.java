/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.db.pool.ConnectionPoolStatistics;
import org.jminor.common.db.pool.PoolableConnection;
import org.jminor.common.model.User;
import org.jminor.framework.server.EntityDbServerAdmin;

import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * A PoolMonitor
 */
public final class PoolMonitor {

  private final EntityDbServerAdmin server;

  private final Collection<ConnectionPoolMonitor> connectionPoolMonitors = new ArrayList<ConnectionPoolMonitor>();

  public PoolMonitor(final EntityDbServerAdmin server) throws RemoteException {
    this.server = server;
    refresh();
  }

  public void refresh() throws RemoteException {
    for (final User user : server.getEnabledConnectionPools()) {
      connectionPoolMonitors.add(new ConnectionPoolMonitor(user, new MonitorPool(user, server)));
    }
  }

  public Collection<ConnectionPoolMonitor> getConnectionPoolInstanceMonitors() {
    return connectionPoolMonitors;
  }

  public void shutdown() {
    for (final ConnectionPoolMonitor monitor : connectionPoolMonitors) {
      monitor.shutdown();
    }
  }

  public void addConnectionPools(final String[] usernames) {
    throw new UnsupportedOperationException("Not implemented");
  }

  private static final class MonitorPool implements ConnectionPool {

    private final EntityDbServerAdmin server;
    private final User user;

    private MonitorPool(final User user, final EntityDbServerAdmin server) {
      this.user = user;
      this.server = server;
    }

    /** {@inheritDoc} */
    public int getMaximumPoolSize() {
      try {
        return server.getMaximumConnectionPoolSize(user);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    public int getMinimumPoolSize() {
      try {
        return server.getMinimumConnectionPoolSize(user);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    public int getPoolCleanupInterval() {
      try {
        return server.getConnectionPoolCleanupInterval(user);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    public int getMaximumRetryWaitPeriod() {
      try {
        return server.getMaximumPoolRetryWaitPeriod(user);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    public int getPooledConnectionTimeout() {
      try {
        return server.getPooledConnectionTimeout(user);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    public void setMaximumPoolSize(final int value) {
      try {
        server.setMaximumConnectionPoolSize(user, value);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    public void setMinimumPoolSize(final int value) {
      try {
        server.setMinimumConnectionPoolSize(user, value);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    public void setPooledConnectionTimeout(final int timeout) {
      try {
        server.setPooledConnectionTimeout(user, timeout);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    public void setMaximumRetryWaitPeriod(final int maximumRetryWaitPeriod) {
      try {
        server.setMaximumPoolRetryWaitPeriod(user, maximumRetryWaitPeriod);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    public int getMaximumCheckOutTime() {
      try {
        return server.getMaximumPoolCheckOutTime(user);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    public void setMaximumCheckOutTime(final int value) {
      try {
        server.setMaximumPoolCheckOutTime(user, value);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    public int getWaitTimeBeforeNewConnection() {
      throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    public void setWaitTimeBeforeNewConnection(final int value) {
      throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    public boolean isEnabled() {
      try {
        return server.isConnectionPoolEnabled(user);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    public void setEnabled(final boolean enabled) {
      try {
        server.setConnectionPoolEnabled(user, enabled);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    public void setPoolCleanupInterval(final int poolCleanupInterval) {
      try {
        server.setConnectionPoolCleanupInterval(user, poolCleanupInterval);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    public ConnectionPoolStatistics getConnectionPoolStatistics(final long since) {
      try {
        return server.getConnectionPoolStatistics(user, since);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    public User getUser() {
      return user;
    }

    /** {@inheritDoc} */
    public boolean isCollectFineGrainedStatistics() {
      try {
        return server.isCollectFineGrainedPoolStatistics(user);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    public void resetPoolStatistics() {
      try {
        server.resetConnectionPoolStatistics(user);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    public void setCollectFineGrainedStatistics(final boolean value) {
      try {
        server.setCollectFineGrainedPoolStatistics(user, value);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    public void checkInConnection(final PoolableConnection dbConnection) {}

    /** {@inheritDoc} */
    public PoolableConnection checkOutConnection() throws ClassNotFoundException, SQLException {return null;}

    /** {@inheritDoc} */
    public void close() {}
  }
}
