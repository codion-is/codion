/*
 * Copyright (c) 2013 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
  public ConnectionPoolWrapper createConnectionPoolWrapper(ConnectionFactory connectionFactory, User user) {
    return new DataSourceWrapper(connectionFactory, user, createDataSource(user, connectionFactory));
  }

  private static DataSource createDataSource(User user, ConnectionFactory connectionFactory) {
    PoolProperties pp = new PoolProperties();
    pp.setUrl(connectionFactory.url());
    pp.setDefaultAutoCommit(false);
    pp.setName(user.username());
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

    private DataSourceWrapper(ConnectionFactory connectionFactory, User user, DataSource dataSource) {
      super(connectionFactory, user, dataSource);
      dataSource.setDataSource(poolDataSource());
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
    public void setCleanupInterval(int poolCleanupInterval) {
      getPool().setTimeBetweenEvictionRunsMillis(poolCleanupInterval);
    }

    @Override
    public int getIdleConnectionTimeout() {
      return getPool().getSuspectTimeout() * 1000;
    }

    @Override
    public void setIdleConnectionTimeout(int idleConnectionTimeout) {
      getPool().setSuspectTimeout(idleConnectionTimeout / 1000);
    }

    @Override
    public int getMinimumPoolSize() {
      return getPool().getMinIdle();
    }

    @Override
    public void setMinimumPoolSize(int minimumPoolSize) {
      getPool().setMinIdle(minimumPoolSize);
    }

    @Override
    public int getMaximumPoolSize() {
      return getPool().getMaxActive();
    }

    @Override
    public void setMaximumPoolSize(int maximumPoolSize) {
      getPool().setMaxActive(maximumPoolSize);
      getPool().setMaxIdle(maximumPoolSize);
    }

    @Override
    public int getMaximumCheckOutTime() {
      return getPool().getMaxWait();
    }

    @Override
    public void setMaximumCheckOutTime(int maximumCheckOutTime) {
      getPool().setMaxWait(maximumCheckOutTime);
    }

    @Override
    protected Connection fetchConnection() throws SQLException {
      return getPool().getConnection();
    }

    @Override
    protected int available() {
      return getPool().getSize() - getPool().getActive();
    }

    @Override
    protected int inUse() {
      return getPool().getActive();
    }

    @Override
    protected int waiting() {
      return getPool().getWaitCount();
    }
  }

  private static final class ConnectionValidator implements Validator {

    private final ConnectionFactory connectionFactory;

    private ConnectionValidator(ConnectionFactory connectionFactory) {
      this.connectionFactory = connectionFactory;
    }

    @Override
    public boolean validate(Connection connection, int i) {
      return connectionFactory.isConnectionValid(connection);
    }
  }
}
