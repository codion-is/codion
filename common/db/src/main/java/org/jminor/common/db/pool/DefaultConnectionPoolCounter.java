/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import org.jminor.common.TaskScheduler;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
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
  private final LinkedList<Integer> checkOutTimes = new LinkedList<>();
  private final LinkedList<ConnectionPoolState> snapshotStatistics = new LinkedList<>();
  private boolean collectSnapshotStatistics = false;
  private final TaskScheduler snapshotStatisticsCollector = new TaskScheduler(new StatisticsCollector(),
          SNAPSHOT_COLLECTION_INTERVAL_MS, TimeUnit.MILLISECONDS);

  private final AtomicLong resetDate = new AtomicLong(creationDate);
  private final AtomicLong requestsPerSecondTime = new AtomicLong(creationDate);
  private final AtomicInteger connectionsCreated = new AtomicInteger(0);
  private final AtomicInteger connectionsDestroyed = new AtomicInteger(0);
  private final AtomicInteger connectionRequests = new AtomicInteger(0);
  private final AtomicInteger requestsPerSecondCounter = new AtomicInteger(0);
  private final AtomicInteger connectionRequestsFailed = new AtomicInteger(0);
  private final AtomicInteger requestsFailedPerSecondCounter = new AtomicInteger(0);

  DefaultConnectionPoolCounter(final AbstractConnectionPool connectionPool) {
    this.connectionPool = connectionPool;
  }

  boolean isCollectSnapshotStatistics() {
    return collectSnapshotStatistics;
  }

  void setCollectSnapshotStatistics(final boolean collectSnapshotStatistics) {
    synchronized (snapshotStatistics) {
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
  }

  void addCheckOutTime(final int time) {
    synchronized (checkOutTimes) {
      checkOutTimes.add(time);
      if (checkOutTimes.size() > CHECK_OUT_TIMES_MAX_SIZE) {
        checkOutTimes.removeFirst();
      }
    }
  }

  void incrementConnectionsDestroyedCounter() {
    connectionsDestroyed.incrementAndGet();
  }

  void incrementConnectionsCreatedCounter() {
    connectionsCreated.incrementAndGet();
  }

  void incrementFailedRequestCounter() {
    connectionRequestsFailed.incrementAndGet();
    requestsFailedPerSecondCounter.incrementAndGet();
  }

  void incrementRequestCounter() {
    connectionRequests.incrementAndGet();
    requestsPerSecondCounter.incrementAndGet();
  }

  void resetStatistics() {
    connectionsCreated.set(0);
    connectionsDestroyed.set(0);
    connectionRequests.set(0);
    connectionRequestsFailed.set(0);
    synchronized (checkOutTimes) {
      checkOutTimes.clear();
    }
    resetDate.set(System.currentTimeMillis());
  }

  ConnectionPoolStatistics getStatistics(final long since) {
    final DefaultConnectionPoolStatistics statistics = new DefaultConnectionPoolStatistics(connectionPool.getUser().getUsername());
    final long current = System.currentTimeMillis();
    statistics.setTimestamp(current);
    statistics.setResetDate(resetDate.get());
    statistics.setAvailableInPool(connectionPool.getSize());
    statistics.setConnectionsInUse(connectionPool.getInUse());
    statistics.setConnectionsCreated(connectionsCreated.get());
    statistics.setConnectionsDestroyed(connectionsDestroyed.get());
    statistics.setCreationDate(creationDate);
    statistics.setConnectionRequests(connectionRequests.get());
    statistics.setConnectionRequestsFailed(connectionRequestsFailed.get());
    final double seconds = (current - requestsPerSecondTime.get()) / THOUSAND;
    requestsPerSecondTime.set(current);
    statistics.setRequestsPerSecond((int) ((double) requestsPerSecondCounter.get() / seconds));
    requestsPerSecondCounter.set(0);
    statistics.setRequestsFailedPerSecond((int) ((double) requestsFailedPerSecondCounter.get() / seconds));
    requestsFailedPerSecondCounter.set(0);
    if (!checkOutTimes.isEmpty()) {
      populateCheckOutTime(statistics);
    }
    if (collectSnapshotStatistics && since >= 0) {
      synchronized (snapshotStatistics) {
        statistics.setSnapshot(snapshotStatistics.stream()
                .filter(state -> state.getTimestamp() >= since).collect(Collectors.toList()));
      }
    }

    return statistics;
  }

  private void populateCheckOutTime(final DefaultConnectionPoolStatistics statistics) {
    int total = 0;
    int min = -1;
    int max = -1;
    synchronized (checkOutTimes) {
      for (final Integer time : checkOutTimes) {
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
      statistics.setAverageCheckOutTime(total / checkOutTimes.size());
      statistics.setMinimumCheckOutTime(min);
      statistics.setMaximumCheckOutTime(max);
      checkOutTimes.clear();
    }
  }

  private final class StatisticsCollector implements Runnable {

    /**
     * Adds the current state of the pool to the snapshot connection pool log
     */
    @Override
    public void run() {
      synchronized (snapshotStatistics) {
        snapshotStatistics.addLast(connectionPool.updateState(snapshotStatistics.removeFirst()));
      }
    }
  }
}
