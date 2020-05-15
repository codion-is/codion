/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.hikari.pool;

import is.codion.common.db.pool.AbstractConnectionPool;
import is.codion.common.db.pool.ConnectionFactory;
import is.codion.common.db.pool.ConnectionPool;
import is.codion.common.db.pool.ConnectionPoolProvider;
import is.codion.common.user.User;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.pool.HikariPool;
import com.zaxxer.hikari.util.DriverDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * A HikariCP connection pool based ConnectionPoolProvider implementation
 */
public final class HikariConnectionPoolProvider implements ConnectionPoolProvider {

  /**
   * Creates a HikariCP based connection pool
   * @param connectionFactory the connection factory
   * @param user the user
   * @return a connection pool
   */
  @Override
  public ConnectionPool createConnectionPool(final ConnectionFactory connectionFactory, final User user) {
    return new HikariConnectionPool(connectionFactory, user);
  }

  private static final class HikariConnectionPool extends AbstractConnectionPool<HikariPool> {

    private final HikariConfig config = new HikariConfig();

    public HikariConnectionPool(final ConnectionFactory connectionFactory, final User user) {
      super(connectionFactory, user, new DriverDataSource(connectionFactory.getUrl(), null,
              new Properties(), user.getUsername(), String.valueOf(user.getPassword())));
      config.setJdbcUrl(connectionFactory.getUrl());
      config.setAutoCommit(false);
      config.setUsername(user.getUsername());
      config.setMaximumPoolSize(ConnectionPool.DEFAULT_MAXIMUM_POOL_SIZE.get());
      config.setMinimumIdle(ConnectionPool.DEFAULT_MINIMUM_POOL_SIZE.get());
      config.setIdleTimeout(ConnectionPool.DEFAULT_IDLE_TIMEOUT.get());
      config.setJdbcUrl(connectionFactory.getUrl());
      config.setDataSource(getPoolDataSource());
      setPool(new HikariPool(config));
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
    public void setMaximumCheckOutTime(final int value) {
      config.setConnectionTimeout(value);
    }

    @Override
    protected Connection fetchConnection() throws SQLException {
      return getPool().getConnection();
    }

    @Override
    protected int getWaiting() {
      return getPool().getThreadsAwaitingConnection();
    }
  }
}
