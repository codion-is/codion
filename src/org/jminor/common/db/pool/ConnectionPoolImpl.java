/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import org.jminor.common.db.dbms.Database;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;

import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;

/**
 * A simple connection pool implementation, pools connections on username basis.
 */
public final class ConnectionPoolImpl implements ConnectionPool {

  public static final int DEFAULT_CONNECTION_TIMEOUT_MS = 60000;
  public static final int DEFAULT_CLEANUP_INTERVAL_MS = 20000;
  public static final int DEFAULT_MAXIMUM_POOL_SIZE = 8;
  public static final int DEFAULT_MAXIMUM_RETRY_WAIT_PERIOD_MS = 50;
  public static final int RETRIES_BEFORE_NEW_CONNECTION = 2;

  private static final Logger LOG = Util.getLogger(ConnectionPoolImpl.class);

  private final Stack<PoolableConnection> connectionPool = new Stack<PoolableConnection>();

  private final List<ConnectionPoolStateImpl> connectionPoolStatistics = new ArrayList<ConnectionPoolStateImpl>(1000);

  private Timer poolCleaner;

  private int connectionPoolStatisticsIndex = 0;
  private volatile boolean collectFineGrainedStatistics = System.getProperty(Database.DATABASE_POOL_STATISTICS, "true").equalsIgnoreCase("true");

  private final PoolableConnectionProvider poolableConnectionProvider;
  private boolean closed = false;

  private final Counter counter = new Counter();
  private final Random random = new Random();
  private final User user;
  private volatile boolean creatingConnection = false;
  private volatile int pooledConnectionTimeout = DEFAULT_CONNECTION_TIMEOUT_MS;
  private volatile int minimumPoolSize = DEFAULT_MAXIMUM_POOL_SIZE / 2;
  private volatile int maximumPoolSize = DEFAULT_MAXIMUM_POOL_SIZE;
  private volatile int poolCleanupInterval = DEFAULT_CLEANUP_INTERVAL_MS;
  private volatile int maximumRetryWaitPeriod = DEFAULT_MAXIMUM_RETRY_WAIT_PERIOD_MS;
  private volatile boolean enabled = true;

  public ConnectionPoolImpl(final PoolableConnectionProvider poolableConnectionProvider, final User user) {
    this.poolableConnectionProvider = poolableConnectionProvider;
    this.user = user;
    startPoolCleaner();
  }

  /** {@inheritDoc} */
  public PoolableConnection checkOutConnection() throws ClassNotFoundException, SQLException {
    if (closed) {
      throw new IllegalStateException("Can not check out a connection from a closed connection pool!");
    }

    final long time = System.nanoTime();
    counter.incrementRequestCounter();
    PoolableConnection connection = getConnectionFromPool();
    if (connection == null) {
      counter.incrementDelayedRequestCounter();
    }
    int retryCount = 0;
    while (connection == null) {
      if (retryCount > RETRIES_BEFORE_NEW_CONNECTION) {
        synchronized (connectionPool) {
          if (!creatingConnection && counter.getPoolSize() < maximumPoolSize) {
            creatingConnection = true;
            Executors.newSingleThreadExecutor().submit(new ConnectionCreator());
          }
        }
      }
      waitForRetry();
      connection = getConnectionFromPool();
      retryCount++;
    }
    connection.setPoolRetryCount(retryCount);
    counter.addCheckOutTime(System.nanoTime() - time);
    //todo max retries and/or max retry time, throw exception

    return connection;
  }

  /** {@inheritDoc} */
  public void checkInConnection(final PoolableConnection dbConnection) {
    if (closed) {
      disconnect(dbConnection);
    }
    if (dbConnection.isConnectionValid()) {
      try {
        if (dbConnection.isTransactionOpen()) {
          dbConnection.rollbackTransaction();
        }
      }
      catch (SQLException e) {
        LOG.error(this, e);
      }
      addConnectionToPool(dbConnection);
    }
    else {
      disconnect(dbConnection);
    }
  }

