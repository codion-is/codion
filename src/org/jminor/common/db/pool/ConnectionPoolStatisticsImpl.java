/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import org.jminor.common.model.User;

import java.io.Serializable;
import java.util.List;

/**
 * User: Björn Darri<br>
 * Date: 14.7.2010<br>
 * Time: 23:16:34
 */
public class ConnectionPoolStatisticsImpl implements ConnectionPoolStatistics, Serializable {
  private static final long serialVersionUID = 1;

  private final User user;
  private long timestamp;
  private int connectionsInUse;
  private int availableInPool;

  private int connectionsCreated;
  private int connectionsDestroyed;
  private long creationDate;

  private List<ConnectionPoolState> poolStatistics;
  private long resetDate;
  private int connectionRequests;
  private int connectionRequestsDelayed;
  private int requestsDelayedPerSecond;
  private int requestsPerSecond;
  private int liveConnectionCount;
  private long averageCheckOutTime;

  public ConnectionPoolStatisticsImpl(final User user) {
    this.user = user;
  }

  public User getUser() {
    return user;
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

  public void setCreationDate(final long time) {
    this.creationDate = time;
  }

  public long getCreationDate() {
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

  public long getAverageCheckOutTime() {
    return averageCheckOutTime;
  }

  public void setAverageCheckOutTime(final long averageCheckOutTime) {
    this.averageCheckOutTime = averageCheckOutTime;
  }

  public void setLiveConnectionCount(final int liveConnectionCount) {
    this.liveConnectionCount = liveConnectionCount;
  }

  public int getLiveConnectionCount() {
    return liveConnectionCount;
  }

  public void setResetDate(final long resetDate) {
    this.resetDate = resetDate;
  }

  public long getResetDate() {
    return resetDate;
  }
}
