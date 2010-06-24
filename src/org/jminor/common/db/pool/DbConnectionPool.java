/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import org.jminor.common.db.DbConnection;
import org.jminor.common.db.DbConnectionProvider;
import org.jminor.common.db.dbms.Database;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;

import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A simple connection pool implementation, pools connections on username basis.<br>
 * User: Bjorn Darri<br>
 * Date: 7.12.2007<br>
 * Time: 00:04:08<br>
 */
public class DbConnectionPool implements ConnectionPool {

  private static final Logger LOG = Util.getLogger(DbConnectionPool.class);

  private final Stack<DbConnection> connectionPool = new Stack<DbConnection>();
  private final Set<DbConnection> connectionsInUse = new HashSet<DbConnection>();

  private final List<ConnectionPoolState> connectionPoolStatistics = new ArrayList<ConnectionPoolState>(1000);

  private Timer poolCleaner;

  private int connectionPoolStatisticsIndex = 0;
  private volatile ConnectionPoolSettings connectionPoolSettings;
  private volatile boolean collectFineGrainedStatistics = System.getProperty(Database.DATABASE_POOL_STATISTICS, "true").equalsIgnoreCase("true");

  private final DbConnectionProvider dbConnectionProvider;
  private boolean closed = false;

  private final Counter counter = new Counter();
  private volatile boolean creatingConnection = false;

  public DbConnectionPool(final DbConnectionProvider dbConnectionProvider, final ConnectionPoolSettings settings) {
    this.dbConnectionProvider = dbConnectionProvider;
    this.connectionPoolSettings = settings;
    startPoolCleaner(settings.getPoolCleanupInterval());
  }

  public DbConnection checkOutConnection() throws ClassNotFoundException, SQLException {
    if (closed) {
      throw new IllegalStateException("Can not check out a connection from a closed connection pool!");
    }

    final long time = System.nanoTime();
    counter.incrementRequestCounter();
    DbConnection connection = getConnectionFromPool();
    int retryCount = 0;
    if (connection == null) {
      counter.incrementDelayedRequestCounter();
      synchronized (connectionPool) {
        if (counter.getLiveConnections() < connectionPoolSettings.getMaximumPoolSize() && !creatingConnection) {
          try {
            creatingConnection = true;
            counter.incrementConnectionsCreatedCounter();
            checkInConnection(dbConnectionProvider.createConnection(connectionPoolSettings.getUser()));
          }
          finally {
            creatingConnection = false;
          }
        }
      }
      while (connection == null) {
        try {
          synchronized (connectionPool) {
            if (connectionPool.size() == 0) {
              connectionPool.wait();
            }
            connection = getConnectionFromPool();
            retryCount++;
          }
        }
        catch (InterruptedException e) {/**/}
      }
    }

    connection.setPoolRetryCount(retryCount);
    counter.addCheckOutTime(System.nanoTime() - time);
    return connection;
  }

  public void checkInConnection(final DbConnection dbConnection) {
    if (closed) {
      disconnect(dbConnection);
    }

    synchronized (connectionPool) {
      synchronized (connectionsInUse) {
        connectionsInUse.remove(dbConnection);
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
        dbConnection.setPoolTime(System.currentTimeMillis());
        connectionPool.push(dbConnection);
        connectionPool.notifyAll();
      }
      else {
        disconnect(dbConnection);
      }
    }
  }

  public User getUser() {
    return connectionPoolSettings.getUser();
  }

  public void close() {
    closed = true;
    emptyPool();
  }

  public void setConnectionPoolSettings(final ConnectionPoolSettings settings) {
    final boolean cleanupIntervalChanged = connectionPoolSettings.getPoolCleanupInterval() != settings.getPoolCleanupInterval();
    connectionPoolSettings.set(settings);
    if (cleanupIntervalChanged) {
      startPoolCleaner(connectionPoolSettings.getPoolCleanupInterval());
    }
    if (!connectionPoolSettings.isEnabled()) {
      close();
    }
  }

  public ConnectionPoolSettings getConnectionPoolSettings() {
    return connectionPoolSettings;
  }

  public ConnectionPoolStatistics getConnectionPoolStatistics(final long since) {
    final ConnectionPoolStatistics statistics = new ConnectionPoolStatistics(connectionPoolSettings.getUser());
    synchronized (connectionPool) {
      synchronized (connectionsInUse) {
        statistics.setConnectionsInUse(connectionsInUse.size());
        statistics.setAvailableInPool(connectionPool.size());
      }
    }
    statistics.setLiveConnectionCount(counter.getLiveConnections());
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
    if (since >= 0) {
      statistics.setPoolStatistics(getPoolStatistics(since));
    }

    return statistics;
  }

