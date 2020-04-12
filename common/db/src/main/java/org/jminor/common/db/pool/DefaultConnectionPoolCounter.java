/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import org.jminor.common.TaskScheduler;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A connection pool statistics collector.
 */
final class DefaultConnectionPoolCounter {

  private static final double THOUSAND = 1000d;
  private static final int SNAPSHOT_STATS_SIZE = 1000;
  private static final int SNAPSHOT_COLLECTION_INTERVAL_MS = 10;
  private static final int CHECK_OUT_TIMES_MAX_SIZE = 10000;

  private final AbstractConnectionPool connectionPool;
  private final long creationDate = System.currentTimeMillis();
  private final LinkedList<Long> checkOutTimes = new LinkedList<>();
  private final LinkedList<ConnectionPoolState> snapshotStatistics = new LinkedList<>();
  private boolean collectSnapshotStatistics = false;
  private final TaskScheduler snapshotStatisticsCollector = new TaskScheduler(new StatisticsCollector(),
          SNAPSHOT_COLLECTION_INTERVAL_MS, TimeUnit.MILLISECONDS);

  private long resetDate = creationDate;
  private int connectionsCreated = 0;
  private int connectionsDestroyed = 0;
  private int connectionRequests = 0;
  private int requestsPerSecond = 0;
  private int requestsPerSecondCounter = 0;
  private int connectionRequestsFailed = 0;
  private int requestsFailedPerSecondCounter = 0;
  private int requestsFailedPerSecond = 0;
  private long averageCheckOutTime = 0;
  private long minimumCheckOutTime = 0;
  private long maximumCheckOutTime = 0;
  private long requestsPerSecondTime = creationDate;

  DefaultConnectionPoolCounter(final AbstractConnectionPool connectionPool) {
    this.connectionPool = connectionPool;
  }

  synchronized boolean isCollectSnapshotStatistics() {
    return collectSnapshotStatistics;
  }

  synchronized void setCollectSnapshotStatistics(final boolean collectSnapshotStatistics) {
    if (collectSnapshotStatistics) {
      IntStream.range(0, SNAPSHOT_STATS_SIZE).forEach(i -> snapshotStatistics.add(new DefaultConnectionPoolState()));
      snapshotStatisticsCollector.start();
    }
    else {
      snapshotStatisticsCollector.stop();
      snapshotStatistics.clear();
    }
    this.collectSnapshotStatistics = collectSnapshotStatistics;
  }

  synchronized void addCheckOutTime(final long time) {
    checkOutTimes.add(time);
    if (checkOutTimes.size() > CHECK_OUT_TIMES_MAX_SIZE) {
      checkOutTimes.removeFirst();
    }
  }

  synchronized void incrementConnectionsDestroyedCounter() {
    connectionsDestroyed++;
  }

  synchronized void incrementConnectionsCreatedCounter() {
    connectionsCreated++;
  }

  synchronized void incrementFailedRequestCounter() {
    connectionRequestsFailed++;
    requestsFailedPerSecondCounter++;
  }

  synchronized void incrementRequestCounter() {
    connectionRequests++;
    requestsPerSecondCounter++;
  }

  synchronized void resetStatistics() {
    connectionsCreated = 0;
    connectionsDestroyed = 0;
    connectionRequests = 0;
    connectionRequestsFailed = 0;
    checkOutTimes.clear();
    resetDate = System.currentTimeMillis();
  }

  synchronized ConnectionPoolStatistics getStatistics(final long since) {
    final DefaultConnectionPoolStatistics statistics = new DefaultConnectionPoolStatistics(connectionPool.getUser().getUsername());
    updateStatistics();
    final int inPool = connectionPool.getSize();
    final int inUseCount = connectionPool.getInUse();
    statistics.setAvailableInPool(inPool);
    statistics.setConnectionsInUse(inUseCount);
    statistics.setPoolSize(inPool + inUseCount);
    statistics.setConnectionsCreated(connectionsCreated);
    statistics.setConnectionsDestroyed(connectionsDestroyed);
    statistics.setCreationDate(creationDate);
    statistics.setConnectionRequests(connectionRequests);
    statistics.setConnectionRequestsFailed(connectionRequestsFailed);
    statistics.setRequestsFailedPerSecond(requestsFailedPerSecond);
    statistics.setRequestsPerSecond(requestsPerSecond);
    statistics.setAverageCheckOutTime(averageCheckOutTime);
    statistics.setMinimumCheckOutTime(minimumCheckOutTime);
    statistics.setMaximumCheckOutTime(maximumCheckOutTime);
    statistics.setResetDate(resetDate);
    statistics.setTimestamp(System.currentTimeMillis());
    if (collectSnapshotStatistics && since >= 0) {
      statistics.setSnapshot(snapshotStatistics.stream()
              .filter(state -> state.getTimestamp() >= since).collect(Collectors.toList()));
    }

    return statistics;
  }

  private void updateStatistics() {
    final long current = System.currentTimeMillis();
    final double seconds = (current - requestsPerSecondTime) / THOUSAND;
    requestsPerSecond = (int) ((double) requestsPerSecondCounter / seconds);
    requestsPerSecondCounter = 0;
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
      for (final Long time : checkOutTimes) {
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

  private final class StatisticsCollector implements Runnable {

    /**
     * Adds the current state of the pool to the snapshot connection pool log
     */
    @Override
    public void run() {
      synchronized (DefaultConnectionPoolCounter.this) {
        snapshotStatistics.addLast(connectionPool.updateState(snapshotStatistics.removeFirst()));
      }
    }
  }
}
