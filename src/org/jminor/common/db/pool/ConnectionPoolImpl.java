/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import org.jminor.common.db.Database;
import org.jminor.common.db.PoolableConnection;
import org.jminor.common.db.PoolableConnectionProvider;
import org.jminor.common.model.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.*;

/**
 * A simple connection pool implementation, pools connections on username basis.
 */
final class ConnectionPoolImpl implements ConnectionPool {

  private static final Logger LOG = LoggerFactory.getLogger(ConnectionPoolImpl.class);

  public static final int DEFAULT_CONNECTION_TIMEOUT_MS = 60000;
  public static final int DEFAULT_CLEANUP_INTERVAL_MS = 20000;
  public static final int DEFAULT_MAXIMUM_POOL_SIZE = 8;
  public static final int DEFAULT_MAXIMUM_RETRY_WAIT_PERIOD_MS = 50;
  public static final int DEFAULT_MAXIMUM_CHECK_OUT_TIME = 2000;
  public static final int DEFAULT_NEW_CONNECTION_THRESHOLD = 500;

  private static final int FINE_GRAINED_STATS_SIZE = 1000;
  private static final long NANO_IN_MILLI = 1000000;

  private final PoolableConnectionProvider connectionProvider;
  private final User user;
  private final Stack<PoolableConnection> pool = new Stack<PoolableConnection>();
  private final Collection<PoolableConnection> inUse = new ArrayList<PoolableConnection>();
  private final Counter counter = new Counter();
  private final Random random = new Random();

  private Timer cleanupTimer;

  private volatile int minimumPoolSize = DEFAULT_MAXIMUM_POOL_SIZE / 2;
  private volatile int maximumPoolSize = DEFAULT_MAXIMUM_POOL_SIZE;
  private volatile int maximumRetryWaitPeriod = DEFAULT_MAXIMUM_RETRY_WAIT_PERIOD_MS;
  private volatile int poolCleanupInterval = DEFAULT_CLEANUP_INTERVAL_MS;
  private volatile int pooledConnectionTimeout = DEFAULT_CONNECTION_TIMEOUT_MS;
  private volatile int maximumCheckOuttime = DEFAULT_MAXIMUM_CHECK_OUT_TIME;
  private volatile int newConnectionThreshold = DEFAULT_NEW_CONNECTION_THRESHOLD;

  private volatile boolean creatingConnection = false;
  private boolean enabled = true;
  private boolean closed = false;

  private final List<ConnectionPoolStateImpl> connectionPoolStatistics = new ArrayList<ConnectionPoolStateImpl>(1000);
  private int currentPoolStatisticsIndex = 0;
  private boolean collectFineGrainedStatistics = System.getProperty(Database.DATABASE_POOL_STATISTICS, "false").equalsIgnoreCase("true");

  /**
   * Instantiates a new ConnectionPoolImpl.
   * @param connectionProvider the connection provider
   * @param user the user this pool is based on
   */
  ConnectionPoolImpl(final PoolableConnectionProvider connectionProvider, final User user) {
    this.user = user;
    this.connectionProvider = connectionProvider;
    for (int i = 0; i < FINE_GRAINED_STATS_SIZE; i++) {
      connectionPoolStatistics.add(new ConnectionPoolStateImpl());
    }
    startPoolCleaner();
  }

  /** {@inheritDoc} */
  public PoolableConnection getConnection() throws ClassNotFoundException, SQLException {
    if (!enabled || closed) {
      throw new IllegalStateException("ConnectionPool not enabled or closed");
    }
    counter.incrementRequestCounter();

    final long nanoStartTime = System.nanoTime();
    if (collectFineGrainedStatistics) {
      addPoolStatistics(System.currentTimeMillis());
    }

    PoolableConnection connection = fetchFromPool();
    if (connection == null) {
      counter.incrementDelayedRequestCounter();
    }
    long elapsedNanoTime = System.nanoTime() - nanoStartTime;
    int retryCount = 0;
    boolean keepTrying = connection == null;
    while (keepTrying) {
      retryCount++;
      if (isNewConnectionWarranted(elapsedNanoTime / NANO_IN_MILLI)) {
        connection = createConnection();
      }
      else {
        waitBeforeRetry();
        connection = fetchFromPool();
      }
      elapsedNanoTime = System.nanoTime() - nanoStartTime;
      keepTrying = connection == null && (elapsedNanoTime / NANO_IN_MILLI) < maximumCheckOuttime;
    }

    if (connection != null) {
      counter.addCheckOutTime(elapsedNanoTime / NANO_IN_MILLI);
      connection.setRetryCount(retryCount);

      return connection;
    }
    counter.incrementFailedRequestCounter();

    throw new ConnectionPoolException.NoConnectionAvailable(retryCount, elapsedNanoTime / NANO_IN_MILLI);
  }