  /**
   * @param since the time
   * @return stats collected since <code>since</code>, the results are not guaranteed to be ordered
   */
  public List<ConnectionPoolState> getPoolStatistics(final long since) {
    final List<ConnectionPoolState> poolStates = new ArrayList<ConnectionPoolState>();
    synchronized (connectionPoolStatistics) {
      final ListIterator<ConnectionPoolState> iterator = connectionPoolStatistics.listIterator();
      while (iterator.hasNext()) {//NB. the stat log is circular, result should be sorted
        final ConnectionPoolState state = iterator.next();
        if (state.getTime() >= since) {
          poolStates.add(state);
        }
      }
    }

    return poolStates;
  }

  public boolean isCollectFineGrainedStatistics() {
    return collectFineGrainedStatistics;
  }

  public void setCollectFineGrainedStatistics(final boolean value) {
    this.collectFineGrainedStatistics = value;
  }

  public void resetPoolStatistics() {
    counter.resetPoolStatistics();
  }

  private DbConnection getConnectionFromPool() {
    synchronized (connectionPool) {
      synchronized (connectionsInUse) {
        final int connectionsInPool = connectionPool.size();
        if (collectFineGrainedStatistics) {
          addInPoolStats(connectionsInPool, connectionsInUse.size(), System.currentTimeMillis());
        }
        final DbConnection dbConnection = connectionsInPool > 0 ? connectionPool.pop() : null;
        if (dbConnection != null) {
          connectionsInUse.add(dbConnection);
        }

        return dbConnection;
      }
    }
  }

  private void addInPoolStats(final int size, final int inUse, final long time) {
    synchronized (connectionPoolStatistics) {
      final int poolStatisticsSize = 1000;
      connectionPoolStatisticsIndex = connectionPoolStatisticsIndex == poolStatisticsSize ? 0 : connectionPoolStatisticsIndex;
      if (connectionPoolStatistics.size() == poolStatisticsSize) //filled already, reuse
      {
        connectionPoolStatistics.get(connectionPoolStatisticsIndex).set(time, size, inUse);
      }
      else {
        connectionPoolStatistics.add(new ConnectionPoolState(time, size, inUse));
      }

      connectionPoolStatisticsIndex++;
    }
  }

  private void startPoolCleaner(final int interval) {
    if (poolCleaner != null) {
      poolCleaner.cancel();
    }

    poolCleaner = new Timer(true);
    poolCleaner.schedule(new TimerTask() {
      @Override
      public void run() {
        cleanPool();
      }
    }, new Date(), interval);
  }

  private void cleanPool() {
    synchronized (connectionPool) {
      final long currentTime = System.currentTimeMillis();
      final ListIterator<DbConnection> iterator = connectionPool.listIterator();
      while (iterator.hasNext() && connectionPool.size() > connectionPoolSettings.getMinimumPoolSize()) {
        final DbConnection connection = iterator.next();
        final long idleTime = currentTime - connection.getPoolTime();
        if (idleTime > connectionPoolSettings.getPooledConnectionTimeout()) {
          iterator.remove();
          disconnect(connection);
        }
      }
    }
  }

  private void emptyPool() {
    synchronized (connectionPool) {
      while (connectionPool.size() > 0) {
        disconnect(connectionPool.pop());
      }
    }
  }

  private void disconnect(final DbConnection connection) {
    if (connection == null) {
      return;
    }

    counter.incrementConnectionsDestroyedCounter();
    dbConnectionProvider.destroyConnection(connection);
  }

  private static class Counter {
    private final long creationDate = System.currentTimeMillis();
    private long resetDate = creationDate;
    private int liveConnections = 0;
    private int connectionsCreated = 0;
    private int connectionsDestroyed = 0;
    private int connectionRequests = 0;
    private int connectionRequestsDelayed = 0;
    private int requestsDelayedPerSecond = 0;
    private int requestsDelayedPerSecondCounter = 0;
    private int requestsPerSecond = 0;
    private int requestsPerSecondCounter = 0;
    private long averageCheckOutTime = 0;
    private final List<Long> checkOutTimes = new ArrayList<Long>();
    private long requestsPerSecondTime = creationDate;

    Counter() {
      new Timer(true).schedule(new TimerTask() {
        @Override
        public void run() {
          updateStatistics();
        }
      }, new Date(), 1000);
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

    public synchronized int getLiveConnections() {
      return liveConnections;
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
      final double seconds = (current - requestsPerSecondTime) / 1000d;
      requestsPerSecond = (int) ((double) requestsPerSecondCounter / seconds);
      requestsPerSecondCounter = 0;
      requestsDelayedPerSecond = (int) ((double) requestsDelayedPerSecondCounter / seconds);
      requestsDelayedPerSecondCounter = 0;
      requestsPerSecondTime = current;
      averageCheckOutTime = 0;
      if (checkOutTimes.size() > 0) {
        long total = 0;
        for (final Long time : checkOutTimes) {
          total += time;
        }
        averageCheckOutTime = total / checkOutTimes.size();
        checkOutTimes.clear();
      }
    }

    public synchronized void incrementConnectionsDestroyedCounter() {
      liveConnections--;
      connectionsDestroyed++;
    }

    public synchronized void incrementConnectionsCreatedCounter() {
      liveConnections++;
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