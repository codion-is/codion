/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor;

import org.jminor.common.db.DatabaseConnection;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.db.pool.ConnectionPoolStatistics;
import org.jminor.common.model.User;
import org.jminor.framework.server.EntityConnectionServerAdmin;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * A class responsible for monitoring the connection pools of a given EntityConnectionServer.
 */
public final class PoolMonitor {

  private final EntityConnectionServerAdmin server;

  private final Collection<ConnectionPoolMonitor> connectionPoolMonitors = new ArrayList<ConnectionPoolMonitor>();

  public PoolMonitor(final EntityConnectionServerAdmin server) throws RemoteException {
    this.server = server;
    refresh();
  }

  public void refresh() throws RemoteException {
    for (final User user : server.getEnabledConnectionPools()) {
      connectionPoolMonitors.add(new ConnectionPoolMonitor(new MonitorPool(user, server)));
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

    private final EntityConnectionServerAdmin server;
    private final User user;

    private MonitorPool(final User user, final EntityConnectionServerAdmin server) {
      this.user = user;
      this.server = server;
    }

    /** {@inheritDoc} */
    @Override
    public int getMaximumPoolSize() {
      try {
        return server.getMaximumConnectionPoolSize(user);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    @Override
    public int getMinimumPoolSize() {
      try {
        return server.getMinimumConnectionPoolSize(user);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    @Override
    public int getCleanupInterval() {
      try {
        return server.getConnectionPoolCleanupInterval(user);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    @Override
    public int getMaximumRetryWaitPeriod() {
      try {
        return server.getMaximumPoolRetryWaitPeriod(user);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    @Override
    public int getConnectionTimeout() {
      try {
        return server.getPooledConnectionTimeout(user);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    @Override
    public void setMaximumPoolSize(final int value) {
      try {
        server.setMaximumConnectionPoolSize(user, value);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    @Override
    public void setMinimumPoolSize(final int value) {
      try {
        server.setMinimumConnectionPoolSize(user, value);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    @Override
    public void setConnectionTimeout(final int timeout) {
      try {
        server.setPooledConnectionTimeout(user, timeout);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    @Override
    public void setMaximumRetryWaitPeriod(final int maximumRetryWaitPeriod) {
      try {
        server.setMaximumPoolRetryWaitPeriod(user, maximumRetryWaitPeriod);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    @Override
    public int getMaximumCheckOutTime() {
      try {
        return server.getMaximumPoolCheckOutTime(user);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    @Override
    public void setMaximumCheckOutTime(final int value) {
      try {
        server.setMaximumPoolCheckOutTime(user, value);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    @Override
    public int getNewConnectionThreshold() {
      try {
        return server.getPoolConnectionThreshold(user);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    @Override
    public void setNewConnectionThreshold(final int value) {
      try {
        server.setPoolConnectionThreshold(user, value);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEnabled() {
      try {
        return server.isConnectionPoolEnabled(user);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    @Override
    public void setEnabled(final boolean enabled) {
      try {
        server.setConnectionPoolEnabled(user, enabled);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    @Override
    public void setCleanupInterval(final int poolCleanupInterval) {
      try {
        server.setConnectionPoolCleanupInterval(user, poolCleanupInterval);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    @Override
    public ConnectionPoolStatistics getStatistics(final long since) {
      try {
        return server.getConnectionPoolStatistics(user, since);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    @Override
    public User getUser() {
      return user;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCollectFineGrainedStatistics() {
      try {
        return server.isCollectFineGrainedPoolStatistics(user);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    @Override
    public void resetStatistics() {
      try {
        server.resetConnectionPoolStatistics(user);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    @Override
    public void setCollectFineGrainedStatistics(final boolean value) {
      try {
        server.setCollectFineGrainedPoolStatistics(user, value);
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    @Override
    public void returnConnection(final DatabaseConnection databaseConnection) {}

    /** {@inheritDoc} */
    @Override
    public DatabaseConnection getConnection() throws DatabaseException {return null;}

    /** {@inheritDoc} */
    @Override
    public void close() {}
  }
}
