/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * A class encapsulating database connection pool settings and statistics
 */
public class ConnectionPoolSettings implements Serializable {

  private User user;
  private boolean enabled;
  private int pooledConnectionTimeout;
  private int maximumPoolSize;
  private int minimumPoolSize;
  private int poolCleanupInterval;

  private long timestamp;
  private int connectionsInUse;
  private int availableInPool;

  private int connectionsCreated;
  private int connectionsDestroyed;
  private Date creationDate;

  private List<ConnectionPoolState> poolStatistics;
  private Date resetDate;
  private int connectionRequests;
  private int connectionRequestsDelayed;
  private int requestsDelayedPerSecond;
  private int requestsPerSecond;
  private int liveConnectionCount;
  private int queriesPerSecond;
  private int cachedQueriesPerSecond;

  public ConnectionPoolSettings(final User user, final boolean enabled, final int pooledConnectionTimeout,
                                final int minimumPoolSize, final int poolCleanupInterval) {
    this.user = user;
    this.poolCleanupInterval = poolCleanupInterval;
    setEnabled(enabled);
    setPooledConnectionTimeout(pooledConnectionTimeout);
    setMinimumPoolSize(minimumPoolSize);
    setMaximumPoolSize(minimumPoolSize*2);
  }

  public User getUser() {
    return user;
  }

  public void setUser(final User user) {
    this.user = user;
  }

  public int getPoolCleanupInterval() {
    return poolCleanupInterval;
  }

  public void setPoolStatistics(final List<ConnectionPoolState> stats) {
    this.poolStatistics = stats;
  }

  public List<ConnectionPoolState> getPoolStatistics() {
    return poolStatistics;
  }

  public int getAvailableInPool() {
    return availableInPool;
  }

  public void setAvailableInPool(final int availableInPool) {
    this.availableInPool = availableInPool;
  }

  public int getConnectionsInUse() {
    return connectionsInUse;
  }

  public void setConnectionsInUse(final int connectionsInUse) {
    this.connectionsInUse = connectionsInUse;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(final long timestamp) {
    this.timestamp = timestamp;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
  }

  public int getMinimumPoolSize() {
    return minimumPoolSize;
  }

  public void setMinimumPoolSize(final int minimumPoolSize) {
    this.minimumPoolSize = minimumPoolSize;
  }

  public int getMaximumPoolSize() {
    return maximumPoolSize;
  }

  public void setMaximumPoolSize(final int maximumPoolSize) {
    this.maximumPoolSize = maximumPoolSize;
  }

  public int getPooledConnectionTimeout() {
    return pooledConnectionTimeout;
  }

  public void setPooledConnectionTimeout(final int pooledConnectionTimeout) {
    this.pooledConnectionTimeout = pooledConnectionTimeout;
  }

  public void setCreationDate(final Date time) {
    this.creationDate = time;
  }

  public Date getCreationDate() {
    return this.creationDate;
  }

  public void setConnectionsCreated(final int connectionsCreated) {
    this.connectionsCreated = connectionsCreated;
  }

  public int getConnectionsCreated() {
    return connectionsCreated;
  }

  public void setConnectionsDestroyed(final int connectionsDestroyed) {
    this.connectionsDestroyed = connectionsDestroyed;
  }

  public int getConnectionsDestroyed() {
    return connectionsDestroyed;
  }

  public int getConnectionRequestsDelayed() {
    return connectionRequestsDelayed;
  }

  public void setConnectionRequestsDelayed(final int connectionRequestsDelayed) {
    this.connectionRequestsDelayed = connectionRequestsDelayed;
  }

  public int getConnectionRequests() {
    return connectionRequests;
  }

  public void setConnectionRequests(final int connectionRequests) {
    this.connectionRequests = connectionRequests;
  }

  public int getRequestsDelayedPerSecond() {
    return requestsDelayedPerSecond;
  }

  public void setRequestsDelayedPerSecond(final int requestsDelayedPerSecond) {
    this.requestsDelayedPerSecond = requestsDelayedPerSecond;
  }

  public void setRequestsPerSecond(int requestsPerSecond) {
    this.requestsPerSecond = requestsPerSecond;
  }

  public int getRequestsPerSecond() {
    return requestsPerSecond;
  }

  public int getQueriesPerSecond() {
    return queriesPerSecond;
  }

  public int getCachedQueriesPerSecond() {
    return cachedQueriesPerSecond;
  }

  public void setCachedQueriesPerSecond(int cachedQueriesPerSecond) {
    this.cachedQueriesPerSecond = cachedQueriesPerSecond;
  }

  public void setQueriesPerSecond(int queriesPerSecond) {
    this.queriesPerSecond = queriesPerSecond;
  }

  public void setLiveConnectionCount(final int liveConnectionCount) {
    this.liveConnectionCount = liveConnectionCount;
  }

  public int getLiveConnectionCount() {
    return liveConnectionCount;
  }

  public void setResetDate(Date resetDate) {
    this.resetDate = resetDate;
  }

  public Date getResetDate() {
    return resetDate;
  }

  public static ConnectionPoolSettings getDefault(final User user) {
    return new ConnectionPoolSettings(user, true, 60000, 4, 20000);
  }
}