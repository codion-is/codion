/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import java.io.Serializable;
import java.util.List;

import static java.util.Collections.emptyList;

/**
 * A default ConnectionPoolStatistics implementation
 */
public final class DefaultConnectionPoolStatistics implements ConnectionPoolStatistics, Serializable {

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
  private int poolSize;
  private long averageCheckOutTime;
  private long minimumCheckOutTime;
  private long maximumCheckOutTime;

  /**
   * @param username the database user the pool is based on
   */
  public DefaultConnectionPoolStatistics(final String username) {
    this.username = username;
  }

  /** {@inheritDoc} */
  @Override
  public String getUsername() {
    return username;
  }

  /** {@inheritDoc} */
  @Override
  public List<ConnectionPoolState> getSnapshot() {
    return snapshot;
  }

  /** {@inheritDoc} */
  @Override
  public int getAvailable() {
    return availableInPool;
  }

  /** {@inheritDoc} */
  @Override
  public int getInUse() {
    return connectionsInUse;
  }

  /** {@inheritDoc} */
  @Override
  public long getTimestamp() {
    return timestamp;
  }

  /** {@inheritDoc} */
  @Override
  public long getCreationDate() {
    return this.creationDate;
  }

  /** {@inheritDoc} */
  @Override
  public int getCreated() {
    return connectionsCreated;
  }

  /** {@inheritDoc} */
  @Override
  public int getDestroyed() {
    return connectionsDestroyed;
  }

  /** {@inheritDoc} */
  @Override
  public int getRequests() {
    return connectionRequests;
  }

  /** {@inheritDoc} */
  @Override
  public int getFailedRequests() {
    return connectionRequestsFailed;
  }

  /** {@inheritDoc} */
  @Override
  public int getFailedRequestsPerSecond() {
    return requestsFailedPerSecond;
  }

  /** {@inheritDoc} */
  @Override
  public int getRequestsPerSecond() {
    return requestsPerSecond;
  }

  /** {@inheritDoc} */
  @Override
  public long getAverageGetTime() {
    return averageCheckOutTime;
  }

  /** {@inheritDoc} */
  @Override
  public long getMinimumCheckOutTime() {
    return minimumCheckOutTime;
  }

  /** {@inheritDoc} */
  @Override
  public long getMaximumCheckOutTime() {
    return maximumCheckOutTime;
  }

  /** {@inheritDoc} */
  @Override
  public int getSize() {
    return poolSize;
  }

  /** {@inheritDoc} */
  @Override
  public long getResetTime() {
    return resetDate;
  }

  public void setSnapshot(final List<ConnectionPoolState> snapshot) {
    this.snapshot = snapshot;
  }

  public void setAvailableInPool(final int availableInPool) {
    this.availableInPool = availableInPool;
  }

  public void setConnectionsInUse(final int connectionsInUse) {
    this.connectionsInUse = connectionsInUse;
  }

  public void setTimestamp(final long timestamp) {
    this.timestamp = timestamp;
  }

  public void setCreationDate(final long time) {
    this.creationDate = time;
  }

  public void setConnectionsCreated(final int connectionsCreated) {
    this.connectionsCreated = connectionsCreated;
  }

  public void setConnectionsDestroyed(final int connectionsDestroyed) {
    this.connectionsDestroyed = connectionsDestroyed;
  }

  public void setConnectionRequests(final int connectionRequests) {
    this.connectionRequests = connectionRequests;
  }

  public void setRequestsPerSecond(final int requestsPerSecond) {
    this.requestsPerSecond = requestsPerSecond;
  }

  public void setAverageCheckOutTime(final long averageCheckOutTime) {
    this.averageCheckOutTime = averageCheckOutTime;
  }

  public void setMinimumCheckOutTime(final long minimumCheckOutTime) {
    this.minimumCheckOutTime = minimumCheckOutTime;
  }

  public void setMaximumCheckOutTime(final long maximumCheckOutTime) {
    this.maximumCheckOutTime = maximumCheckOutTime;
  }

  public void setPoolSize(final int poolSize) {
    this.poolSize = poolSize;
  }

  public void setResetDate(final long resetDate) {
    this.resetDate = resetDate;
  }

  public void setConnectionRequestsFailed(final int connectionRequestsFailed) {
    this.connectionRequestsFailed = connectionRequestsFailed;
  }

  public void setRequestsFailedPerSecond(final int requestsFailedPerSecond) {
    this.requestsFailedPerSecond = requestsFailedPerSecond;
  }
}
