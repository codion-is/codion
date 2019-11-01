/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import org.jminor.common.TaskScheduler;
import org.jminor.common.User;
import org.jminor.common.Util;
import org.jminor.common.db.Database;
import org.jminor.common.db.exception.DatabaseException;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A default base implementation of ConnectionPool, handling the collection of statistics
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

  private final LinkedList<DefaultConnectionPoolState> fineGrainedCStatistics = new LinkedList<>();
  private boolean collectFineGrainedStatistics = false;
  private final TaskScheduler fineGrainedStatisticsCollector = new TaskScheduler(new StatisticsCollector(),
          FINE_GRAINED_COLLECTION_INTERVAL, TimeUnit.MILLISECONDS);

  private final DefaultConnectionPoolCounter counter = new DefaultConnectionPoolCounter();

  /**
   * @param database the underlying database
   * @param user the connection pool user
   */
  public AbstractConnectionPool(final Database database, final User user) {
    this.database = database;
    this.user = user;
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
    }
    if (collectFineGrainedStatistics && since >= 0) {
      statistics.setFineGrainedStatistics(getFineGrainedStatistics(since));
    }

    return statistics;
  }

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
   * @return the counter
   */
  protected final ConnectionPool.Counter getCounter() {
    return counter;
  }

  /**
   * Handles a method invocation for this pool, counting created and destroyed connections.
   * @param database the database
   * @param user the user
   * @param dataSource the data source
   * @param dataSourceMethod the data source method being called
   * @param dataSourceArgs the data source method arguments
   * @return the method return value
   * @throws DatabaseException in case of a an exception
   * @throws IllegalAccessException in case of illegal access
   * @throws InvocationTargetException in case of invocation exception
   */
  protected final Object handleInvocation(final Database database, final User user, final DataSource dataSource,
                                          final Method dataSourceMethod, final Object[] dataSourceArgs)
          throws DatabaseException, IllegalAccessException, InvocationTargetException {
    if ("getConnection".equals(dataSourceMethod.getName())) {
      final Connection connection = database.createConnection(user);
      counter.incrementConnectionsCreatedCounter();

      return Util.initializeProxy(Connection.class, (connectionProxy, connectionMethod, connectionArgs) -> {
        if ("close".equals(connectionMethod.getName())) {
          counter.incrementConnectionsDestroyedCounter();
        }

        return connectionMethod.invoke(connection, connectionArgs);
      });
    }

    return dataSourceMethod.invoke(dataSource, dataSourceArgs);
  }

  private void initializePoolStatistics() {
    for (int i = 0; i < FINE_GRAINED_STATS_SIZE; i++) {
      fineGrainedCStatistics.add(new DefaultConnectionPoolState());
    }
  }

  /**
   * @param since the time
   * @return stats collected since {@code since}
   */
  private List<ConnectionPoolState> getFineGrainedStatistics(final long since) {
    final List<ConnectionPoolState> poolStates;
    synchronized (pool) {
      poolStates = new LinkedList<>(fineGrainedCStatistics);
    }
    poolStates.removeIf(state -> state.getTimestamp() < since);

    return poolStates;
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
