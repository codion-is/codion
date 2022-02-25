/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.tomcat.pool;

import is.codion.common.db.pool.AbstractConnectionPoolWrapper;
import is.codion.common.db.pool.ConnectionFactory;
import is.codion.common.db.pool.ConnectionPoolFactory;
import is.codion.common.db.pool.ConnectionPoolWrapper;
import is.codion.common.user.User;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.apache.tomcat.jdbc.pool.Validator;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * A Tomcat connection pool based {@link ConnectionPoolFactory} implementation
 */
public final class TomcatConnectionPoolFactory implements ConnectionPoolFactory {

  /**
   * Creates a Tomcat based connection pool wrapper
   * @param connectionFactory the connection factory
   * @param user the user
   * @return a connection pool
   */
  @Override
  public ConnectionPoolWrapper createConnectionPoolWrapper(final ConnectionFactory connectionFactory, final User user) {
    return new DataSourceWrapper(connectionFactory, user, createDataSource(user, connectionFactory));
  }

  private static DataSource createDataSource(final User user, final ConnectionFactory connectionFactory) {
    PoolProperties pp = new PoolProperties();
    pp.setUrl(connectionFactory.getUrl());
    pp.setDefaultAutoCommit(false);
    pp.setName(user.getUsername());
    //Codion does not validate connections coming from a connection pool
    pp.setTestOnBorrow(true);
    pp.setValidator(new ConnectionValidator(connectionFactory));
    pp.setMaxActive(ConnectionPoolWrapper.DEFAULT_MAXIMUM_POOL_SIZE.get());
    pp.setInitialSize(ConnectionPoolWrapper.DEFAULT_MAXIMUM_POOL_SIZE.get());
    pp.setMaxIdle(ConnectionPoolWrapper.DEFAULT_MAXIMUM_POOL_SIZE.get());
    pp.setMinIdle(ConnectionPoolWrapper.DEFAULT_MINIMUM_POOL_SIZE.get());
    pp.setSuspectTimeout(ConnectionPoolWrapper.DEFAULT_IDLE_TIMEOUT.get() / 1000);

    return new DataSource(pp);
  }

  private static final class DataSourceWrapper extends AbstractConnectionPoolWrapper<DataSource> {

    private DataSourceWrapper(final ConnectionFactory connectionFactory, final User user, final DataSource dataSource) {
      super(connectionFactory, user, dataSource);
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

    private final ConnectionFactory connectionFactory;

    private ConnectionValidator(final ConnectionFactory connectionFactory) {
      this.connectionFactory = connectionFactory;
    }

    @Override
    public boolean validate(final Connection connection, final int i) {
      return connectionFactory.isConnectionValid(connection);
    }
  }
}