  /** {@inheritDoc} */
  public User getUser() {
    return user;
  }

  /** {@inheritDoc} */
  public synchronized int getPooledConnectionTimeout() {
    return pooledConnectionTimeout;
  }

  /** {@inheritDoc} */
  public synchronized void setPooledConnectionTimeout(final int timeout) {
    this.pooledConnectionTimeout = timeout;
  }

  /** {@inheritDoc} */
  public synchronized int getMaximumRetryWaitPeriod() {
    return maximumRetryWaitPeriod;
  }

  /** {@inheritDoc} */
  public synchronized void setMaximumRetryWaitPeriod(final int maximumRetryWaitPeriod) {
    this.maximumRetryWaitPeriod = maximumRetryWaitPeriod;
  }

  /** {@inheritDoc} */
  public synchronized int getMinimumPoolSize() {
    return minimumPoolSize;
  }

  /** {@inheritDoc} */
  public synchronized void setMinimumPoolSize(final int value) {
    if (value > maximumPoolSize || value < 0) {
      throw new IllegalArgumentException("Minimum pool size must be a positive integer an be less than maximum pool size");
    }
    this.minimumPoolSize = value;
  }

  /** {@inheritDoc} */
  public synchronized int getMaximumPoolSize() {
    return maximumPoolSize;
  }

  /** {@inheritDoc} */
  public synchronized void setMaximumPoolSize(final int value) {
    if (value < minimumPoolSize || value < 1) {
      throw new IllegalArgumentException("Maximum pool size must be larger than 1 and larger than minimum pool size");
    }
    this.maximumPoolSize = value;
  }

  /** {@inheritDoc} */
  public synchronized boolean isEnabled() {
    return enabled;
  }

  /** {@inheritDoc} */
  public synchronized void setEnabled(final boolean enabled) {
    this.enabled = enabled;
    if (!enabled) {
      close();
    }
  }

  /** {@inheritDoc} */
  public synchronized void setPoolCleanupInterval(final int poolCleanupInterval) {
    if (this.poolCleanupInterval != poolCleanupInterval) {
      this.poolCleanupInterval = poolCleanupInterval;
      startPoolCleaner();
    }
  }

  /** {@inheritDoc} */
  public synchronized int getPoolCleanupInterval() {
    return poolCleanupInterval;
  }

  /** {@inheritDoc} */
  public ConnectionPoolStatistics getConnectionPoolStatistics(final long since) {
    final ConnectionPoolStatisticsImpl statistics = new ConnectionPoolStatisticsImpl(user);
    synchronized (connectionPool) {
      final int inPool = connectionPool.size();
      statistics.setAvailableInPool(inPool);
      statistics.setConnectionsInUse(counter.getPoolSize() - inPool);
      statistics.setPoolSize(counter.getPoolSize());
      statistics.setConnectionsCreated(counter.getConnectionsCreated());
      statistics.setConnectionsDestroyed(counter.getConnectionsDestroyed());
      statistics.setCreationDate(counter.getCreationDate());
      statistics.setConnectionRequests(counter.getConnectionRequests());
      statistics.setConnectionRequestsDelayed(counter.getConnectionRequestsDelayed());
      statistics.setRequestsDelayedPerSecond(counter.getRequestsDelayedPerSecond());
      statistics.setRequestsPerSecond(counter.getRequestsPerSecond());
      statistics.setAverageCheckOutTime(counter.getAverageCheckOutTime());
      statistics.setResetDate(counter.getResetDate());
      statistics.setTimestamp(System.currentTimeMillis());
    }
    if (since >= 0) {
      statistics.setFineGrainedStatistics(getFineGrainedStatistics(since));
    }

    return statistics;
  }

  /** {@inheritDoc} */
  public boolean isCollectFineGrainedStatistics() {
    return collectFineGrainedStatistics;
  }

