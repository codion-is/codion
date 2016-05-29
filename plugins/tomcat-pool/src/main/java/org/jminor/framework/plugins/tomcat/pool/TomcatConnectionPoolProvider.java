/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.tomcat.pool;

import org.jminor.common.Util;
import org.jminor.common.db.Database;
import org.jminor.common.db.DatabaseUtil;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.pool.AbstractConnectionPool;
import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.db.pool.ConnectionPoolProvider;
import org.jminor.common.model.User;

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
      super(dataSource, user);
      dataSource.setDataSource(Util.initializeProxy(javax.sql.DataSource.class, (proxy, method, args) -> {
        if ("getConnection".equals(method.getName())) {
          try {
            final Connection connection = database.createConnection(user);
            getCounter().incrementConnectionsCreatedCounter();

            return Util.initializeProxy(Connection.class, (proxy1, method1, args1) -> {
              if ("close".equals(method1.getName())) {
                getCounter().incrementConnectionsDestroyedCounter();
              }

              return method1.invoke(connection, args1);
            });
          }
          catch (final DatabaseException e) {
            throw e.getCause();
          }
        }

        throw new NoSuchMethodException(method.toString());
      }));
    }

    @Override
    public Connection getConnection() throws DatabaseException {
      final long nanoTime = System.nanoTime();
      try {
        getCounter().incrementRequestCounter();

        return pool.getConnection();
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
      pool.close();
    }

    @Override
    public int getCleanupInterval() {
      return pool.getTimeBetweenEvictionRunsMillis();
    }

    @Override
    public void setCleanupInterval(final int poolCleanupInterval) {
      pool.setTimeBetweenEvictionRunsMillis(poolCleanupInterval);
    }

    @Override
    public int getConnectionTimeout() {
      return pool.getSuspectTimeout() * 1000;
    }

    @Override
    public void setConnectionTimeout(final int timeout) {
      pool.setSuspectTimeout(timeout / 1000);
    }

    @Override
    public int getMaximumRetryWaitPeriod() {
      return 0;
    }

    @Override
    public void setMaximumRetryWaitPeriod(final int maximumRetryWaitPeriod) {}

    @Override
    public int getMinimumPoolSize() {
      return pool.getMinIdle();
    }

    @Override
    public void setMinimumPoolSize(final int value) {
      pool.setMinIdle(value);
    }

    @Override
    public int getMaximumPoolSize() {
      return pool.getMaxActive();
    }

    @Override
    public void setMaximumPoolSize(final int value) {
      pool.setMaxActive(value);
      pool.setMaxIdle(value);
    }

    @Override
    public int getMaximumCheckOutTime() {
      return pool.getMaxWait();
    }

    @Override
    public void setMaximumCheckOutTime(final int value) {
      pool.setMaxWait(value);
    }

    @Override
    public int getNewConnectionThreshold() {
      return 0;
    }

    @Override
    public void setNewConnectionThreshold(final int value) {}

    @Override
    protected int getSize() {
      return pool.getSize() - pool.getActive();
    }

    @Override
    protected int getInUse() {
      return pool.getActive();
    }

    @Override
    protected int getWaiting() {
      return pool.getWaitCount();
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
