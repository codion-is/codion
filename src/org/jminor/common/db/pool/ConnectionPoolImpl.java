/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import org.jminor.common.db.Database;
import org.jminor.common.db.DatabaseConnection;
import org.jminor.common.db.DatabaseConnectionProvider;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.TaskScheduler;
import org.jminor.common.model.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.concurrent.TimeUnit;

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

  private final DatabaseConnectionProvider connectionProvider;
  private final Deque<DatabaseConnection> pool = new ArrayDeque<DatabaseConnection>();
  private final Collection<DatabaseConnection> inUse = new ArrayList<DatabaseConnection>();
  private final Counter counter = new Counter();
  private final Random random = new Random();

  private final TaskScheduler poolCleanupScheduler = new TaskScheduler(new Runnable() {
    @Override
    public void run() {
      cleanPool();
    }
  }, DEFAULT_CLEANUP_INTERVAL_MS, TimeUnit.MILLISECONDS).start();

  private volatile int minimumPoolSize = DEFAULT_MAXIMUM_POOL_SIZE / 2;
  private volatile int maximumPoolSize = DEFAULT_MAXIMUM_POOL_SIZE;
  private volatile int maximumRetryWaitPeriod = DEFAULT_MAXIMUM_RETRY_WAIT_PERIOD_MS;
  private volatile int pooledConnectionTimeout = DEFAULT_CONNECTION_TIMEOUT_MS;
  private volatile int maximumCheckOutTime = DEFAULT_MAXIMUM_CHECK_OUT_TIME;
  private volatile int newConnectionThreshold = DEFAULT_NEW_CONNECTION_THRESHOLD;

  private volatile boolean creatingConnection = false;
  private boolean enabled = true;
  private boolean closed = false;

  private final LinkedList<ConnectionPoolStateImpl> connectionPoolStatistics = new LinkedList<ConnectionPoolStateImpl>();
  private boolean collectFineGrainedStatistics = System.getProperty(Database.DATABASE_POOL_STATISTICS, "false").equalsIgnoreCase("true");

  /**
   * Instantiates a new ConnectionPoolImpl.
   * @param connectionProvider the connection provider
   * @throws DatabaseException in case of an exception while constructing the initial connections
   */
  ConnectionPoolImpl(final DatabaseConnectionProvider connectionProvider) throws DatabaseException {
    this.connectionProvider = connectionProvider;
    for (int i = 0; i < FINE_GRAINED_STATS_SIZE; i++) {
      connectionPoolStatistics.add(new ConnectionPoolStateImpl());
    }
    initializeConnections();
  }

  /** {@inheritDoc} */
  @Override
  public DatabaseConnection getConnection() throws DatabaseException {
    if (!enabled || closed) {
      throw new IllegalStateException("ConnectionPool not enabled or closed");
    }
    counter.incrementRequestCounter();

    final long nanoStartTime = System.nanoTime();
    if (collectFineGrainedStatistics) {
      addPoolStatistics(System.currentTimeMillis());
    }

    DatabaseConnection connection = fetchFromPool();
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
      keepTrying = connection == null && (elapsedNanoTime / NANO_IN_MILLI) < maximumCheckOutTime;
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
  @Override
  public void returnConnection(final DatabaseConnection connection) {
    if (connection.isTransactionOpen()) {
      throw new IllegalStateException("Open transaction");
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
  @Override
  public User getUser() {
    return connectionProvider.getUser();
  }

  /** {@inheritDoc} */
  @Override
  public void close() {
    closed = true;
    poolCleanupScheduler.stop();
    synchronized (pool) {
      while (!pool.isEmpty()) {
        connectionProvider.destroyConnection(pool.pop());
        counter.incrementConnectionsDestroyedCounter();
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public ConnectionPoolStatistics getStatistics(final long since) {
    final ConnectionPoolStatisticsImpl statistics = new ConnectionPoolStatisticsImpl(getUser());
    counter.updateStatistics();
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

  /** {@inheritDoc} */
  @Override
  public void resetStatistics() {
    counter.resetPoolStatistics();
  }

  /** {@inheritDoc} */
  @Override
  public boolean isCollectFineGrainedStatistics() {
    return collectFineGrainedStatistics;
  }

  /** {@inheritDoc} */
  @Override
  public void setCollectFineGrainedStatistics(final boolean collectFineGrainedStatistics) {
    this.collectFineGrainedStatistics = collectFineGrainedStatistics;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isEnabled() {
    return enabled;
  }

  /** {@inheritDoc} */
  @Override
  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
    if (enabled) {
      closed = false;
    }
    else {
      close();
    }
  }

  /** {@inheritDoc} */
  @Override
  public int getCleanupInterval() {
    return poolCleanupScheduler.getInterval();
  }

  /** {@inheritDoc} */
  @Override
  public void setCleanupInterval(final int poolCleanupInterval) {
    poolCleanupScheduler.setInterval(poolCleanupInterval);
  }

  /** {@inheritDoc} */
  @Override
  public int getConnectionTimeout() {
    return pooledConnectionTimeout;
  }

  /** {@inheritDoc} */
  @Override
  public void setConnectionTimeout(final int timeout) {
    this.pooledConnectionTimeout = timeout;
  }

  /** {@inheritDoc} */
  @Override
  public int getMaximumRetryWaitPeriod() {
    return maximumRetryWaitPeriod;
  }

  /** {@inheritDoc} */
  @Override
  public void setMaximumRetryWaitPeriod(final int maximumRetryWaitPeriod) {
    this.maximumRetryWaitPeriod = maximumRetryWaitPeriod;
  }

  /** {@inheritDoc} */
  @Override
  public int getMinimumPoolSize() {
    return minimumPoolSize;
  }

  /** {@inheritDoc} */
  @Override
  public void setMinimumPoolSize(final int value) {
    if (value > maximumPoolSize || value < 0) {
      throw new IllegalArgumentException("Minimum pool size must be a positive integer an be less than maximum pool size");
    }
    this.minimumPoolSize = value;
  }

  /** {@inheritDoc} */
  @Override
  public int getMaximumPoolSize() {
    return maximumPoolSize;
  }

  /** {@inheritDoc} */
  @Override
  public void setMaximumPoolSize(final int value) {
    if (value < minimumPoolSize || value < 1) {
      throw new IllegalArgumentException("Maximum pool size must be at least 1 and larger than or equal to the minimum pool size");
    }
    this.maximumPoolSize = value;
  }

  /** {@inheritDoc} */
  @Override
  public int getMaximumCheckOutTime() {
    return maximumCheckOutTime;
  }

  /** {@inheritDoc} */
  @Override
  public void setMaximumCheckOutTime(final int value) {
    if (value < 0) {
      throw new IllegalArgumentException("Maximum check out time must be a positive integer");
    }
    this.maximumCheckOutTime = value;
  }

  /** {@inheritDoc} */
  @Override
  public int getNewConnectionThreshold() {
    return newConnectionThreshold;
  }

  /** {@inheritDoc} */
  @Override
  public void setNewConnectionThreshold(final int value) {
    if (value < 0 || value >= maximumCheckOutTime) {
      throw new IllegalArgumentException("Wait time before new connection must be larger than zero and less than or equal to maximumCheckOutTime");
    }
    this.newConnectionThreshold = value;
  }

  private DatabaseConnection fetchFromPool() {
    DatabaseConnection connection = null;
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

  private DatabaseConnection createConnection() throws DatabaseException {
    synchronized (pool) {
      creatingConnection = true;
    }
    try {
      final DatabaseConnection connection = connectionProvider.createConnection();
      counter.incrementConnectionsCreatedCounter();
      inUse.add(connection);

      return connection;
    }
    catch (DatabaseException dbe) {
      LOG.error("Database error while creating a new connection", dbe);
      throw dbe;
    }
    finally {
      creatingConnection = false;
    }
  }

  private void initializeConnections() throws DatabaseException {
    for (int i = 0; i < getMinimumPoolSize(); i++) {
      returnConnection(createConnection());
    }
  }

  private void waitBeforeRetry() {
    try {
      Thread.sleep(random.nextInt(maximumRetryWaitPeriod));
    }
    catch (InterruptedException ignored) {}
  }

  private boolean isNewConnectionWarranted(final long elapsedTime) {
    if (elapsedTime < newConnectionThreshold) {
      return false;
    }
    synchronized (pool) {
      return !creatingConnection && (pool.size() + inUse.size()) < maximumPoolSize;
    }
  }

  private void addPoolStatistics(final long currentTime) {
    synchronized (pool) {
      final ConnectionPoolStateImpl state = connectionPoolStatistics.removeFirst();
      state.set(currentTime, pool.size(), inUse.size());
      connectionPoolStatistics.addLast(state);
    }
  }

  /**
   * @param since the time
   * @return stats collected since <code>since</code>
   */
  private List<ConnectionPoolState> getFineGrainedStatistics(final long since) {
    final List<ConnectionPoolState> poolStates;
    synchronized (pool) {
      poolStates = new LinkedList<ConnectionPoolState>(connectionPoolStatistics);
    }
    final ListIterator<ConnectionPoolState> iterator = poolStates.listIterator();
    while (iterator.hasNext() && iterator.next().getTimestamp() < since) {
      iterator.remove();
    }

    return poolStates;
  }

  private void cleanPool() {
    final long currentTime = System.currentTimeMillis();
    synchronized (pool) {
      final int inUseCount = inUse.size();
      final Collection<DatabaseConnection> pooledConnections = new ArrayList<DatabaseConnection>(pool);
      for (final DatabaseConnection connection : pooledConnections) {
        if (pool.size() + inUseCount <= minimumPoolSize) {
          return;
        }
        if (currentTime - connection.getPoolTime() > pooledConnectionTimeout) {
          connectionProvider.destroyConnection(connection);//todo could be spun of into a thread, if the operation is expensive
          counter.incrementConnectionsDestroyedCounter();
          pool.remove(connection);
        }
      }
    }
  }

  private static final class Counter {
    private static final double THOUSAND = 1000d;

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
    @Override
    public int getSize() {
      return connectionCount;
    }

    /** {@inheritDoc} */
    @Override
    public int getInUse() {
      return connectionsInUse;
    }

    /** {@inheritDoc} */
    @Override
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
    private long minimumCheckOutTime;
    private long maximumCheckOutTime;

    /** {@inheritDoc} */
    @Override
    public User getUser() {
      return user;
    }

    /** {@inheritDoc} */
    @Override
    public List<ConnectionPoolState> getFineGrainedStatistics() {
      return fineGrainedStatistics;
    }

    /** {@inheritDoc} */
    @Override
    public int getAvailable() {
      return availableInPool;
    }

    /** {@inheritDoc} */
    @Override
    public int getInUse() {
      return connectionsInUse;
    }

    /** {@inheritDoc} */
    @Override
    public long getTimestamp() {
      return timestamp;
    }

    /** {@inheritDoc} */
    @Override
    public long getCreationDate() {
      return this.creationDate;
    }

    /** {@inheritDoc} */
    @Override
    public int getCreated() {
      return connectionsCreated;
    }

    /** {@inheritDoc} */
    @Override
    public int getDestroyed() {
      return connectionsDestroyed;
    }

    /** {@inheritDoc} */
    @Override
    public int getDelayedRequests() {
      return connectionRequestsDelayed;
    }

    /** {@inheritDoc} */
    @Override
    public int getRequests() {
      return connectionRequests;
    }

    /** {@inheritDoc} */
    @Override
    public int getDelayedRequestsPerSecond() {
      return requestsDelayedPerSecond;
    }

    /** {@inheritDoc} */
    @Override
    public int getFailedRequests() {
      return connectionRequestsFailed;
    }

    /** {@inheritDoc} */
    @Override
    public int getFailedRequestsPerSecond() {
      return requestsFailedPerSecond;
    }

    /** {@inheritDoc} */
    @Override
    public int getRequestsPerSecond() {
      return requestsPerSecond;
    }

    /** {@inheritDoc} */
    @Override
    public long getAverageGetTime() {
      return averageCheckOutTime;
    }

    /** {@inheritDoc} */
    @Override
    public long getMinimumCheckOutTime() {
      return minimumCheckOutTime;
    }

    /** {@inheritDoc} */
    @Override
    public long getMaximumCheckOutTime() {
      return maximumCheckOutTime;
    }

    /** {@inheritDoc} */
    @Override
    public int getSize() {
      return poolSize;
    }

    /** {@inheritDoc} */
    @Override
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

    private void setMinimumCheckOutTime(final long minimumCheckOutTime) {
      this.minimumCheckOutTime = minimumCheckOutTime;
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