  /** {@inheritDoc} */
  public void setCollectFineGrainedStatistics(final boolean value) {
    this.collectFineGrainedStatistics = value;
  }

  /** {@inheritDoc} */
  public void resetPoolStatistics() {
    counter.resetPoolStatistics();
  }

  void close() {
    closed = true;
    emptyPool();
  }

  /**
   * @return a connection from the pool, null if none is available
   */
  private PoolableConnection getConnectionFromPool() {
    synchronized (connectionPool) {
      final int size = connectionPool.size();
      if (collectFineGrainedStatistics) {
        addInPoolStats(size, System.currentTimeMillis());
      }
      return size > 0 ? connectionPool.pop() : null;
    }
  }

  private void addConnectionToPool(final PoolableConnection connection) {
    connection.setPoolTime(System.currentTimeMillis());
    synchronized (connectionPool) {
      connectionPool.push(connection);
    }
  }

  private void waitForRetry() {
    try {
      Thread.sleep(random.nextInt(maximumRetryWaitPeriod));
    }
    catch (InterruptedException e) {/**/}
  }

  private void addInPoolStats(final int inPool, final long timestamp) {
    synchronized (connectionPoolStatistics) {
      final int poolStatisticsSize = 1000;
      final int inUse = counter.getPoolSize() - inPool;
      connectionPoolStatisticsIndex = connectionPoolStatisticsIndex == poolStatisticsSize ? 0 : connectionPoolStatisticsIndex;
      if (connectionPoolStatistics.size() == poolStatisticsSize) {//filled already, reuse
        connectionPoolStatistics.get(connectionPoolStatisticsIndex).set(timestamp, inPool, inUse);
      }
      else {
        connectionPoolStatistics.add(new ConnectionPoolStateImpl(timestamp, inPool, inUse));
      }

      connectionPoolStatisticsIndex++;
    }
  }

  private void startPoolCleaner() {
    if (poolCleaner != null) {
      poolCleaner.cancel();
    }

    poolCleaner = new Timer(true);
    poolCleaner.schedule(new TimerTask() {
      @Override
      public void run() {
        cleanPool();
      }
    }, new Date(), poolCleanupInterval);
  }

  private void cleanPool() {
    synchronized (connectionPool) {
      final long currentTime = System.currentTimeMillis();
      final ListIterator<PoolableConnection> iterator = connectionPool.listIterator();
      while (iterator.hasNext() && connectionPool.size() > minimumPoolSize) {
        final PoolableConnection connection = iterator.next();
        final long idleTime = currentTime - connection.getPoolTime();
        if (idleTime > pooledConnectionTimeout) {
          iterator.remove();
          disconnect(connection);
        }
      }
    }
  }

  private void emptyPool() {
    synchronized (connectionPool) {
      while (!connectionPool.isEmpty()) {
        disconnect(connectionPool.pop());
      }
    }
  }

  private void disconnect(final PoolableConnection connection) {
    if (connection == null) {
      return;
    }

    counter.incrementConnectionsDestroyedCounter();
    poolableConnectionProvider.destroyConnection(connection);
  }

  /**
   * @param since the time
   * @return stats collected since <code>since</code>, the results are not guaranteed to be ordered
   */
  private List<ConnectionPoolState> getFineGrainedStatistics(final long since) {
    final List<ConnectionPoolState> poolStates = new ArrayList<ConnectionPoolState>();
    synchronized (connectionPoolStatistics) {
      final ListIterator<ConnectionPoolStateImpl> iterator = connectionPoolStatistics.listIterator();
      while (iterator.hasNext()) {//NB. the stat log is circular, result should be sorted
        final ConnectionPoolState state = iterator.next();
        if (state.getTime() >= since) {
          poolStates.add(state);
        }
      }
    }

    return poolStates;
  }

