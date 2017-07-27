/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.hikari.pool;

import org.jminor.common.User;
import org.jminor.common.Util;
import org.jminor.common.db.Database;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.pool.AbstractConnectionPool;
import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.db.pool.ConnectionPoolProvider;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.pool.HikariPool;
import com.zaxxer.hikari.util.DriverDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * A HikariCP connection pool based ConnectionPoolProvider implementation
 */
public final class HikariConnectionPoolProvider implements ConnectionPoolProvider {

  /**
   * Creates a HikariCP based connection pool
   * @param user the user
   * @param database the underlying database
   * @return a connection pool
   */
  @Override
  public ConnectionPool createConnectionPool(final User user, final Database database) {
    return new HikariConnectionPool(user, database);
  }

  private static final class HikariConnectionPool extends AbstractConnectionPool<HikariPool> {

    private final HikariConfig config = new HikariConfig();
    private final DriverDataSource dataSource;

    public HikariConnectionPool(final User user, final Database database) {
      super(user);
      dataSource = new DriverDataSource(database.getURL(null), database.getDriverClassName(),
              new Properties(), user.getUsername(), user.getPassword());
      config.setJdbcUrl(database.getURL(null));
      config.setUsername(user.getUsername());
      config.setMaximumPoolSize(8);
      config.setMinimumIdle(4);
      config.setIdleTimeout(60000);
      config.setJdbcUrl(database.getURL(null));
      config.setDataSource(Util.initializeProxy(DataSource.class, (dataSourceProxy, dataSourceMethod, dataSourceArgs) -> {
        if ("getConnection".equals(dataSourceMethod.getName())) {
          final Connection connection = database.createConnection(user);
          getCounter().incrementConnectionsCreatedCounter();

          return Util.initializeProxy(Connection.class, (connectionProxy, connectionMethod, connectionArgs) -> {
            if ("close".equals(connectionMethod.getName())) {
              getCounter().incrementConnectionsDestroyedCounter();
            }

            return connectionMethod.invoke(connection, connectionArgs);
          });
        }

        return dataSourceMethod.invoke(dataSource, dataSourceArgs);
      }));
      setPool(new HikariPool(config));
    }

    @Override
    public Connection getConnection() throws DatabaseException {
      final long nanoTime = System.nanoTime();
      try {
        getCounter().incrementRequestCounter();

        return getPool().getConnection();
      }
      catch (final SQLException e) {
        getCounter().incrementFailedRequestCounter();
        throw new DatabaseException(e, e.getMessage());
      }
      finally {
        getCounter().addCheckOutTime((System.nanoTime() - nanoTime) / 1000000);
      }
    }

    @Override
    public void returnConnection(final Connection connection) {
      try {
        connection.close();
      }
      catch (final SQLException ignored) {/*ignored*/}
    }

    @Override
    public void close() {
      try {
        getPool().shutdown();
      }
      catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    @Override
    public int getCleanupInterval() {
      return 0;
    }

    @Override
    public void setCleanupInterval(final int poolCleanupInterval) {/*non-configurable, com.zaxxer.hikari.housekeeping.periodMs*/}

    @Override
    public int getConnectionTimeout() {
      return (int) config.getIdleTimeout();
    }

    @Override
    public void setConnectionTimeout(final int timeout) {
      config.setIdleTimeout(timeout);
    }

    @Override
    public int getMaximumRetryWaitPeriod() {
      return 0;
    }

    @Override
    public void setMaximumRetryWaitPeriod(final int maximumRetryWaitPeriod) {/*not implemented*/}

    @Override
    public int getMinimumPoolSize() {
      return config.getMinimumIdle();
    }

    @Override
    public void setMinimumPoolSize(final int value) {
      config.setMinimumIdle(value);
    }

    @Override
    public int getMaximumPoolSize() {
      return config.getMaximumPoolSize();
    }

    @Override
    public void setMaximumPoolSize(final int value) {
      config.setMaximumPoolSize(value);
    }

    @Override
    protected int getSize() {
      return getPool().getIdleConnections();
    }

    @Override
    protected int getInUse() {
      return getPool().getActiveConnections();
    }

    @Override
    public int getMaximumCheckOutTime() {
      return (int) config.getConnectionTimeout();
    }

    @Override
    protected int getWaiting() {
      return getPool().getThreadsAwaitingConnection();
    }

    @Override
    public void setMaximumCheckOutTime(final int value) {
      config.setConnectionTimeout(value);
    }

    @Override
    public int getNewConnectionThreshold() {
      return 0;
    }

    @Override
    public void setNewConnectionThreshold(final int value) {/*Not implemented*/}
  }
}
