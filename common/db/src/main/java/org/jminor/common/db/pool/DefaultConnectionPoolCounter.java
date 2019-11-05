/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import java.util.ArrayList;
import java.util.List;

/**
 * A default ConnectionPool.Counter implementation
 */
final class DefaultConnectionPoolCounter implements ConnectionPool.Counter {

  private static final double THOUSAND = 1000d;

  private final long creationDate = System.currentTimeMillis();
  private long resetDate = creationDate;
  private int connectionsCreated = 0;
  private int connectionsDestroyed = 0;
  private int connectionRequests = 0;
  private int connectionRequestsDelayed = 0;
  private int requestsDelayedPerSecond = 0;
  private int requestsDelayedPerSecondCounter = 0;
  private int requestsPerSecond = 0;
  private int requestsPerSecondCounter = 0;
  private int connectionRequestsFailed = 0;
  private int requestsFailedPerSecondCounter = 0;
  private int requestsFailedPerSecond = 0;
  private long averageCheckOutTime = 0;
  private long minimumCheckOutTime = 0;
  private long maximumCheckOutTime = 0;
  private final List<Long> checkOutTimes = new ArrayList<>();
  private long requestsPerSecondTime = creationDate;

  /** {@inheritDoc} */
  @Override
  public synchronized void addCheckOutTime(final long time) {
    checkOutTimes.add(time);
  }

  /** {@inheritDoc} */
  @Override
  public synchronized void incrementConnectionsDestroyedCounter() {
    connectionsDestroyed++;
  }

  /** {@inheritDoc} */
  @Override
  public synchronized void incrementConnectionsCreatedCounter() {
    connectionsCreated++;
  }

  /** {@inheritDoc} */
  @Override
  public synchronized void incrementDelayedRequestCounter() {
    connectionRequestsDelayed++;
    requestsDelayedPerSecondCounter++;
  }

  /** {@inheritDoc} */
  @Override
  public synchronized void incrementFailedRequestCounter() {
    connectionRequestsFailed++;
    requestsFailedPerSecondCounter++;
  }

  /** {@inheritDoc} */
  @Override
  public synchronized void incrementRequestCounter() {
    connectionRequests++;
    requestsPerSecondCounter++;
  }

  synchronized void resetPoolStatistics() {
    connectionsCreated = 0;
    connectionsDestroyed = 0;
    connectionRequests = 0;
    connectionRequestsDelayed = 0;
    connectionRequestsFailed = 0;
    checkOutTimes.clear();
    resetDate = System.currentTimeMillis();
  }

  synchronized void updateStatistics() {
    final long current = System.currentTimeMillis();
    final double seconds = (current - requestsPerSecondTime) / THOUSAND;
    requestsPerSecond = (int) ((double) requestsPerSecondCounter / seconds);
    requestsPerSecondCounter = 0;
    requestsDelayedPerSecond = (int) ((double) requestsDelayedPerSecondCounter / seconds);
    requestsDelayedPerSecondCounter = 0;
    requestsFailedPerSecond = (int) ((double) requestsFailedPerSecondCounter / seconds);
    requestsFailedPerSecondCounter = 0;
    requestsPerSecondTime = current;
    averageCheckOutTime = 0;
    minimumCheckOutTime = 0;
    maximumCheckOutTime = 0;
    if (!checkOutTimes.isEmpty()) {
      long total = 0;
      long min = -1;
      long max = -1;
      for (int i = 0; i < checkOutTimes.size(); i++){
        final Long time = checkOutTimes.get(i);
        total += time;
        if (min == -1) {
          min = time;
          max = time;
        }
        else {
          min = Math.min(min, time);
          max = Math.max(max, time);
        }
      }
      averageCheckOutTime = total / checkOutTimes.size();
      minimumCheckOutTime = min;
      maximumCheckOutTime = max;
      checkOutTimes.clear();
    }
  }

  synchronized long getCreationDate() {
    return creationDate;
  }

  synchronized long getResetDate() {
    return resetDate;
  }

  synchronized int getConnectionRequests() {
    return connectionRequests;
  }

  synchronized int getConnectionRequestsDelayed() {
    return connectionRequestsDelayed;
  }

  synchronized int getConnectionRequestsFailed() {
    return connectionRequestsFailed;
  }

  synchronized int getConnectionsCreated() {
    return connectionsCreated;
  }

  synchronized int getConnectionsDestroyed() {
    return connectionsDestroyed;
  }

  synchronized int getRequestsDelayedPerSecond() {
    return requestsDelayedPerSecond;
  }

  synchronized int getRequestsFailedPerSecond() {
    return requestsFailedPerSecond;
  }

  synchronized int getRequestsPerSecond() {
    return requestsPerSecond;
  }

  synchronized long getAverageCheckOutTime() {
    return averageCheckOutTime;
  }

  synchronized long getMinimumCheckOutTime() {
    return minimumCheckOutTime;
  }

  synchronized long getMaximumCheckOutTime() {
    return maximumCheckOutTime;
  }
}
