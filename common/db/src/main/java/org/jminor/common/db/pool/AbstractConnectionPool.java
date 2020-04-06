/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import org.jminor.common.TaskScheduler;
import org.jminor.common.db.Database;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.user.User;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.reflect.Proxy.newProxyInstance;

/**
 * A default base implementation of the ConnectionPool wrapper, handling the collection of statistics
 * @param <T> the type representing the actual pool object
 */
public abstract class AbstractConnectionPool<T> implements ConnectionPool {

  private static final int FINE_GRAINED_STATS_SIZE = 1000;
  private static final int FINE_GRAINED_COLLECTION_INTERVAL = 10;

  /**
   * The actual connection pool object
   */
  private T pool;
  private final Database database;
  private final User user;
  private final DataSource poolDataSource;

  private final LinkedList<DefaultConnectionPoolState> fineGrainedCStatistics = new LinkedList<>();
  private boolean collectFineGrainedStatistics = false;
  private final TaskScheduler fineGrainedStatisticsCollector = new TaskScheduler(new StatisticsCollector(),
          FINE_GRAINED_COLLECTION_INTERVAL, TimeUnit.MILLISECONDS);

  private final DefaultConnectionPoolCounter counter = new DefaultConnectionPoolCounter();

  /**
   * @param database the underlying database
   * @param user the connection pool user
   * @param poolDataSource the DataSource
   */
  public AbstractConnectionPool(final Database database, final User user, final DataSource poolDataSource) {
    this.database = database;
    this.user = user;
    this.poolDataSource = (DataSource) newProxyInstance(DataSource.class.getClassLoader(),
            new Class[] {DataSource.class}, new DataSourceInvocationHandler(poolDataSource));
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
    counter.resetPoolStatistics();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isCollectFineGrainedStatistics() {
    return collectFineGrainedStatistics;
  }

  /** {@inheritDoc} */
  @Override
  public final void setCollectFineGrainedStatistics(final boolean collectFineGrainedStatistics) {
    if (collectFineGrainedStatistics) {
      initializePoolStatistics();
      fineGrainedStatisticsCollector.start();
    }
    else {
      fineGrainedStatisticsCollector.stop();
      fineGrainedCStatistics.clear();
    }
    this.collectFineGrainedStatistics = collectFineGrainedStatistics;
  }

  /** {@inheritDoc} */
  @Override
  public final ConnectionPoolStatistics getStatistics(final long since) {
    final DefaultConnectionPoolStatistics statistics = new DefaultConnectionPoolStatistics(getUser());
    counter.updateStatistics();
    synchronized (pool) {
      final int inPool = getSize();
      final int inUseCount = getInUse();
      statistics.setAvailableInPool(inPool);
      statistics.setConnectionsInUse(inUseCount);
      statistics.setPoolSize(inPool + inUseCount);
      statistics.setConnectionsCreated(counter.getConnectionsCreated());
      statistics.setConnectionsDestroyed(counter.getConnectionsDestroyed());
      statistics.setCreationDate(counter.getCreationDate());
      statistics.setConnectionRequests(counter.getConnectionRequests());
      statistics.setConnectionRequestsDelayed(counter.getConnectionRequestsDelayed());
      statistics.setRequestsDelayedPerSecond(counter.getRequestsDelayedPerSecond());
      statistics.setConnectionRequestsFailed(counter.getConnectionRequestsFailed());
      statistics.setRequestsFailedPerSecond(counter.getRequestsFailedPerSecond());
      statistics.setRequestsPerSecond(counter.getRequestsPerSecond());
      statistics.setAverageCheckOutTime(counter.getAverageCheckOutTime());
      statistics.setMinimumCheckOutTime(counter.getMinimumCheckOutTime());
      statistics.setMaximumCheckOutTime(counter.getMaximumCheckOutTime());
      statistics.setResetDate(counter.getResetDate());
      statistics.setTimestamp(System.currentTimeMillis());
      if (collectFineGrainedStatistics && since >= 0) {
        statistics.setFineGrainedStatistics(fineGrainedCStatistics.stream()
                .filter(state -> state.getTimestamp() >= since).collect(Collectors.toList()));
      }
    }

    return statistics;
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

  private void initializePoolStatistics() {
    for (int i = 0; i < FINE_GRAINED_STATS_SIZE; i++) {
      fineGrainedCStatistics.add(new DefaultConnectionPoolState());
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

  private final class StatisticsCollector implements Runnable {

    /**
     * Adds the current state of the pool to the fine grained connection pool log
     */
    @Override
    public void run() {
      synchronized (pool) {
        final DefaultConnectionPoolState state = fineGrainedCStatistics.removeFirst();
        state.set(System.currentTimeMillis(), getSize(), getInUse(), getWaiting());
        fineGrainedCStatistics.addLast(state);
      }
    }
  }
}
