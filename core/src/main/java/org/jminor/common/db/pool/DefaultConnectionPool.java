/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import org.jminor.common.db.DatabaseConnection;
import org.jminor.common.db.DatabaseConnectionProvider;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.TaskScheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * A simple connection pool implementation, pools connections on username basis.
 */
final class DefaultConnectionPool extends AbstractConnectionPool<Deque<DatabaseConnection>> {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultConnectionPool.class);

  private static final int DEFAULT_CONNECTION_TIMEOUT_MS = 60000;
  private static final int DEFAULT_CLEANUP_INTERVAL_MS = 20000;
  private static final int DEFAULT_MAXIMUM_POOL_SIZE = 8;
  private static final int DEFAULT_MAXIMUM_RETRY_WAIT_PERIOD_MS = 50;
  private static final int DEFAULT_MAXIMUM_CHECK_OUT_TIME = 2000;
  private static final int DEFAULT_NEW_CONNECTION_THRESHOLD = 100;
  private static final int NEW_CONNECTION_THRESHOLD_RATIO = 4;
  private static final long NANO_IN_MILLI = 1000000;

  private final DatabaseConnectionProvider connectionProvider;
  private final Collection<DatabaseConnection> inUse = new ArrayList<>();
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
  private boolean closed = false;

  /**
   * Instantiates a new DefaultConnectionPool.
   * @param connectionProvider the connection provider
   * @throws DatabaseException in case of an exception while constructing the initial connections
   */
  DefaultConnectionPool(final DatabaseConnectionProvider connectionProvider) throws DatabaseException {
    super(new ArrayDeque<DatabaseConnection>(), connectionProvider.getUser());
    this.connectionProvider = connectionProvider;
    initializeConnections();
  }

  /** {@inheritDoc} */
  @Override
  public Connection getConnection() throws DatabaseException {
    return getDatabaseConnection().getConnection();
  }

  /** {@inheritDoc} */
  @Override
  public void returnConnection(final Connection connection) {
    returnConnection(findConnection(connection));
  }

  /** {@inheritDoc} */
  @Override
  public void close() {
    closed = true;
    poolCleanupScheduler.stop();
    synchronized (pool) {
      while (!pool.isEmpty()) {
        connectionProvider.destroyConnection(pool.pop());
        getCounter().incrementConnectionsDestroyedCounter();
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  protected int getSize() {
    return pool.size();
  }

  /** {@inheritDoc} */
  @Override
  protected int getInUse() {
    return inUse.size();
  }

  /** {@inheritDoc} */
  @Override
  protected int getWaiting() {
    return 0;
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
    if (newConnectionThreshold > this.maximumCheckOutTime) {
      newConnectionThreshold = Math.max(0, this.maximumCheckOutTime / NEW_CONNECTION_THRESHOLD_RATIO);
    }
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

  DatabaseConnection getDatabaseConnection() throws DatabaseException {
    if (closed) {
      throw new IllegalStateException("ConnectionPool is closed");
    }
    getCounter().incrementRequestCounter();

    final long nanoStartTime = System.nanoTime();
    if (isCollectFineGrainedStatistics()) {
      addPoolStatistics();
    }

    DatabaseConnection connection = fetchFromPool();
    if (connection == null) {
      getCounter().incrementDelayedRequestCounter();
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
      getCounter().addCheckOutTime(elapsedNanoTime / NANO_IN_MILLI);
      connection.setRetryCount(retryCount);

      return connection;
    }
    getCounter().incrementFailedRequestCounter();

    throw new ConnectionPoolException.NoConnectionAvailable(retryCount, elapsedNanoTime / NANO_IN_MILLI);
  }

  void returnConnection(final DatabaseConnection databaseConnection) {
    if (databaseConnection.isTransactionOpen()) {
      throw new IllegalStateException("Open transaction");
    }
    if (closed || !databaseConnection.isConnected()) {
      synchronized (pool) {
        inUse.remove(databaseConnection);
      }
      connectionProvider.destroyConnection(databaseConnection);
      getCounter().incrementConnectionsDestroyedCounter();
    }
    else {
      synchronized (pool) {
        inUse.remove(databaseConnection);
        pool.push(databaseConnection);
        databaseConnection.setPoolTime(System.currentTimeMillis());
      }
    }
  }

  private DatabaseConnection fetchFromPool() {
    DatabaseConnection connection = null;
    boolean destroyConnection = false;
    synchronized (pool) {
      if (pool.size() > 0) {
        connection = pool.pop();
        if (!connection.isConnected()) {
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
        getCounter().incrementConnectionsDestroyedCounter();
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
      getCounter().incrementConnectionsCreatedCounter();
      inUse.add(connection);

      return connection;
    }
    catch (final DatabaseException dbe) {
      LOG.error("Database error while creating a new connection", dbe);
      throw dbe;
    }
    finally {
      creatingConnection = false;
    }
  }

  private DatabaseConnection findConnection(final Connection connection) {
    synchronized (pool) {
      for (final DatabaseConnection databaseConnection : inUse) {
        if (databaseConnection.getConnection().equals(connection)) {
          return databaseConnection;
        }
      }
    }

    throw new IllegalStateException("Connection not in use: " + connection);
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
    catch (final InterruptedException ignored) {/*ignored*/}
  }

  private boolean isNewConnectionWarranted(final long elapsedTime) {
    if (elapsedTime < newConnectionThreshold) {
      return false;
    }
    synchronized (pool) {
      return !creatingConnection && (pool.size() + inUse.size()) < maximumPoolSize;
    }
  }

  private void cleanPool() {
    final long currentTime = System.currentTimeMillis();
    synchronized (pool) {
      final int inUseCount = inUse.size();
      final Collection<DatabaseConnection> pooledConnections = new ArrayList<>(pool);
      for (final DatabaseConnection connection : pooledConnections) {
        if (pool.size() + inUseCount <= minimumPoolSize) {
          return;
        }
        if (currentTime - connection.getPoolTime() > pooledConnectionTimeout) {
          connectionProvider.destroyConnection(connection);//todo could be spun off in a thread, if the operation is expensive
          getCounter().incrementConnectionsDestroyedCounter();
          pool.remove(connection);
        }
      }
    }
  }
}