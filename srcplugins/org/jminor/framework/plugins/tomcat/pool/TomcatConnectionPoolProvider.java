/*
 * Copyright (c) 2004 - 2013, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.tomcat.pool;

import org.jminor.common.db.Database;
import org.jminor.common.db.DatabaseUtil;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.pool.AbstractConnectionPool;
import org.jminor.common.db.pool.ConnectionPool;
import org.jminor.common.db.pool.ConnectionPoolProvider;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.apache.tomcat.jdbc.pool.Validator;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
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
    pp.setValidator(new ConnectionValidator(database));

    return new DataSource(pp);
  }

  private static class DataSourceWrapper extends AbstractConnectionPool<DataSource> {

    private DataSourceWrapper(final Database database, final User user, final DataSource dataSource) {
      super(dataSource, user);
      dataSource.setDataSource(Util.initializeProxy(javax.sql.DataSource.class, new InvocationHandler() {
        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
          if (method.getName().equals("getConnection")) {
            try {
              final Connection connection = database.createConnection(user);
              getCounter().incrementConnectionsCreatedCounter();

              return Util.initializeProxy(Connection.class, new InvocationHandler() {
                @Override
                public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
                  if (method.getName().equals("close")) {
                    getCounter().incrementConnectionsDestroyedCounter();
                  }

                  return method.invoke(connection, args);
                }
              });
            }
            catch (DatabaseException e) {
              throw (SQLException) e.getCause();
            }
          }

          throw new NoSuchMethodException(method.toString());
        }
      }));
    }

    /** {@inheritDoc} */
    @Override
    public Connection getConnection() throws DatabaseException {
      final long nanoTime = System.nanoTime();
      try {
        getCounter().incrementRequestCounter();
        if (isCollectFineGrainedStatistics()) {
          addPoolStatistics();
        }

        return pool.getConnection();
      }
      catch (SQLException e) {
        getCounter().incrementFailedRequestCounter();
        throw new DatabaseException(e, e.getMessage());
      }
      finally {
        getCounter().addCheckOutTime((System.nanoTime() - nanoTime) / 1000000);
      }
    }

    /** {@inheritDoc} */
    @Override
    public void returnConnection(final Connection connection) {
      try {
        connection.close();
      }
      catch (SQLException ignored) {}
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
      pool.close();
    }

    /** {@inheritDoc} */
    @Override
    public int getCleanupInterval() {
      return pool.getTimeBetweenEvictionRunsMillis();
    }

    /** {@inheritDoc} */
    @Override
    public void setCleanupInterval(final int poolCleanupInterval) {
      pool.setTimeBetweenEvictionRunsMillis(poolCleanupInterval);
    }

    /** {@inheritDoc} */
    @Override
    public int getConnectionTimeout() {
      return pool.getSuspectTimeout() * 1000;
    }

    /** {@inheritDoc} */
    @Override
    public void setConnectionTimeout(final int timeout) {
      pool.setSuspectTimeout(timeout / 1000);
    }

    /** {@inheritDoc} */
    @Override
    public int getMaximumRetryWaitPeriod() {
      return 0;
    }

    /** {@inheritDoc} */
    @Override
    public void setMaximumRetryWaitPeriod(final int maximumRetryWaitPeriod) {}

    /** {@inheritDoc} */
    @Override
    public int getMinimumPoolSize() {
      return pool.getMinIdle();
    }

    /** {@inheritDoc} */
    @Override
    public void setMinimumPoolSize(final int value) {
      pool.setMinIdle(value);
    }

    /** {@inheritDoc} */
    @Override
    public int getMaximumPoolSize() {
      return pool.getMaxActive();
    }

    /** {@inheritDoc} */
    @Override
    public void setMaximumPoolSize(final int value) {
      pool.setMaxActive(value);
      pool.setMaxIdle(value);
    }

    /** {@inheritDoc} */
    @Override
    public int getMaximumCheckOutTime() {
      return pool.getMaxWait();
    }

    /** {@inheritDoc} */
    @Override
    public void setMaximumCheckOutTime(final int value) {
      pool.setMaxWait(value);
    }

    /** {@inheritDoc} */
    @Override
    public int getNewConnectionThreshold() {
      return 0;
    }

    /** {@inheritDoc} */
    @Override
    public void setNewConnectionThreshold(final int value) {}

    /** {@inheritDoc} */
    @Override
    protected int getSize() {
      return pool.getSize() - pool.getActive();
    }

    /** {@inheritDoc} */
    @Override
    protected int getInUse() {
      return pool.getActive();
    }

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
    @Override
    public boolean validate(final Connection connection, final int i) {
      return DatabaseUtil.isValid(connection, database, 0);
    }
  }
}