  /** {@inheritDoc} */
  public void returnConnection(final PoolableConnection connection) {
    if (connection.isTransactionOpen()) {
      throw new RuntimeException("Open transaction");
    }
    if (closed || !connection.isValid()) {
      synchronized (pool) {
        inUse.remove(connection);
      }
      connectionProvider.destroyConnection(connection);
      counter.incrementConnectionsDestroyedCounter();

      return;
    }
    synchronized (pool) {
      inUse.remove(connection);
      pool.push(connection);
      connection.setPoolTime(System.currentTimeMillis());
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
  public ConnectionPoolStatistics getStatistics(final long since) {
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
      statistics.setConnectionRequestsFailed(counter.getConnectionRequestsFailed());
      statistics.setRequestsFailedPerSecond(counter.getRequestsFailedPerSecond());
      statistics.setRequestsPerSecond(counter.getRequestsPerSecond());
      statistics.setAverageCheckOutTime(counter.getAverageCheckOutTime());
      statistics.setMininumCheckOutTime(counter.getMinimumCheckOutTime());
      statistics.setMaximumCheckOutTime(counter.getMaximumCheckOutTime());
      statistics.setResetDate(counter.getResetDate());
      statistics.setTimestamp(System.currentTimeMillis());
    }
    if (collectFineGrainedStatistics && since >= 0) {
      statistics.setFineGrainedStatistics(getFineGrainedStatistics(since));
    }

    return statistics;
  }

  /** {@inheritDoc} */
  public void resetStatistics() {
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
  public int getCleanupInterval() {
    return poolCleanupInterval;
  }

  /** {@inheritDoc} */
  public void setCleanupInterval(final int poolCleanupInterval) {
    if (poolCleanupInterval != this.poolCleanupInterval) {
      this.poolCleanupInterval = poolCleanupInterval;
      startPoolCleaner();
    }
  }

  /** {@inheritDoc} */
  public int getConnectionTimeout() {
    return pooledConnectionTimeout;
  }

  /** {@inheritDoc} */
  public void setConnectionTimeout(final int timeout) {
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

  /** {@inheritDoc} */
  public int getNewConnectionThreshold() {
    return newConnectionThreshold;
  }

  /** {@inheritDoc} */
  public void setNewConnectionThreshold(final int value) {
    if (value < 0 || value >= maximumCheckOuttime) {
      throw new IllegalArgumentException("Wait time before new connection must be larger than zero and smaller than maximumCheckOutTime");
    }
    this.newConnectionThreshold = value;
  }

  private PoolableConnection fetchFromPool() {
    PoolableConnection connection = null;
    boolean destroyConnection = false;
    synchronized (pool) {
      if (pool.size() > 0) {
        connection = pool.pop();
        if (!connection.isValid()) {
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

  private PoolableConnection createConnection() throws ClassNotFoundException, SQLException {
    synchronized (pool) {
      creatingConnection = true;
    }
    try {
      final PoolableConnection connection = connectionProvider.createConnection(user);
      counter.incrementConnectionsCreatedCounter();
      inUse.add(connection);

      return connection;
    }
    catch (SQLException sqle) {
      LOG.error("Database error while creating a new connection", sqle);
      throw sqle;
    }
    catch (ClassNotFoundException e) {
      LOG.error("JDBC Driver class not found", e);
      throw e;
    }
    finally {
      creatingConnection = false;
    }
  }

  private void waitBeforeRetry() {
    try {
      Thread.sleep(random.nextInt(maximumRetryWaitPeriod));
    }
    catch (InterruptedException e) {/**/}
  }

  private boolean isNewConnectionWarranted(final long elapsedTime) {
    if (elapsedTime < newConnectionThreshold) {
      return false;
    }
    synchronized (pool) {
      return !creatingConnection && (pool.size() + inUse.size() ) < maximumPoolSize;
    }
  }

  private void addPoolStatistics(final long currentTime) {
    synchronized (pool) {
      if (currentPoolStatisticsIndex == FINE_GRAINED_STATS_SIZE) {
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
        if (state.getTimestamp() >= since) {
          poolStates.add(state);
        }
      }
    }

    return poolStates;
  }

  private void cleanPool() {
    final long currentTime = System.currentTimeMillis();
    synchronized (pool) {
      final int inUseCount = inUse.size();
      final ListIterator<PoolableConnection> iterator = pool.listIterator();
      while (iterator.hasNext() && pool.size() + inUseCount > minimumPoolSize) {
        final PoolableConnection connection = iterator.next();
        if (currentTime - connection.getPoolTime() > pooledConnectionTimeout) {
          connectionProvider.destroyConnection(connection);
          counter.incrementConnectionsDestroyedCounter();
          iterator.remove();
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
    private int connectionsCreated = 0;
    private int connectionsDestroyed = 0;
    private int connectionRequests = 0;
    private int connectionRequestsDelayed = 0;
    private int requestsDelayedPerSecond = 0;
    private int requestsDelayedPerSecondCounter = 0;
    private int requestsPerSecond = 0;
    private int requestsPerSecondCounter = 0;
    private int connectionRequestsFailed = 0;
    private int requestsFailedPerSecondCounter = 0;
    private int requestsFailedPerSecond;
    private long averageCheckOutTime = 0;
    private long minimumCheckOutTime = 0;
    private long maximumCheckOutTime = 0;
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

    private synchronized int getConnectionRequestsFailed() {
      return connectionRequestsFailed;
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

    private synchronized int getRequestsFailedPerSecond() {
      return requestsFailedPerSecond;
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
      connectionRequestsFailed = 0;
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
      requestsFailedPerSecond = (int) ((double) requestsFailedPerSecondCounter / seconds);
      requestsFailedPerSecondCounter = 0;
      requestsPerSecondTime = current;
      averageCheckOutTime = 0;
      minimumCheckOutTime = 0;
      maximumCheckOutTime = 0;
      if (!checkOutTimes.isEmpty()) {
        long total = 0;
        long min = -1;
        long max = -1;
        for (final Long time : checkOutTimes) {
          total += time;
          if (min == -1) {
            min = time;
            max = time;
          }
          else {
            min = Math.min(min, time);
            max = Math.max(max, time);
          }
        }
        averageCheckOutTime = total / checkOutTimes.size();
        minimumCheckOutTime = min;
        maximumCheckOutTime = max;
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

    private synchronized void incrementFailedRequestCounter() {
      connectionRequestsFailed++;
      requestsFailedPerSecondCounter++;
    }

    private synchronized void incrementRequestCounter() {
      connectionRequests++;
      requestsPerSecondCounter++;
    }

    private synchronized long getAverageCheckOutTime() {
      return averageCheckOutTime;
    }

    private synchronized long getMinimumCheckOutTime() {
      return minimumCheckOutTime;
    }

    private synchronized long getMaximumCheckOutTime() {
      return maximumCheckOutTime;
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
    public int getSize() {
      return connectionCount;
    }

    /** {@inheritDoc} */
    public int getInUse() {
      return connectionsInUse;
    }

    /** {@inheritDoc} */
    public long getTimestamp() {
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

    private List<ConnectionPoolState> fineGrainedStatistics = Collections.emptyList();
    private long resetDate;
    private int connectionRequests;
    private int requestsPerSecond;
    private int connectionRequestsDelayed;
    private int requestsDelayedPerSecond;
    private int connectionRequestsFailed;
    private int requestsFailedPerSecond;
    private int poolSize;
    private long averageCheckOutTime;
    private long mininumCheckOutTime;
    private long maximumCheckOutTime;

    /** {@inheritDoc} */
    public User getUser() {
      return user;
    }

    /** {@inheritDoc} */
    public List<ConnectionPoolState> getFineGrainedStatistics() {
      return fineGrainedStatistics;
    }

    /** {@inheritDoc} */
    public int getAvailable() {
      return availableInPool;
    }

    /** {@inheritDoc} */
    public int getInUse() {
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
    public int getCreated() {
      return connectionsCreated;
    }

    /** {@inheritDoc} */
    public int getDestroyed() {
      return connectionsDestroyed;
    }

    /** {@inheritDoc} */
    public int getDelayedRequests() {
      return connectionRequestsDelayed;
    }

    /** {@inheritDoc} */
    public int getRequests() {
      return connectionRequests;
    }

    /** {@inheritDoc} */
    public int getDelayedRequestsPerSecond() {
      return requestsDelayedPerSecond;
    }

    /** {@inheritDoc} */
    public int getFailedRequests() {
      return connectionRequestsFailed;
    }

    /** {@inheritDoc} */
    public int getFailedRequestsPerSecond() {
      return requestsFailedPerSecond;
    }

    /** {@inheritDoc} */
    public int getRequestsPerSecond() {
      return requestsPerSecond;
    }

    /** {@inheritDoc} */
    public long getAverageGetTime() {
      return averageCheckOutTime;
    }

    /** {@inheritDoc} */
    public long getMininumCheckOutTime() {
      return mininumCheckOutTime;
    }

    /** {@inheritDoc} */
    public long getMaximumCheckOutTime() {
      return maximumCheckOutTime;
    }

    /** {@inheritDoc} */
    public int getSize() {
      return poolSize;
    }

    /** {@inheritDoc} */
    public long getResetTime() {
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

    private void setMininumCheckOutTime(final long mininumCheckOutTime) {
      this.mininumCheckOutTime = mininumCheckOutTime;
    }

    private void setMaximumCheckOutTime(final long maximumCheckOutTime) {
      this.maximumCheckOutTime = maximumCheckOutTime;
    }

    private void setPoolSize(final int poolSize) {
      this.poolSize = poolSize;
    }

    private void setResetDate(final long resetDate) {
      this.resetDate = resetDate;
    }

    private void setConnectionRequestsFailed(final int connectionRequestsFailed) {
      this.connectionRequestsFailed = connectionRequestsFailed;
    }

    private void setRequestsFailedPerSecond(final int requestsFailedPerSecond) {
      this.requestsFailedPerSecond = requestsFailedPerSecond;
    }
  }
}