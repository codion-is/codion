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

  private static final Logger log = Util.getLogger(DbConnectionPool.class);

  private final Stack<DbConnection> connectionPool = new Stack<DbConnection>();
  private final Set<DbConnection> connectionsInUse = new HashSet<DbConnection>();

  private final List<ConnectionPoolState> connectionPoolStatistics = new ArrayList<ConnectionPoolState>(1000);

  private Timer poolCleaner;

  private int connectionPoolStatisticsIndex = 0;
  private ConnectionPoolSettings connectionPoolSettings;
  private boolean collectFineGrainedStatistics = System.getProperty(Database.DATABASE_POOL_STATISTICS, "true").equalsIgnoreCase("true");

  private final DbConnectionProvider dbConnectionProvider;
  private boolean closed = false;
  private int poolStatisticsSize = 1000;

  private final Counter counter = new Counter();
  private volatile boolean creatingConnection = false;

  public DbConnectionPool(final DbConnectionProvider dbConnectionProvider, final ConnectionPoolSettings settings) {
    this.dbConnectionProvider = dbConnectionProvider;
    this.connectionPoolSettings = settings;
    startPoolCleaner(settings.getPoolCleanupInterval());
  }

  public DbConnection checkOutConnection() throws ClassNotFoundException, SQLException {
    if (closed)
      throw new IllegalStateException("Can not check out a connection from a closed connection pool!");

    final long time = System.nanoTime();
    int retryCount = 0;
    counter.incrementRequestCounter();
    DbConnection connection = getConnectionFromPool();
    if (connection == null) {
      counter.incrementDelayedRequestCounter();
      synchronized (connectionPool) {
        if (counter.getLiveConnections() < connectionPoolSettings.getMaximumPoolSize() && !creatingConnection) {
          try {
            creatingConnection = true;
            counter.incrementConnectionsCreatedCounter();
            if (log.isDebugEnabled())
              log.debug("$$$$ adding a new connection to connection pool " + getConnectionPoolSettings().getUser());
            checkInConnection(dbConnectionProvider.createConnection(getConnectionPoolSettings().getUser()));
          }
          finally {
            creatingConnection = false;
          }
        }
      }
      while (connection == null) {
        try {
          synchronized (connectionPool) {
            if (connectionPool.size() == 0)
              connectionPool.wait();
            connection = getConnectionFromPool();
            retryCount++;
          }
          if (connection != null && log.isDebugEnabled())
            log.debug("##### " + getConnectionPoolSettings().getUser() + " got connection"
                    + " after " + (System.currentTimeMillis() - time) + "ms (retries: " + retryCount + ")");
        }
        catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }

    connection.setPoolRetryCount(retryCount);
    counter.addCheckOutTime(System.nanoTime() - time);
    return connection;
  }

  public void checkInConnection(final DbConnection connection) {
    if (closed)
      disconnect(connection);

    synchronized (connectionPool) {
      synchronized (connectionsInUse) {
        connectionsInUse.remove(connection);
      }
      if (connection.isConnectionValid()) {
        try {
          if (connection.isTransactionOpen())
            connection.rollbackTransaction();
        }
        catch (SQLException e) {
          log.error(this, e);
        }
        connection.setPoolTime(System.currentTimeMillis());
        connectionPool.push(connection);
        connectionPool.notify();
      }
      else {
        if (log.isDebugEnabled())
          log.debug(getConnectionPoolSettings().getUser() + " connection invalid upon check in");
        disconnect(connection);
      }
    }
  }

  public User getUser() {
    return getConnectionPoolSettings().getUser();
  }

  public void close() {
    closed = true;
    emptyPool();
  }

  public void setConnectionPoolSettings(final ConnectionPoolSettings poolSettings) {
    final boolean cleanupIntervalChanged = connectionPoolSettings.getPoolCleanupInterval() != poolSettings.getPoolCleanupInterval();
    connectionPoolSettings.set(poolSettings);
    if (cleanupIntervalChanged)
      startPoolCleaner(connectionPoolSettings.getPoolCleanupInterval());
    if (!connectionPoolSettings.isEnabled())
      close();
  }

  public ConnectionPoolSettings getConnectionPoolSettings() {
    return connectionPoolSettings;
  }

  public ConnectionPoolStatistics getConnectionPoolStatistics(final long since) {
    final ConnectionPoolStatistics statistics = new ConnectionPoolStatistics(getConnectionPoolSettings().getUser());
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
    if (since >= 0)
      statistics.setPoolStatistics(getPoolStatistics(since));

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
        if (state.getTime() >= since)
          poolStates.add(state);
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
        if (collectFineGrainedStatistics)
          addInPoolStats(connectionsInPool, connectionsInUse.size(), System.currentTimeMillis());
        final DbConnection dbConnection = connectionsInPool > 0 ? connectionPool.pop() : null;
        if (dbConnection != null)
          connectionsInUse.add(dbConnection);

        return dbConnection;
      }
    }
  }

  private void addInPoolStats(final int size, final int inUse, final long time) {
    synchronized (connectionPoolStatistics) {
      connectionPoolStatisticsIndex = connectionPoolStatisticsIndex == poolStatisticsSize ? 0 : connectionPoolStatisticsIndex;
      if (connectionPoolStatistics.size() == poolStatisticsSize) //filled already, reuse
        connectionPoolStatistics.get(connectionPoolStatisticsIndex).set(time, size, inUse);
      else
        connectionPoolStatistics.add(new ConnectionPoolState(time, size, inUse));

      connectionPoolStatisticsIndex++;
    }
  }

  private void startPoolCleaner(final int interval) {
    if (poolCleaner != null)
      poolCleaner.cancel();

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
          if (log.isDebugEnabled())
            log.debug(getConnectionPoolSettings().getUser() + " removing connection from pool, idle for " + idleTime / 1000
                    + " seconds, " + connectionPool.size() + " available");
          disconnect(connection);
        }
      }
    }
  }

  private void emptyPool() {
    synchronized (connectionPool) {
      while (connectionPool.size() > 0)
        disconnect(connectionPool.pop());
    }
  }

  private void disconnect(final DbConnection connection) {
    if (connection == null)
      return;

    counter.incrementConnectionsDestroyedCounter();
    dbConnectionProvider.destroyConnection(connection);
  }

  private static class Counter {
    private final Date creationDate = new Date();
    private Date resetDate = new Date();
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
    private long requestsPerSecondTime = System.currentTimeMillis();

    public Counter() {
      new Timer(true).schedule(new TimerTask() {
        @Override
        public void run() {
          updateStatistics();
        }
      }, new Date(), 1000);
    }

    public Date getCreationDate() {
      return creationDate;
    }

    public Date getResetDate() {
      return resetDate;
    }

    public int getConnectionRequests() {
      return connectionRequests;
    }

    public int getConnectionRequestsDelayed() {
      return connectionRequestsDelayed;
    }

    public int getConnectionsCreated() {
      return connectionsCreated;
    }

    public int getConnectionsDestroyed() {
      return connectionsDestroyed;
    }

    public int getLiveConnections() {
      return liveConnections;
    }

    public int getRequestsDelayedPerSecond() {
      return requestsDelayedPerSecond;
    }

    public int getRequestsPerSecond() {
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
      resetDate = new Date();
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

    public long getAverageCheckOutTime() {
      return averageCheckOutTime;
    }
  }
}