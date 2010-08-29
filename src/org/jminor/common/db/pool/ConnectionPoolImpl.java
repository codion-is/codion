/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import org.jminor.common.db.dbms.Database;
import org.jminor.common.model.User;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Date;

/**
 * A simple connection pool implementation, pools connections on username basis.
 */
public final class ConnectionPoolImpl implements ConnectionPool {

  public static final int DEFAULT_CONNECTION_TIMEOUT_MS = 60000;
  public static final int DEFAULT_CLEANUP_INTERVAL_MS = 20000;
  public static final int DEFAULT_MAXIMUM_POOL_SIZE = 8;
  public static final int DEFAULT_MAXIMUM_RETRY_WAIT_PERIOD_MS = 50;
  public static final int DEFAULT_MAXIMUM_CHECK_OUT_TIME = 1000;
  public static final int DEFAULT_RETRIES_BEFORE_NEW_CONNECTION = 10;

  private final PoolableConnectionProvider connectionProvider;
  private final User user;
  private final Stack<PoolableConnection> pool = new Stack<PoolableConnection>();
  private final Collection<PoolableConnection> inUse = new ArrayList<PoolableConnection>();
  private final Counter counter = new Counter();
  private final Random random = new Random();

  private int minimumPoolSize = DEFAULT_MAXIMUM_POOL_SIZE / 2;
  private int maximumPoolSize = DEFAULT_MAXIMUM_POOL_SIZE;
  private int retriesBeforeNewConnection = DEFAULT_RETRIES_BEFORE_NEW_CONNECTION;
  private int maximumRetryWaitPeriod = DEFAULT_MAXIMUM_RETRY_WAIT_PERIOD_MS;
  private int poolCleanupInterval = DEFAULT_CLEANUP_INTERVAL_MS;
  private int pooledConnectionTimeout = DEFAULT_CONNECTION_TIMEOUT_MS;
  private int maximumCheckOuttime = DEFAULT_MAXIMUM_CHECK_OUT_TIME;

  private boolean creatingConnection = false;
  private boolean enabled = true;
  private boolean closed = false;

  private Timer cleanupTimer;
  private int currentPoolStatisticsIndex = 0;
  private volatile boolean collectFineGrainedStatistics = System.getProperty(Database.DATABASE_POOL_STATISTICS, "true").equalsIgnoreCase("true");
  private final List<ConnectionPoolStateImpl> connectionPoolStatistics = new ArrayList<ConnectionPoolStateImpl>(1000);

  /**
   * Instantiates a new ConnectionPoolImpl.
   * @param connectionProvider the connection provider
   * @param user the user this pool is based on
   */
  public ConnectionPoolImpl(final PoolableConnectionProvider connectionProvider, final User user) {
    this.user = user;
    this.connectionProvider = connectionProvider;
    for (int i = 0; i < 1000; i++) {
      connectionPoolStatistics.add(new ConnectionPoolStateImpl());
    }
    startPoolCleaner();
  }

  /** {@inheritDoc} */
  public PoolableConnection checkOutConnection() throws ClassNotFoundException, SQLException {
    if (!enabled || closed) {
      throw new IllegalStateException("ConnectionPool not enabled or closed");
    }
    final long currentTime = System.currentTimeMillis();
    addPoolStatistics(currentTime);
    counter.incrementRequestCounter();

    PoolableConnection connection = fetchFromPool();
    if (connection == null) {
      counter.incrementDelayedRequestCounter();
    }
    int retryCount = 0;
    while (connection == null && System.currentTimeMillis() - currentTime < maximumCheckOuttime) {
      retryCount++;
      boolean newConnection = false;
      synchronized (pool) {
        if (retryCount > retriesBeforeNewConnection && pool.size() + inUse.size() < maximumPoolSize && !creatingConnection) {
          newConnection = true;
        }
      }
      if (newConnection) {
        synchronized (pool) {
          creatingConnection = true;
        }
        try {
          connection = connectionProvider.createConnection(user);
          counter.incrementConnectionsCreatedCounter();
          inUse.add(connection);
        }
        finally {
          creatingConnection = false;
        }
      }
      else {
        try {
          Thread.sleep(random.nextInt(maximumRetryWaitPeriod));
        }
        catch (InterruptedException e) {/**/}

        connection = fetchFromPool();
      }
    }
    if (connection != null) {
      counter.addCheckOutTime(System.currentTimeMillis() - currentTime);
      connection.setPoolRetryCount(retryCount);

      return connection;
    }

    throw new ConnectionPoolException.NoConnectionAvailable();
  }

