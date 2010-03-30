/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import org.jminor.common.db.DbConnection;
import org.jminor.common.db.DbConnectionProvider;
import org.jminor.common.db.User;
import org.jminor.common.db.dbms.Database;
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
 * A simple connection pool implementation, pools connections on username basis
 * User: Björn Darri
 * Date: 7.12.2007
 * Time: 00:04:08
 */
public class DbConnectionPool implements ConnectionPool {

  private static final Logger log = Util.getLogger(DbConnectionPool.class);

  private final Stack<DbConnection> connectionPool = new Stack<DbConnection>();
  private final Set<DbConnection> connectionsInUse = new HashSet<DbConnection>();

  private final List<ConnectionPoolState> connectionPoolStatistics = new ArrayList<ConnectionPoolState>(1000);
  private final Date creationDate = new Date();
  private Date resetDate = new Date();
  private ConnectionPoolSettings connectionPoolSettings;
  private boolean collectFineGrainedStatistics = System.getProperty(Database.DATABASE_POOL_STATISTICS, "true").equalsIgnoreCase("true");
  private int connectionPoolStatisticsIndex = 0;
  private int liveConnections = 0;
  private int connectionsCreated = 0;
  private int connectionsDestroyed = 0;
  private int connectionRequests = 0;
  private int connectionRequestsDelayed = 0;
  private int requestsDelayedPerSecond = 0;
  private int requestsDelayedPerSecondCounter = 0;
  private int requestsPerSecond = 0;
  private int requestsPerSecondCounter = 0;
  private long requestsPerSecondTime = System.currentTimeMillis();

  private final DbConnectionProvider dbConnectionProvider;
  private boolean closed = false;
  private int poolStatisticsSize = 1000;

  public DbConnectionPool(final DbConnectionProvider dbConnectionProvider, final ConnectionPoolSettings settings) {
    this.dbConnectionProvider = dbConnectionProvider;
    this.connectionPoolSettings = settings;
    new Timer(true).schedule(new TimerTask() {
      @Override
      public void run() {
        cleanPool();
      }
    }, new Date(), settings.getPoolCleanupInterval());
    new Timer(true).schedule(new TimerTask() {
      @Override
      public void run() {
        updateStatistics();
      }
    }, new Date(), 2550);
  }

  public DbConnection checkOutConnection() throws ClassNotFoundException, SQLException {
    if (closed)
      throw new IllegalStateException("Can not check out a connection from a closed connection pool!");

    incrementRequestCounter();
    DbConnection connection = getConnectionFromPool();
    if (connection == null) {
      incrementDelayedRequestCounter();
      synchronized (connectionPool) {
        if (liveConnections < connectionPoolSettings.getMaximumPoolSize()) {
          incrementConnectionsCreatedCounter();
          if (log.isDebugEnabled())
            log.debug("$$$$ adding a new connection to connection pool " + getConnectionPoolSettings().getUser());
          checkInConnection(dbConnectionProvider.createConnection(getConnectionPoolSettings().getUser()));
        }
      }
      int retryCount = 0;
      final long time = System.currentTimeMillis();
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

    return connection;
  }

  public void checkInConnection(final DbConnection connection) {
    if (closed)
      throw new IllegalStateException("Trying to check a connection into a closed connection pool!");

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
    connectionPoolSettings = poolSettings;
    if (!poolSettings.isEnabled())
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
    statistics.setLiveConnectionCount(liveConnections);
    statistics.setConnectionsCreated(connectionsCreated);
    statistics.setConnectionsDestroyed(connectionsDestroyed);
    statistics.setCreationDate(creationDate);
    statistics.setConnectionRequests(connectionRequests);
    statistics.setConnectionRequestsDelayed(connectionRequestsDelayed);
    statistics.setRequestsDelayedPerSecond(requestsDelayedPerSecond);
    statistics.setRequestsPerSecond(requestsPerSecond);
    statistics.setResetDate(resetDate);
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
    connectionsCreated = 0;
    connectionsDestroyed = 0;
    connectionRequests = 0;
    connectionRequestsDelayed = 0;
    resetDate = new Date();
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
    connectionsDestroyed++;
    liveConnections--;
    connection.disconnect();
  }

  private void updateStatistics() {
    final long current = System.currentTimeMillis();
    final double seconds = (current - requestsPerSecondTime) / 1000;
    if (seconds > 5) {
      requestsPerSecond = (int) ((double) requestsPerSecondCounter / seconds);
      requestsPerSecondCounter = 0;
      requestsDelayedPerSecond = (int) ((double) requestsDelayedPerSecondCounter / seconds);
      requestsDelayedPerSecondCounter = 0;
      requestsPerSecondTime = current;
    }
  }

  private void incrementConnectionsCreatedCounter() {
    liveConnections++;
    connectionsCreated++;
  }

  private void incrementDelayedRequestCounter() {
    connectionRequestsDelayed++;
    requestsDelayedPerSecondCounter++;
  }

  private void incrementRequestCounter() {
    connectionRequests++;
    requestsPerSecondCounter++;
  }
}
