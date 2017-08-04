/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.tomcat.pool;

import org.jminor.common.User;
import org.jminor.common.Util;
import org.jminor.common.db.Database;
import org.jminor.common.db.DatabaseUtil;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.pool.AbstractConnectionPool;
import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.db.pool.ConnectionPoolProvider;

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
   * @param user the user
   * @param database the underlying database
   * @return a connection pool
   */
  @Override
  public ConnectionPool createConnectionPool(final User user, final Database database) {
    return new DataSourceWrapper(database, user, createDataSource(user, database));
  }

  private static DataSource createDataSource(final User user, final Database database) {
    final PoolProperties pp = new PoolProperties();
    pp.setDriverClassName(database.getDriverClassName());
    pp.setUrl(database.getURL(null));
    pp.setName(user.getUsername());
    //JMinor does not validate connections coming from a connection pool
    pp.setTestOnBorrow(true);
    pp.setValidator(new ConnectionValidator(database));

    return new DataSource(pp);
  }

  private static final class DataSourceWrapper extends AbstractConnectionPool<DataSource> {

    private DataSourceWrapper(final Database database, final User user, final DataSource dataSource) {
      super(user);
      dataSource.setDataSource(Util.initializeProxy(javax.sql.DataSource.class, (dataSourceProxy, dataSourceMethod, dataSourceArgs) ->
              handleInvocation(database, user, dataSource, dataSourceMethod, dataSourceArgs)));
      setPool(dataSource);
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
    public int getMaximumRetryWaitPeriod() {
      return 0;
    }

    @Override
    public void setMaximumRetryWaitPeriod(final int maximumRetryWaitPeriod) {/*Not implemented*/}

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
    public int getNewConnectionThreshold() {
      return 0;
    }

    @Override
    public void setNewConnectionThreshold(final int value) {}

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

    private final Database database;

    private ConnectionValidator(final Database database) {
      this.database = database;
    }

    @Override
    public boolean validate(final Connection connection, final int i) {
      return DatabaseUtil.isValid(connection, database, 0);
    }
  }
}
