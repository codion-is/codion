/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.tomcat.pool;

import org.jminor.common.db.pool.AbstractConnectionPool;
import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.db.pool.ConnectionPoolProvider;
import org.jminor.common.db.pool.ConnectionProvider;
import org.jminor.common.user.User;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.apache.tomcat.jdbc.pool.Validator;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * A Tomcat connection pool based ConnectionPoolProvider implementation
 */
public final class TomcatConnectionPoolProvider implements ConnectionPoolProvider {

  /**
   * Creates a Tomcat based connection pool
   * @param connectionProvider the connection provider
   * @param user the user
   * @return a connection pool
   */
  @Override
  public ConnectionPool createConnectionPool(final ConnectionProvider connectionProvider, final User user) {
    return new DataSourceWrapper(connectionProvider, user, createDataSource(user, connectionProvider));
  }

  private static DataSource createDataSource(final User user, final ConnectionProvider connectionProvider) {
    final PoolProperties pp = new PoolProperties();
    pp.setUrl(connectionProvider.getUrl());
    pp.setDefaultAutoCommit(false);
    pp.setName(user.getUsername());
    //JMinor does not validate connections coming from a connection pool
    pp.setTestOnBorrow(true);
    pp.setValidator(new ConnectionValidator(connectionProvider));
    pp.setMaxActive(ConnectionPool.DEFAULT_MAXIMUM_POOL_SIZE.get());
    pp.setInitialSize(ConnectionPool.DEFAULT_MAXIMUM_POOL_SIZE.get());
    pp.setMaxIdle(ConnectionPool.DEFAULT_MAXIMUM_POOL_SIZE.get());
    pp.setMinIdle(ConnectionPool.DEFAULT_MINIMUM_POOL_SIZE.get());
    pp.setSuspectTimeout(ConnectionPool.DEFAULT_IDLE_TIMEOUT.get() / 1000);

    return new DataSource(pp);
  }

  private static final class DataSourceWrapper extends AbstractConnectionPool<DataSource> {

    private DataSourceWrapper(final ConnectionProvider connectionProvider, final User user, final DataSource dataSource) {
      super(connectionProvider, user, dataSource);
      dataSource.setDataSource(getPoolDataSource());
      setPool(dataSource);
    }

    @Override
    public void close() {
      getPool().close();
    }

    @Override
    public int getCleanupInterval() {
      return getPool().getTimeBetweenEvictionRunsMillis();
    }

    @Override
    public void setCleanupInterval(final int poolCleanupInterval) {
      getPool().setTimeBetweenEvictionRunsMillis(poolCleanupInterval);
    }

    @Override
    public int getConnectionTimeout() {
      return getPool().getSuspectTimeout() * 1000;
    }

    @Override
    public void setConnectionTimeout(final int timeout) {
      getPool().setSuspectTimeout(timeout / 1000);
    }

    @Override
    public int getMinimumPoolSize() {
      return getPool().getMinIdle();
    }

    @Override
    public void setMinimumPoolSize(final int value) {
      getPool().setMinIdle(value);
    }

    @Override
    public int getMaximumPoolSize() {
      return getPool().getMaxActive();
    }

    @Override
    public void setMaximumPoolSize(final int value) {
      getPool().setMaxActive(value);
      getPool().setMaxIdle(value);
    }

    @Override
    public int getMaximumCheckOutTime() {
      return getPool().getMaxWait();
    }

    @Override
    public void setMaximumCheckOutTime(final int value) {
      getPool().setMaxWait(value);
    }

    @Override
    protected Connection fetchConnection() throws SQLException {
      return getPool().getConnection();
    }

    @Override
    protected int getSize() {
      return getPool().getSize() - getPool().getActive();
    }

    @Override
    protected int getInUse() {
      return getPool().getActive();
    }

    @Override
    protected int getWaiting() {
      return getPool().getWaitCount();
    }
  }

  private static final class ConnectionValidator implements Validator {

    private final ConnectionProvider connectionProvider;

    private ConnectionValidator(final ConnectionProvider connectionProvider) {
      this.connectionProvider = connectionProvider;
    }

    @Override
    public boolean validate(final Connection connection, final int i) {
      return connectionProvider.isConnectionValid(connection);
    }
  }
}
