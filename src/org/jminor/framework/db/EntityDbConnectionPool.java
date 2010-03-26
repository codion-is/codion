/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.db.ConnectionPoolSettings;
import org.jminor.common.db.ConnectionPoolState;
import org.jminor.common.db.ConnectionPoolStatistics;
import org.jminor.common.db.User;
import org.jminor.common.db.dbms.Database;
import org.jminor.common.model.Util;
import org.jminor.framework.Configuration;

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
public class EntityDbConnectionPool {

  private static final Logger log = Util.getLogger(EntityDbConnectionPool.class);

  private final Stack<EntityDbConnection> connectionPool = new Stack<EntityDbConnection>();
  private final Set<EntityDbConnection> connectionsInUse = new HashSet<EntityDbConnection>();

  private final List<ConnectionPoolState> connectionPoolStatistics = new ArrayList<ConnectionPoolState>(1000);
  private final Date creationDate = new Date();
  private Date resetDate = new Date();
  private ConnectionPoolSettings connectionPoolSettings;
  private boolean collectFineGrainedStatistics = Configuration.getBooleanValue(Configuration.SERVER_CONNECTION_POOL_STATISTICS);
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

  private final User user;
  private final Database database;
  private boolean closed = false;
  private int poolStatisticsSize = 1000;

  public EntityDbConnectionPool(final Database database, final ConnectionPoolSettings settings) {
    this.database = database;
    this.user = settings.getUser();
    final String sid = database.getSid();
    if (sid != null && sid.length() != 0)
      this.user.setProperty(Database.DATABASE_SID, sid);
    this.connectionPoolSettings = settings;
    new Timer(true).schedule(new TimerTask() {
      @Override
      public void run() {
        cleanPool(false);
      }
    }, new Date(), settings.getPoolCleanupInterval());
    new Timer(true).schedule(new TimerTask() {
      @Override
      public void run() {
        updateRequestsPerSecond();
      }
    }, new Date(), 2550);
  }

  public EntityDbConnection checkOutConnection() throws ClassNotFoundException, SQLException {
    if (closed)
      throw new IllegalStateException("Can not check out a connection from a closed connection pool!");

    connectionRequests++;
    requestsPerSecondCounter++;
    EntityDbConnection connection = getConnectionFromPool();
    if (connection == null) {
      connectionRequestsDelayed++;
      requestsDelayedPerSecondCounter++;
      synchronized (connectionPool) {
        if (liveConnections < connectionPoolSettings.getMaximumPoolSize()) {
          liveConnections++;
          connectionsCreated++;
          if (log.isDebugEnabled())
            log.debug("$$$$ adding a new connection to connection pool " + user);
          checkInConnection(new EntityDbConnection(database, user));
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
            log.debug("##### " + user + " got connection"
                    + " after " + (System.currentTimeMillis() - time) + "ms (retries: " + retryCount + ")");
        }
        catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }

    return connection;
  }

  public void checkInConnection(final EntityDbConnection connection) {
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
        connection.poolTime = System.currentTimeMillis();
        connectionPool.push(connection);
        connectionPool.notify();
      }
      else {
        if (log.isDebugEnabled())
          log.debug(user + " connection invalid upon check in");
        disconnect(connection);
      }
    }
  }

  public void setPassword(final String password) {
    this.user.setPassword(password);
  }

  public void close() {
    closed = true;
    cleanPool(true);
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
    final ConnectionPoolStatistics statistics = new ConnectionPoolStatistics(user);
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
        if (state.getTime() > since)
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

  private EntityDbConnection getConnectionFromPool() {
    synchronized (connectionPool) {
      synchronized (connectionsInUse) {
        final int connectionsInPool = connectionPool.size();
        if (collectFineGrainedStatistics)
          addInPoolStats(connectionsInPool, connectionsInUse.size(), System.currentTimeMillis());
        final EntityDbConnection dbConnection = connectionsInPool > 0 ? connectionPool.pop() : null;
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

  private void cleanPool(final boolean disconnectAll) {
    synchronized (connectionPool) {
      final long currentTime = System.currentTimeMillis();
      final ListIterator<EntityDbConnection> iterator = connectionPool.listIterator();
      while (iterator.hasNext() && connectionPool.size() > connectionPoolSettings.getMinimumPoolSize()) {
        final EntityDbConnection connection = iterator.next();
        final long idleTime = currentTime - connection.poolTime;
        if (disconnectAll || idleTime > connectionPoolSettings.getPooledConnectionTimeout()) {
          iterator.remove();
          if (log.isDebugEnabled())
            log.debug(user + " removing connection from pool, idle for " + idleTime / 1000
                  + " seconds, " + connectionPool.size() + " available");
          disconnect(connection);
        }
      }
    }
  }

  private void disconnect(final EntityDbConnection connection) {
    connectionsDestroyed++;
    liveConnections--;
    connection.disconnect();
  }

  private void updateRequestsPerSecond() {
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
}
