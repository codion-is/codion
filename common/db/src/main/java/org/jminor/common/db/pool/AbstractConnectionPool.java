/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import org.jminor.common.db.Database;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.user.User;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;

import static java.lang.reflect.Proxy.newProxyInstance;

/**
 * A default base implementation of the ConnectionPool wrapper, handling the collection of statistics
 * @param <T> the type representing the actual pool object
 */
public abstract class AbstractConnectionPool<T> implements ConnectionPool {

  /**
   * The actual connection pool object
   */
  private T pool;
  private final Database database;
  private final User user;
  private final DataSource poolDataSource;
  private final DefaultConnectionPoolCounter counter;

  /**
   * Instantiates a new AbstractConnectionPool instance.
   * @param database the underlying database
   * @param user the connection pool user
   * @param poolDataSource the DataSource
   */
  public AbstractConnectionPool(final Database database, final User user, final DataSource poolDataSource) {
    this.database = database;
    this.user = user;
    this.poolDataSource = (DataSource) newProxyInstance(DataSource.class.getClassLoader(),
            new Class[] {DataSource.class}, new DataSourceInvocationHandler(poolDataSource));
    this.counter = new DefaultConnectionPoolCounter(this);
  }

  /** {@inheritDoc} */
  @Override
  public Database getDatabase() {
    return database;
  }

  /** {@inheritDoc} */
  @Override
  public final User getUser() {
    return user;
  }

  /** {@inheritDoc} */
  @Override
  public final DataSource getPoolDataSource() {
    return poolDataSource;
  }

  /** {@inheritDoc} */
  @Override
  public final Connection getConnection() throws DatabaseException {
    final long nanoTime = System.nanoTime();
    try {
      counter.incrementRequestCounter();

      return fetchConnection();
    }
    catch (final SQLException e) {
      counter.incrementFailedRequestCounter();
      throw new DatabaseException(e, e.getMessage());
    }
    finally {
      counter.addCheckOutTime((System.nanoTime() - nanoTime) / 1000000);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void resetStatistics() {
    counter.resetStatistics();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isCollectSnapshotStatistics() {
    return counter.isCollectSnapshotStatistics();
  }

  /** {@inheritDoc} */
  @Override
  public final void setCollectSnapshotStatistics(final boolean collectSnapshotStatistics) {
    counter.setCollectSnapshotStatistics(collectSnapshotStatistics);
  }

  /** {@inheritDoc} */
  @Override
  public final ConnectionPoolStatistics getStatistics(final long since) {
    return counter.getStatistics(since);
  }

  /**
   * Fetches a connection from the underlying pool.
   * @return a connection from the underlying pool
   * @throws SQLException in case of an exception.
   */
  protected abstract Connection fetchConnection() throws SQLException;

  /**
   * @param pool the underlying connection pool
   */
  protected void setPool(final T pool) {
    this.pool = pool;
  }

  /**
   * @return the underlying pool object
   */
  protected T getPool() {
    return pool;
  }

  /**
   * @return the number of connections in this pool
   */
  protected abstract int getSize();

  /**
   * @return the number of connections in active use
   */
  protected abstract int getInUse();

  /**
   * @return the number of waiting connections
   */
  protected abstract int getWaiting();

  /**
   * Updates the given state instance with the current pool state.
   * @param state the state to update
   * @return the updated state
   */
  final ConnectionPoolState updateState(final ConnectionPoolState state) {
    state.set(System.currentTimeMillis(), getSize(), getInUse(), getWaiting());

    return state;
  }

  private final class DataSourceInvocationHandler implements InvocationHandler {

    private final DataSource dataSource;

    private DataSourceInvocationHandler(final DataSource dataSource) {
      this.dataSource = dataSource;
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
      if ("getConnection".equals(method.getName())) {
        final Connection connection = database.createConnection(user);
        counter.incrementConnectionsCreatedCounter();

        return newProxyInstance(Connection.class.getClassLoader(), new Class[] {Connection.class},
                new ConnectionInvocationHandler(connection));
      }

      return method.invoke(dataSource, args);
    }
  }

  private final class ConnectionInvocationHandler implements InvocationHandler {

    private final Connection connection;

    private ConnectionInvocationHandler(final Connection connection) {
      this.connection = connection;
    }

    @Override
    public Object invoke(final Object connectionProxy, final Method connectionMethod,
                         final Object[] connectionArgs) throws Throwable {
      if ("close".equals(connectionMethod.getName())) {
        counter.incrementConnectionsDestroyedCounter();
      }

      return connectionMethod.invoke(connection, connectionArgs);
    }
  }
}
