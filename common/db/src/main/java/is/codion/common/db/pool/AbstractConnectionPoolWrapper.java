/*
 * Copyright (c) 2013 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.pool;

import is.codion.common.db.exception.AuthenticationException;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;

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
public abstract class AbstractConnectionPoolWrapper<T> implements ConnectionPoolWrapper {

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
  protected AbstractConnectionPoolWrapper(ConnectionFactory connectionFactory, User user, DataSource poolDataSource) {
    this.connectionFactory = requireNonNull(connectionFactory, "connectionFactory");
    this.user = requireNonNull(user, "user");
    this.poolDataSource = (DataSource) newProxyInstance(DataSource.class.getClassLoader(),
            new Class[] {DataSource.class}, new DataSourceInvocationHandler(requireNonNull(poolDataSource, "poolDataSource")));
    this.counter = new DefaultConnectionPoolCounter(this);
  }

  @Override
  public final User user() {
    return user;
  }

  @Override
  public final DataSource poolDataSource() {
    return poolDataSource;
  }

  @Override
  public final Connection connection(User user) throws DatabaseException {
    requireNonNull(user, "user");
    checkConnectionPoolCredentials(user);
    long startTime = counter.isCollectCheckOutTimes() ? System.nanoTime() : 0;
    try {
      counter.incrementRequestCounter();

      return fetchConnection();
    }
    catch (SQLException e) {
      counter.incrementFailedRequestCounter();
      throw new DatabaseException(e);
    }
    finally {
      if (counter.isCollectCheckOutTimes() && startTime > 0L) {
        counter.addCheckOutTime((int) (System.nanoTime() - startTime) / 1_000_000);
      }
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
  public final void setCollectSnapshotStatistics(boolean collectSnapshotStatistics) {
    counter.setCollectSnapshotStatistics(collectSnapshotStatistics);
  }

  @Override
  public final boolean isCollectCheckOutTimes() {
    return counter.isCollectCheckOutTimes();
  }

  @Override
  public final void setCollectCheckOutTimes(boolean collectCheckOutTimes) {
    counter.setCollectCheckOutTimes(collectCheckOutTimes);
  }

  @Override
  public final ConnectionPoolStatistics statistics(long since) {
    return counter.collectStatistics(since);
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
  protected final void setPool(T pool) {
    this.pool = requireNonNull(pool, "pool");
  }

  /**
   * @return the underlying pool object
   */
  protected final T getPool() {
    return pool;
  }

  /**
   * @return the number of available connections in this pool
   */
  protected abstract int available();

  /**
   * @return the number of connections in active use
   */
  protected abstract int inUse();

  /**
   * @return the number of waiting connection requests
   */
  protected abstract int waiting();

  /**
   * Updates the given state instance with the current pool state.
   * @param state the state to update
   * @return the updated state
   */
  final DefaultConnectionPoolState updateState(DefaultConnectionPoolState state) {
    return state.set(System.currentTimeMillis(), available(), inUse(), waiting());
  }

  /**
   * Checks the given credentials against the credentials found in the connection pool user
   * @param user the user credentials to check
   * @throws AuthenticationException in case the username or password do not match the ones in the connection pool
   */
  private void checkConnectionPoolCredentials(User user) throws AuthenticationException {
    if (!this.user.username().equalsIgnoreCase(user.username()) || !Arrays.equals(this.user.getPassword(), user.getPassword())) {
      throw new AuthenticationException("Wrong username or password");
    }
  }

  private final class DataSourceInvocationHandler implements InvocationHandler {

    private final DataSource dataSource;

    private DataSourceInvocationHandler(DataSource dataSource) {
      this.dataSource = dataSource;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if ("getConnection".equals(method.getName())) {
        Connection connection = connectionFactory.createConnection(user);
        counter.incrementConnectionsCreatedCounter();

        return newProxyInstance(Connection.class.getClassLoader(), new Class[] {Connection.class},
                new ConnectionInvocationHandler(connection));
      }

      return method.invoke(dataSource, args);
    }
  }

  private final class ConnectionInvocationHandler implements InvocationHandler {

    private final Connection connection;

    private ConnectionInvocationHandler(Connection connection) {
      this.connection = connection;
    }

    @Override
    public Object invoke(Object connectionProxy, Method connectionMethod,
                         Object[] connectionArgs) throws Throwable {
      if ("close".equals(connectionMethod.getName())) {
        counter.incrementConnectionsDestroyedCounter();
      }

      return connectionMethod.invoke(connection, connectionArgs);
    }
  }
}