  /** {@inheritDoc} */
  public void checkInConnection(final PoolableConnection dbConnection) {
    if (closed || !dbConnection.isConnectionValid()) {
      synchronized (pool) {
        inUse.remove(dbConnection);
      }
      connectionProvider.destroyConnection(dbConnection);
      counter.incrementConnectionsDestroyedCounter();

      return;
    }
    synchronized (pool) {
      inUse.remove(dbConnection);
      pool.push(dbConnection);
      dbConnection.setPoolTime(System.currentTimeMillis());
    }
  }

  /** {@inheritDoc} */
  public User getUser() {
    return user;
  }

  /** {@inheritDoc} */
  public void close() {
    closed = true;
    synchronized (pool) {
      while (!pool.isEmpty()) {
        connectionProvider.destroyConnection(pool.pop());
        counter.incrementConnectionsDestroyedCounter();
      }
    }
  }

  /** {@inheritDoc} */
  public ConnectionPoolStatistics getConnectionPoolStatistics(final long since) {
    final ConnectionPoolStatisticsImpl statistics = new ConnectionPoolStatisticsImpl(user);
    synchronized (pool) {
      final int inPool = pool.size();
      final int inUseCount = inUse.size();
      statistics.setAvailableInPool(inPool);
      statistics.setConnectionsInUse(inUseCount);
      statistics.setPoolSize(inPool + inUseCount);
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
  public void resetPoolStatistics() {
    counter.resetPoolStatistics();
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
  public boolean isEnabled() {
    return enabled;
  }

  /** {@inheritDoc} */
  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
    if (!enabled) {
      close();
    }
  }

  /** {@inheritDoc} */
  public int getPoolCleanupInterval() {
    return poolCleanupInterval;
  }

  /** {@inheritDoc} */
  public void setPoolCleanupInterval(final int poolCleanupInterval) {
    if (poolCleanupInterval != this.poolCleanupInterval) {
      this.poolCleanupInterval = poolCleanupInterval;
      startPoolCleaner();
    }
  }

  /** {@inheritDoc} */
  public int getPooledConnectionTimeout() {
    return pooledConnectionTimeout;
  }

  /** {@inheritDoc} */
  public void setPooledConnectionTimeout(final int timeout) {
    this.pooledConnectionTimeout = timeout;
  }

  /** {@inheritDoc} */
  public int getMaximumRetryWaitPeriod() {
    return maximumRetryWaitPeriod;
  }

  /** {@inheritDoc} */
  public void setMaximumRetryWaitPeriod(final int maximumRetryWaitPeriod) {
    this.maximumRetryWaitPeriod = maximumRetryWaitPeriod;
  }

  /** {@inheritDoc} */
  public int getMinimumPoolSize() {
    return minimumPoolSize;
  }

  /** {@inheritDoc} */
  public void setMinimumPoolSize(final int value) {
    if (value > maximumPoolSize || value < 0) {
      throw new IllegalArgumentException("Minimum pool size must be a positive integer an be less than maximum pool size");
    }
    this.minimumPoolSize = value;
  }

  /** {@inheritDoc} */
  public int getMaximumPoolSize() {
    return maximumPoolSize;
  }

  /** {@inheritDoc} */
  public void setMaximumPoolSize(final int value) {
    if (value < minimumPoolSize || value < 1) {
      throw new IllegalArgumentException("Maximum pool size must be larger than 1 and larger than minimum pool size");
    }
    this.maximumPoolSize = value;
  }

  /** {@inheritDoc} */
  public int getMaximumCheckOutTime() {
    return maximumCheckOuttime;
  }

  /** {@inheritDoc} */
  public void setMaximumCheckOutTime(final int value) {
    if (value < 0) {
      throw new IllegalArgumentException("Maximum check out time must be a positive integer");
    }
    this.maximumCheckOuttime = value;
  }

  private PoolableConnection fetchFromPool() {
    PoolableConnection connection = null;
    boolean destroyConnection = false;
    synchronized (pool) {
      if (pool.size() > 0) {
        connection = pool.pop();
        if (!connection.isConnectionValid()) {
          destroyConnection = true;
        }
        else {
          inUse.add(connection);
        }
      }
    }

    if (destroyConnection) {
      connectionProvider.destroyConnection(connection);
      synchronized (pool) {
        counter.incrementConnectionsDestroyedCounter();
      }
      connection = null;
    }

    return connection;
  }

  private void addPoolStatistics(final long currentTime) {
    synchronized (pool) {
      if (currentPoolStatisticsIndex == 1000) {
        currentPoolStatisticsIndex = 0;
      }
      final int inUseCount = inUse.size();
      connectionPoolStatistics.get(currentPoolStatisticsIndex).set(currentTime, pool.size(), inUseCount);
      currentPoolStatisticsIndex++;
    }
  }

  /**
   * @param since the time
   * @return stats collected since <code>since</code>, the results are not guaranteed to be ordered
   */
  private List<ConnectionPoolState> getFineGrainedStatistics(final long since) {
    final List<ConnectionPoolState> poolStates = new ArrayList<ConnectionPoolState>();
    synchronized (pool) {
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

  private void cleanPool() {
    final long currentTime = System.currentTimeMillis();
    synchronized (pool) {
      final ListIterator<PoolableConnection> iterator = pool.listIterator();
      while (iterator.hasNext() && pool.size() > minimumPoolSize) {
        final PoolableConnection connection = iterator.next();
        if (currentTime - connection.getPoolTime() > pooledConnectionTimeout) {
          connectionProvider.destroyConnection(connection);
        }
      }
    }
  }

  private void startPoolCleaner() {
    if (cleanupTimer != null) {
      cleanupTimer.cancel();
    }
    cleanupTimer = new Timer(true);
    cleanupTimer.schedule(new TimerTask() {
      @Override
      public void run() {
        cleanPool();
      }
    }, 0, this.poolCleanupInterval);
  }

  private static final class Counter {
    private static final double THOUSAND = 1000d;
    private static final int DEFAULT_STATS_UPDATE_INTERVAL = 1000;

    private final long creationDate = System.currentTimeMillis();
    private long resetDate = creationDate;
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

    private Counter() {
      new Timer(true).schedule(new TimerTask() {
        @Override
        public void run() {
          updateStatistics();
        }
      }, new Date(), DEFAULT_STATS_UPDATE_INTERVAL);
    }

    private synchronized long getCreationDate() {
      return creationDate;
    }

    private synchronized long getResetDate() {
      return resetDate;
    }

    private synchronized int getConnectionRequests() {
      return connectionRequests;
    }

    private synchronized int getConnectionRequestsDelayed() {
      return connectionRequestsDelayed;
    }

    private synchronized int getConnectionsCreated() {
      return connectionsCreated;
    }

    private synchronized int getConnectionsDestroyed() {
      return connectionsDestroyed;
    }

    private synchronized int getRequestsDelayedPerSecond() {
      return requestsDelayedPerSecond;
    }

    private synchronized int getRequestsPerSecond() {
      return requestsPerSecond;
    }

    private synchronized void addCheckOutTime(final long time) {
      checkOutTimes.add(time);
    }

    private synchronized void resetPoolStatistics() {
      connectionsCreated = 0;
      connectionsDestroyed = 0;
      connectionRequests = 0;
      connectionRequestsDelayed = 0;
      checkOutTimes.clear();
      resetDate = System.currentTimeMillis();
    }

    private synchronized void updateStatistics() {
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

    private synchronized void incrementConnectionsDestroyedCounter() {
      connectionsDestroyed++;
    }

    private synchronized void incrementConnectionsCreatedCounter() {
      connectionsCreated++;
    }

    private synchronized void incrementDelayedRequestCounter() {
      connectionRequestsDelayed++;
      requestsDelayedPerSecondCounter++;
    }

    private synchronized void incrementRequestCounter() {
      connectionRequests++;
      requestsPerSecondCounter++;
    }

    private synchronized long getAverageCheckOutTime() {
      return averageCheckOutTime;
    }
  }

  private static final class ConnectionPoolStateImpl implements ConnectionPoolState, Serializable {

    private static final long serialVersionUID = 1;

    private long time;
    private int connectionCount = -1;
    private int connectionsInUse = -1;

    private ConnectionPoolStateImpl() {}

    private void set(final long time, final int connectionCount, final int connectionsInUse) {
      this.time = time;
      this.connectionCount = connectionCount;
      this.connectionsInUse = connectionsInUse;
    }

    /** {@inheritDoc} */
    public int getConnectionCount() {
      return connectionCount;
    }

    /** {@inheritDoc} */
    public int getConnectionsInUse() {
      return connectionsInUse;
    }

    /** {@inheritDoc} */
    public long getTime() {
      return time;
    }
  }

  private static final class ConnectionPoolStatisticsImpl implements ConnectionPoolStatistics, Serializable {
    private static final long serialVersionUID = 1;

    private final User user;
    private long timestamp;
    private int connectionsInUse;
    private int availableInPool;

    private int connectionsCreated;
    private int connectionsDestroyed;
    private long creationDate;

    private List<ConnectionPoolState> fineGrainedStatistics;
    private long resetDate;
    private int connectionRequests;
    private int connectionRequestsDelayed;
    private int requestsDelayedPerSecond;
    private int requestsPerSecond;
    private int poolSize;
    private long averageCheckOutTime;

    /** {@inheritDoc} */
    public User getUser() {
      return user;
    }

    /** {@inheritDoc} */
    public List<ConnectionPoolState> getFineGrainedStatistics() {
      return fineGrainedStatistics;
    }

    /** {@inheritDoc} */
    public int getAvailableInPool() {
      return availableInPool;
    }

    /** {@inheritDoc} */
    public int getConnectionsInUse() {
      return connectionsInUse;
    }

    /** {@inheritDoc} */
    public long getTimestamp() {
      return timestamp;
    }

    /** {@inheritDoc} */
    public long getCreationDate() {
      return this.creationDate;
    }

    /** {@inheritDoc} */
    public int getConnectionsCreated() {
      return connectionsCreated;
    }

    /** {@inheritDoc} */
    public int getConnectionsDestroyed() {
      return connectionsDestroyed;
    }

    /** {@inheritDoc} */
    public int getConnectionRequestsDelayed() {
      return connectionRequestsDelayed;
    }

    /** {@inheritDoc} */
    public int getConnectionRequests() {
      return connectionRequests;
    }

    /** {@inheritDoc} */
    public int getRequestsDelayedPerSecond() {
      return requestsDelayedPerSecond;
    }

    /** {@inheritDoc} */
    public int getRequestsPerSecond() {
      return requestsPerSecond;
    }

    /** {@inheritDoc} */
    public long getAverageCheckOutTime() {
      return averageCheckOutTime;
    }

    /** {@inheritDoc} */
    public int getPoolSize() {
      return poolSize;
    }

    /** {@inheritDoc} */
    public long getResetDate() {
      return resetDate;
    }

    private ConnectionPoolStatisticsImpl(final User user) {
      this.user = user;
    }

    private void setFineGrainedStatistics(final List<ConnectionPoolState> statistics) {
      this.fineGrainedStatistics = statistics;
    }

    private void setAvailableInPool(final int availableInPool) {
      this.availableInPool = availableInPool;
    }

    private void setConnectionsInUse(final int connectionsInUse) {
      this.connectionsInUse = connectionsInUse;
    }

    private void setTimestamp(final long timestamp) {
      this.timestamp = timestamp;
    }

    private void setCreationDate(final long time) {
      this.creationDate = time;
    }

    private void setConnectionsCreated(final int connectionsCreated) {
      this.connectionsCreated = connectionsCreated;
    }

    private void setConnectionsDestroyed(final int connectionsDestroyed) {
      this.connectionsDestroyed = connectionsDestroyed;
    }

    private void setConnectionRequestsDelayed(final int connectionRequestsDelayed) {
      this.connectionRequestsDelayed = connectionRequestsDelayed;
    }

    private void setConnectionRequests(final int connectionRequests) {
      this.connectionRequests = connectionRequests;
    }

    private void setRequestsDelayedPerSecond(final int requestsDelayedPerSecond) {
      this.requestsDelayedPerSecond = requestsDelayedPerSecond;
    }

    private void setRequestsPerSecond(final int requestsPerSecond) {
      this.requestsPerSecond = requestsPerSecond;
    }

    private void setAverageCheckOutTime(final long averageCheckOutTime) {
      this.averageCheckOutTime = averageCheckOutTime;
    }

    private void setPoolSize(final int poolSize) {
      this.poolSize = poolSize;
    }

    private void setResetDate(final long resetDate) {
      this.resetDate = resetDate;
    }
  }
}