  private final class ConnectionCreator implements Runnable {
    /** {@inheritDoc} */
    public void run() {
      try {
        addConnectionToPool(poolableConnectionProvider.createConnection(user));
        counter.incrementConnectionsCreatedCounter();
      }
      catch (Exception e) {
        LOG.error(e);
      }
      finally {
        creatingConnection = false;
      }
    }
  }

  private static class Counter {
    private static final double THOUSAND = 1000d;
    private static final int DEFAULT_STATS_UPDATE_INTERVAL = 1000;

    private final long creationDate = System.currentTimeMillis();
    private long resetDate = creationDate;
    private volatile int poolSize = 0;
    private volatile int connectionsCreated = 0;
    private volatile int connectionsDestroyed = 0;
    private volatile int connectionRequests = 0;
    private volatile int connectionRequestsDelayed = 0;
    private volatile int requestsDelayedPerSecond = 0;
    private volatile int requestsDelayedPerSecondCounter = 0;
    private volatile int requestsPerSecond = 0;
    private volatile int requestsPerSecondCounter = 0;
    private long averageCheckOutTime = 0;
    private final List<Long> checkOutTimes = new ArrayList<Long>();
    private long requestsPerSecondTime = creationDate;

    Counter() {
      new Timer(true).schedule(new TimerTask() {
        @Override
        public void run() {
          updateStatistics();
        }
      }, new Date(), DEFAULT_STATS_UPDATE_INTERVAL);
    }

    public synchronized long getCreationDate() {
      return creationDate;
    }

    public synchronized long getResetDate() {
      return resetDate;
    }

    public synchronized int getConnectionRequests() {
      return connectionRequests;
    }

    public synchronized int getConnectionRequestsDelayed() {
      return connectionRequestsDelayed;
    }

    public synchronized int getConnectionsCreated() {
      return connectionsCreated;
    }

    public synchronized int getConnectionsDestroyed() {
      return connectionsDestroyed;
    }

    public synchronized int getPoolSize() {
      return poolSize;
    }

    public synchronized int getRequestsDelayedPerSecond() {
      return requestsDelayedPerSecond;
    }

    public synchronized int getRequestsPerSecond() {
      return requestsPerSecond;
    }

    public synchronized void addCheckOutTime(final long time) {
      checkOutTimes.add(time);
    }

    public synchronized void resetPoolStatistics() {
      connectionsCreated = 0;
      connectionsDestroyed = 0;
      connectionRequests = 0;
      connectionRequestsDelayed = 0;
      checkOutTimes.clear();
      resetDate = System.currentTimeMillis();
    }

    public synchronized void updateStatistics() {
      final long current = System.currentTimeMillis();
      final double seconds = (current - requestsPerSecondTime) / THOUSAND;
      requestsPerSecond = (int) ((double) requestsPerSecondCounter / seconds);
      requestsPerSecondCounter = 0;
      requestsDelayedPerSecond = (int) ((double) requestsDelayedPerSecondCounter / seconds);
      requestsDelayedPerSecondCounter = 0;
      requestsPerSecondTime = current;
      averageCheckOutTime = 0;
      if (!checkOutTimes.isEmpty()) {
        long total = 0;
        for (final Long time : checkOutTimes) {
          total += time;
        }
        averageCheckOutTime = total / checkOutTimes.size();
        checkOutTimes.clear();
      }
    }

    public synchronized void incrementConnectionsDestroyedCounter() {
      poolSize--;
      connectionsDestroyed++;
    }

    public synchronized void incrementConnectionsCreatedCounter() {
      poolSize++;
      connectionsCreated++;
    }

    public synchronized void incrementDelayedRequestCounter() {
      connectionRequestsDelayed++;
      requestsDelayedPerSecondCounter++;
    }

    public synchronized void incrementRequestCounter() {
      connectionRequests++;
      requestsPerSecondCounter++;
    }

    public synchronized long getAverageCheckOutTime() {
      return averageCheckOutTime;
    }
  }
}