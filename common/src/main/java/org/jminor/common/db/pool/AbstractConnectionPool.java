/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import org.jminor.common.model.User;
import org.jminor.common.model.tools.TaskScheduler;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
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
  protected final T pool;
  private final User user;

  private final LinkedList<DefaultConnectionPoolState> fineGrainedCStatistics = new LinkedList<>();
  private boolean collectFineGrainedStatistics = false;
  private final TaskScheduler fineGrainedStatisticsCollector = new TaskScheduler(new Runnable() {
    @Override
    public void run() {
      addPoolStatistics();
    }
  }, FINE_GRAINED_COLLECTION_INTERVAL, TimeUnit.MILLISECONDS);

  private final DefaultConnectionPoolCounter counter = new DefaultConnectionPoolCounter();

  /**
   * @param pool the actual connection pool
   * @param user the connection pool user
   */
  public AbstractConnectionPool(final T pool, final User user) {
    this.pool = pool;
    this.user = user;
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
   * Adds the current state of the pool to the fine grained connection pool log
   */
  private void addPoolStatistics() {
    synchronized (pool) {
      final DefaultConnectionPoolState state = fineGrainedCStatistics.removeFirst();
      state.set(System.currentTimeMillis(), getSize(), getInUse(), getWaiting());
      fineGrainedCStatistics.addLast(state);
    }
  }

  private void initializePoolStatistics() {
    for (int i = 0; i < FINE_GRAINED_STATS_SIZE; i++) {
      fineGrainedCStatistics.add(new DefaultConnectionPoolState());
    }
  }

  /**
   * @param since the time
   * @return stats collected since <code>since</code>
   */
  private List<ConnectionPoolState> getFineGrainedStatistics(final long since) {
    final List<ConnectionPoolState> poolStates;
    synchronized (pool) {
      poolStates = new LinkedList<ConnectionPoolState>(fineGrainedCStatistics);
    }
    final ListIterator<ConnectionPoolState> iterator = poolStates.listIterator();
    while (iterator.hasNext() && iterator.next().getTimestamp() < since) {
      iterator.remove();
    }

    return poolStates;
  }
}
