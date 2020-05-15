/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.common.db.pool;

import dev.codion.common.db.exception.AuthenticationException;
import dev.codion.common.db.exception.DatabaseException;
import dev.codion.common.user.User;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

import static java.lang.reflect.Proxy.newProxyInstance;
import static java.util.Objects.requireNonNull;

/**
 * A default base implementation of the ConnectionPool wrapper, handling the collection of statistics
 * @param <T> the type representing the actual pool object
 */
public abstract class AbstractConnectionPool<T> implements ConnectionPool {

  /**
   * The actual connection pool object
   */
  private T pool;
  private final ConnectionFactory connectionFactory;
  private final User user;
  private final DataSource poolDataSource;
  private final DefaultConnectionPoolCounter counter;

  /**
   * Instantiates a new AbstractConnectionPool instance.
   * @param connectionFactory the connection factory
   * @param user the connection pool user
   * @param poolDataSource the DataSource
   */
  public AbstractConnectionPool(final ConnectionFactory connectionFactory, final User user, final DataSource poolDataSource) {
    this.connectionFactory = requireNonNull(connectionFactory, "connectionFactory");
    this.user = requireNonNull(user, "user");
    this.poolDataSource = (DataSource) newProxyInstance(DataSource.class.getClassLoader(),
            new Class[] {DataSource.class}, new DataSourceInvocationHandler(requireNonNull(poolDataSource, "poolDataSource")));
    this.counter = new DefaultConnectionPoolCounter(this);
  }

  @Override
  public final User getUser() {
    return user;
  }

  @Override
  public final DataSource getPoolDataSource() {
    return poolDataSource;
  }

  @Override
  public final Connection getConnection(final User user) throws DatabaseException {
    requireNonNull(user, "user");
    final long nanoTime = System.nanoTime();
    checkConnectionPoolCredentials(user);
    try {
      counter.incrementRequestCounter();

      return fetchConnection();
    }
    catch (final SQLException e) {
      counter.incrementFailedRequestCounter();
      throw new DatabaseException(e, e.getMessage());
    }
    finally {
      counter.addCheckOutTime((int) (System.nanoTime() - nanoTime) / 1000000);
    }
  }

  @Override
  public final void resetStatistics() {
    counter.resetStatistics();
  }

  @Override
  public final boolean isCollectSnapshotStatistics() {
    return counter.isCollectSnapshotStatistics();
  }

  @Override
  public final void setCollectSnapshotStatistics(final boolean collectSnapshotStatistics) {
    counter.setCollectSnapshotStatistics(collectSnapshotStatistics);
  }

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
    this.pool = requireNonNull(pool, "pool");
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

  /**
   * Checks the given credentials against the credentials found in the connection pool user
   * @param user the user credentials to check
   * @throws AuthenticationException in case the username or password do not match the ones in the connection pool
   */
  private void checkConnectionPoolCredentials(final User user) throws AuthenticationException {
    if (!this.user.getUsername().equalsIgnoreCase(user.getUsername()) || !Arrays.equals(this.user.getPassword(), user.getPassword())) {
      throw new AuthenticationException("Wrong username or password");
    }
  }

  private final class DataSourceInvocationHandler implements InvocationHandler {

    private final DataSource dataSource;

    private DataSourceInvocationHandler(final DataSource dataSource) {
      this.dataSource = dataSource;
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
      if ("getConnection".equals(method.getName())) {
        final Connection connection = connectionFactory.createConnection(user);
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
