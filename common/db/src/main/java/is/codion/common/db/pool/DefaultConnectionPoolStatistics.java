/*
 * Copyright (c) 2013 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.pool;

import java.io.Serializable;
import java.util.List;

import static java.util.Collections.emptyList;

/**
 * A default ConnectionPoolStatistics implementation
 */
final class DefaultConnectionPoolStatistics implements ConnectionPoolStatistics, Serializable {

  private static final long serialVersionUID = 1;

  private final String username;
  private long timestamp;
  private int connectionsInUse;
  private int availableInPool;

  private int connectionsCreated;
  private int connectionsDestroyed;
  private long creationDate;

  private List<ConnectionPoolState> snapshot = emptyList();
  private long resetDate;
  private int connectionRequests;
  private int requestsPerSecond;
  private int connectionRequestsFailed;
  private int requestsFailedPerSecond;
  private int averageCheckOutTime = 0;
  private int minimumCheckOutTime = 0;
  private int maximumCheckOutTime = 0;

  /**
   * @param username the database user the pool is based on
   */
  DefaultConnectionPoolStatistics(String username) {
    this.username = username;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public List<ConnectionPoolState> getSnapshot() {
    return snapshot;
  }

  @Override
  public int getAvailable() {
    return availableInPool;
  }

  @Override
  public int getInUse() {
    return connectionsInUse;
  }

  @Override
  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public long getCreationDate() {
    return this.creationDate;
  }

  @Override
  public int getCreated() {
    return connectionsCreated;
  }

  @Override
  public int getDestroyed() {
    return connectionsDestroyed;
  }

  @Override
  public int getRequests() {
    return connectionRequests;
  }

  @Override
  public int getFailedRequests() {
    return connectionRequestsFailed;
  }

  @Override
  public int getFailedRequestsPerSecond() {
    return requestsFailedPerSecond;
  }

  @Override
  public int getRequestsPerSecond() {
    return requestsPerSecond;
  }

  @Override
  public long getAverageGetTime() {
    return averageCheckOutTime;
  }

  @Override
  public long getMinimumCheckOutTime() {
    return minimumCheckOutTime;
  }

  @Override
  public long getMaximumCheckOutTime() {
    return maximumCheckOutTime;
  }

  @Override
  public int getSize() {
    return connectionsInUse + availableInPool;
  }

  @Override
  public long getResetTime() {
    return resetDate;
  }

  public void setSnapshot(List<ConnectionPoolState> snapshot) {
    this.snapshot = snapshot;
  }

  public void setAvailableInPool(int availableInPool) {
    this.availableInPool = availableInPool;
  }

  public void setConnectionsInUse(int connectionsInUse) {
    this.connectionsInUse = connectionsInUse;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public void setCreationDate(long time) {
    this.creationDate = time;
  }

  public void setConnectionsCreated(int connectionsCreated) {
    this.connectionsCreated = connectionsCreated;
  }

  public void setConnectionsDestroyed(int connectionsDestroyed) {
    this.connectionsDestroyed = connectionsDestroyed;
  }

  public void setConnectionRequests(int connectionRequests) {
    this.connectionRequests = connectionRequests;
  }

  public void setRequestsPerSecond(int requestsPerSecond) {
    this.requestsPerSecond = requestsPerSecond;
  }

  public void setAverageCheckOutTime(int averageCheckOutTime) {
    this.averageCheckOutTime = averageCheckOutTime;
  }

  public void setMinimumCheckOutTime(int minimumCheckOutTime) {
    this.minimumCheckOutTime = minimumCheckOutTime;
  }

  public void setMaximumCheckOutTime(int maximumCheckOutTime) {
    this.maximumCheckOutTime = maximumCheckOutTime;
  }

  public void setResetDate(long resetDate) {
    this.resetDate = resetDate;
  }

  public void setConnectionRequestsFailed(int connectionRequestsFailed) {
    this.connectionRequestsFailed = connectionRequestsFailed;
  }

  public void setRequestsFailedPerSecond(int requestsFailedPerSecond) {
    this.requestsFailedPerSecond = requestsFailedPerSecond;
  }
}
