/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.db.ConnectionPoolSettings;
import org.jminor.common.db.ConnectionPoolState;
import org.jminor.common.db.Database;
import org.jminor.common.db.User;
import org.jminor.common.db.UserAccessException;
import org.jminor.common.model.Util;
import org.jminor.framework.FrameworkSettings;
import org.jminor.framework.model.EntityRepository;

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

  private final List<ConnectionPoolState> inPoolStats = new ArrayList<ConnectionPoolState>(1000);
  private final Date creationDate = new Date();
  private Date resetDate = new Date();
  private ConnectionPoolSettings connectionPoolSettings;
  private int poolStatisticsIndex = 0;
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
  private boolean closed = false;
  private int poolStatisticsSize = 1000;

  public EntityDbConnectionPool(final User user, final ConnectionPoolSettings settings) {
    this.user = user;
    this.user.setProperty(Database.DATABASE_SID_PROPERTY,
            System.getProperty(Database.DATABASE_SID_PROPERTY));
    this.connectionPoolSettings = settings;
    new Timer(true).schedule(new TimerTask() {
      public void run() {
        cleanPool(false);
      }
    }, new Date(), settings.getPoolCleanupInterval());
    new Timer(true).schedule(new TimerTask() {
      public void run() {
        updateRequestsPerSecond();
      }
    }, new Date(), 2550);
  }

  public EntityDbConnection checkOutConnection(final EntityRepository repository, final FrameworkSettings settings)
          throws ClassNotFoundException, UserAccessException {
    if (closed)
      throw new IllegalStateException("Trying to check out a connection from a closed connection pool!");

    connectionRequests++;
    requestsPerSecondCounter++;
    EntityDbConnection ret = getConnectionFromPool();
    if (ret == null) {
      connectionRequestsDelayed++;
      requestsDelayedPerSecondCounter++;
      synchronized (connectionPool) {
        if (liveConnections < connectionPoolSettings.getMaximumPoolSize()) {
          liveConnections++;
          connectionsCreated++;
          if (log.isDebugEnabled())
            log.debug("$$$$ creating a new connection for " + user);
          checkInConnection(new EntityDbConnection(user, repository, settings));
        }
      }
      int retryCount = 0;
      final long time = System.currentTimeMillis();
      while (ret == null) {
        try {
          synchronized (connectionPool) {
            if (connectionPool.size() == 0)
              connectionPool.wait();
            ret = getConnectionFromPool();
            retryCount++;
          }
          if (ret != null && log.isDebugEnabled())
            log.debug("##### " + user + " got connection"
                    + " after " + (System.currentTimeMillis() - time) + "ms (count: " + retryCount + ")");
        }
        catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
    ret.initialize(repository, settings);

    return ret;
  }

  public void checkInConnection(final EntityDbConnection connection) {
    if (closed)
      throw new IllegalStateException("Trying to check a connection into a closed connection pool!");

    synchronized (connectionPool) {
      synchronized (connectionsInUse) {
        connectionsInUse.remove(connection);
      }
      if (connection.isConnectionValid()) {
        connection.clearStateData();
        try {
          if (connection.isTransactionOpen())
            connection.endTransaction(true);
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

  public void resetPoolStatistics() {
    connectionsCreated = 0;
    connectionsDestroyed = 0;
    connectionRequests = 0;
    connectionRequestsDelayed = 0;
    resetDate = new Date();
  }

  /**
   * @param since the time
   * @return stats collected since <code>since</code>, the results are not garanteed to be ordered
   */
  public List<ConnectionPoolState> getPoolStatistics(final long since) {
    final List<ConnectionPoolState> ret = new ArrayList<ConnectionPoolState>();
    synchronized (inPoolStats) {
      final ListIterator<ConnectionPoolState> iterator = inPoolStats.listIterator();
      while (iterator.hasNext()) {//NB. the stat log is circular, result should be sorted
        final ConnectionPoolState state = iterator.next();
        if (state.time > since)
          ret.add(state);
      }
    }

    return ret;
  }

  public void setConnectionPoolSettings(final ConnectionPoolSettings poolSettings) {
    connectionPoolSettings = poolSettings;
    if (!poolSettings.isEnabled())
      close();
  }

  public ConnectionPoolSettings getConnectionPoolSettings() {
    return getConnectionPoolSettings(false, -1);
  }

  public ConnectionPoolSettings getConnectionPoolSettings(final boolean includeStatistics, final long since) {
    if (includeStatistics) {
      synchronized (connectionPool) {
        synchronized (connectionsInUse) {
          connectionPoolSettings.setConnectionsInUse(connectionsInUse.size());
          connectionPoolSettings.setAvailableInPool(connectionPool.size());
        }
      }
      connectionPoolSettings.setLiveConnectionCount(liveConnections);
      connectionPoolSettings.setConnectionsCreated(connectionsCreated);
      connectionPoolSettings.setConnectionsDestroyed(connectionsDestroyed);
      connectionPoolSettings.setCreationDate(creationDate);
      connectionPoolSettings.setConnectionRequests(connectionRequests);
      connectionPoolSettings.setConnectionRequestsDelayed(connectionRequestsDelayed);
      connectionPoolSettings.setRequestsDelayedPerSecond(requestsDelayedPerSecond);
      connectionPoolSettings.setRequestsPerSecond(requestsPerSecond);
      connectionPoolSettings.setQueriesPerSecond(EntityDbConnection.getQueriesPerSecond());
      connectionPoolSettings.setCachedQueriesPerSecond(EntityDbConnection.getCachedQueriesPerSecond());
      connectionPoolSettings.setResetDate(resetDate);
      connectionPoolSettings.setTimestamp(System.currentTimeMillis());
      if (since >= 0)
        connectionPoolSettings.setPoolStatistics(getPoolStatistics(since));
    }

    return connectionPoolSettings;
  }

  private EntityDbConnection getConnectionFromPool() {
    synchronized (connectionPool) {
      synchronized (connectionsInUse) {
        final int connectionsInPool = connectionPool.size();
        addInPoolStats(connectionsInPool, connectionsInUse.size(), System.currentTimeMillis());
        final EntityDbConnection ret = connectionsInPool > 0 ? connectionPool.pop() : null;
        if (ret != null)
          connectionsInUse.add(ret);

        return ret;
      }
    }
  }

  private void addInPoolStats(final int size, final int inUse, final long time) {
    synchronized (inPoolStats) {
      poolStatisticsIndex = poolStatisticsIndex == poolStatisticsSize ? 0 : poolStatisticsIndex;
      if (inPoolStats.size() == poolStatisticsSize) //filled already, reuse
        inPoolStats.get(poolStatisticsIndex).set(time, size, inUse);
      else
        inPoolStats.add(new ConnectionPoolState(time, size, inUse));

      poolStatisticsIndex++;
    }
  }

  private void cleanPool(final boolean disconnectAll) {
    synchronized (connectionPool) {
      final long currentTime = System.currentTimeMillis();
      final ListIterator<EntityDbConnection> iterator = connectionPool.listIterator();
      while (iterator.hasNext() && connectionPool.size() > connectionPoolSettings.getMinimumPoolSize()) {
        final EntityDbConnection connection = iterator.next();
        final long idleTime = currentTime - connection.getPoolTime();
        if (disconnectAll || idleTime > connectionPoolSettings.getPooledConnectionTimeout()) {
          iterator.remove();
          if (log.isDebugEnabled())
            log.debug(user + " removing connection from pool, idle for " + idleTime/1000
                  + " seconds, " + connectionPool.size() + " available");
          disconnect(connection);
        }
      }
    }
  }

  private void disconnect(EntityDbConnection connection) {
    connectionsDestroyed++;
    liveConnections--;
    connection.disconnect();
  }

  private void updateRequestsPerSecond() {
    final long current = System.currentTimeMillis();
    final double seconds = (current - requestsPerSecondTime)/1000;
    if (seconds > 5) {
      requestsPerSecond = (int) ((double) requestsPerSecondCounter/seconds);
      requestsPerSecondCounter = 0;
      requestsDelayedPerSecond = (int) ((double) requestsDelayedPerSecondCounter/seconds);
      requestsDelayedPerSecondCounter = 0;
      requestsPerSecondTime = current;
    }
  }
}
