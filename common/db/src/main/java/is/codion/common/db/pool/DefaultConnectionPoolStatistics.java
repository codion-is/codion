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
  public String username() {
    return username;
  }

  @Override
  public List<ConnectionPoolState> snapshot() {
    return snapshot;
  }

  @Override
  public int available() {
    return availableInPool;
  }

  @Override
  public int inUse() {
    return connectionsInUse;
  }

  @Override
  public long timestamp() {
    return timestamp;
  }

  @Override
  public long creationDate() {
    return this.creationDate;
  }

  @Override
  public int created() {
    return connectionsCreated;
  }

  @Override
  public int destroyed() {
    return connectionsDestroyed;
  }

  @Override
  public int requests() {
    return connectionRequests;
  }

  @Override
  public int failedRequests() {
    return connectionRequestsFailed;
  }

  @Override
  public int failedRequestsPerSecond() {
    return requestsFailedPerSecond;
  }

  @Override
  public int requestsPerSecond() {
    return requestsPerSecond;
  }

  @Override
  public long averageGetTime() {
    return averageCheckOutTime;
  }

  @Override
  public long minimumCheckOutTime() {
    return minimumCheckOutTime;
  }

  @Override
  public long maximumCheckOutTime() {
    return maximumCheckOutTime;
  }

  @Override
  public int size() {
    return connectionsInUse + availableInPool;
  }

  @Override
  public long resetTime() {
    return resetDate;
  }

  void setSnapshot(List<ConnectionPoolState> snapshot) {
    this.snapshot = snapshot;
  }

  void setAvailableInPool(int availableInPool) {
    this.availableInPool = availableInPool;
  }

  void setConnectionsInUse(int connectionsInUse) {
    this.connectionsInUse = connectionsInUse;
  }

  void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  void setCreationDate(long time) {
    this.creationDate = time;
  }

  void setConnectionsCreated(int connectionsCreated) {
    this.connectionsCreated = connectionsCreated;
  }

  void setConnectionsDestroyed(int connectionsDestroyed) {
    this.connectionsDestroyed = connectionsDestroyed;
  }

  void setConnectionRequests(int connectionRequests) {
    this.connectionRequests = connectionRequests;
  }

  void setRequestsPerSecond(int requestsPerSecond) {
    this.requestsPerSecond = requestsPerSecond;
  }

  void setAverageCheckOutTime(int averageCheckOutTime) {
    this.averageCheckOutTime = averageCheckOutTime;
  }

  void setMinimumCheckOutTime(int minimumCheckOutTime) {
    this.minimumCheckOutTime = minimumCheckOutTime;
  }

  void setMaximumCheckOutTime(int maximumCheckOutTime) {
    this.maximumCheckOutTime = maximumCheckOutTime;
  }

  void setResetDate(long resetDate) {
    this.resetDate = resetDate;
  }

  void setConnectionRequestsFailed(int connectionRequestsFailed) {
    this.connectionRequestsFailed = connectionRequestsFailed;
  }

  void setRequestsFailedPerSecond(int requestsFailedPerSecond) {
    this.requestsFailedPerSecond = requestsFailedPerSecond;
  }
}
