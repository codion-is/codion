/*
 * Copyright (c) 2013 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.hikari.pool;

import is.codion.common.db.pool.AbstractConnectionPoolWrapper;
import is.codion.common.db.pool.ConnectionFactory;
import is.codion.common.db.pool.ConnectionPoolFactory;
import is.codion.common.db.pool.ConnectionPoolWrapper;
import is.codion.common.user.User;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.pool.HikariPool;
import com.zaxxer.hikari.util.DriverDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * A HikariCP connection pool based {@link ConnectionPoolFactory} implementation
 */
public final class HikariConnectionPoolFactory implements ConnectionPoolFactory {

  /**
   * Creates a HikariCP based connection pool wrapper
   * @param connectionFactory the connection factory
   * @param user the user
   * @return a connection pool
   */
  @Override
  public ConnectionPoolWrapper createConnectionPoolWrapper(ConnectionFactory connectionFactory, User user) {
    return new HikariConnectionPoolWrapper(connectionFactory, user);
  }

  private static final class HikariConnectionPoolWrapper extends AbstractConnectionPoolWrapper<HikariPool> {

    private final HikariConfig config = new HikariConfig();

    public HikariConnectionPoolWrapper(ConnectionFactory connectionFactory, User user) {
      super(connectionFactory, user, new DriverDataSource(connectionFactory.getUrl(), null,
              new Properties(), user.getUsername(), String.valueOf(user.getPassword())));
      config.setJdbcUrl(connectionFactory.getUrl());
      config.setAutoCommit(false);
      config.setUsername(user.getUsername());
      config.setMaximumPoolSize(ConnectionPoolWrapper.DEFAULT_MAXIMUM_POOL_SIZE.get());
      config.setMinimumIdle(ConnectionPoolWrapper.DEFAULT_MINIMUM_POOL_SIZE.get());
      config.setIdleTimeout(ConnectionPoolWrapper.DEFAULT_IDLE_TIMEOUT.get());
      config.setDataSource(getPoolDataSource());
      setPool(new HikariPool(config));
    }

    @Override
    public void close() {
      try {
        getPool().shutdown();
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    @Override
    public int getCleanupInterval() {
      return 0;
    }

    @Override
    public void setCleanupInterval(int poolCleanupInterval) {/*non-configurable, com.zaxxer.hikari.housekeeping.periodMs*/}

    @Override
    public int getIdleConnectionTimeout() {
      return (int) config.getIdleTimeout();
    }

    @Override
    public void setIdleConnectionTimeout(int idleConnectionTimeout) {
      config.setIdleTimeout(idleConnectionTimeout);
    }

    @Override
    public int getMinimumPoolSize() {
      return config.getMinimumIdle();
    }

    @Override
    public void setMinimumPoolSize(int minimumPoolSize) {
      config.setMinimumIdle(minimumPoolSize);
    }

    @Override
    public int getMaximumPoolSize() {
      return config.getMaximumPoolSize();
    }

    @Override
    public void setMaximumPoolSize(int maximumPoolSize) {
      config.setMaximumPoolSize(maximumPoolSize);
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
    public void setMaximumCheckOutTime(int maximumCheckOutTime) {
      config.setConnectionTimeout(maximumCheckOutTime);
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
