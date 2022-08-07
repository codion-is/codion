/*
 * Copyright (c) 2013 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.pool;

import is.codion.common.scheduler.TaskScheduler;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

/**
 * A connection pool statistics collector.
 */
final class DefaultConnectionPoolCounter {

  private static final double THOUSAND = 1000d;
  private static final int SNAPSHOT_STATS_SIZE = 1000;
  private static final int SNAPSHOT_COLLECTION_INTERVAL_MS = 10;
  private static final int CHECK_OUT_TIMES_MAX_SIZE = 10000;

  private final AbstractConnectionPoolWrapper<?> connectionPool;
  private final long creationDate = System.currentTimeMillis();
  private final LinkedList<Integer> checkOutTimes = new LinkedList<>();
  private final LinkedList<DefaultConnectionPoolState> snapshotStatistics = new LinkedList<>();
  private volatile boolean collectSnapshotStatistics = false;
  private volatile boolean collectCheckOutTimes = false;
  private final TaskScheduler snapshotStatisticsCollector = TaskScheduler.builder(new StatisticsCollector())
          .interval(SNAPSHOT_COLLECTION_INTERVAL_MS)
          .timeUnit(TimeUnit.MILLISECONDS)
          .build();

  private final AtomicLong resetDate = new AtomicLong(creationDate);
  private final AtomicLong requestsPerSecondTime = new AtomicLong(creationDate);
  private final AtomicInteger connectionsCreated = new AtomicInteger();
  private final AtomicInteger connectionsDestroyed = new AtomicInteger();
  private final AtomicInteger connectionRequests = new AtomicInteger();
  private final AtomicInteger requestsPerSecondCounter = new AtomicInteger();
  private final AtomicInteger connectionRequestsFailed = new AtomicInteger();
  private final AtomicInteger requestsFailedPerSecondCounter = new AtomicInteger();

  DefaultConnectionPoolCounter(AbstractConnectionPoolWrapper<?> connectionPool) {
    this.connectionPool = connectionPool;
  }

  boolean isCollectSnapshotStatistics() {
    return collectSnapshotStatistics;
  }

  void setCollectSnapshotStatistics(boolean collectSnapshotStatistics) {
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

  boolean isCollectCheckOutTimes() {
    return collectCheckOutTimes;
  }

  void setCollectCheckOutTimes(boolean collectCheckOutTimes) {
    synchronized (checkOutTimes) {
      if (!collectCheckOutTimes) {
        checkOutTimes.clear();
      }
      this.collectCheckOutTimes = collectCheckOutTimes;
    }
  }

  void addCheckOutTime(int time) {
    if (collectCheckOutTimes) {
      synchronized (checkOutTimes) {
        checkOutTimes.add(time);
        if (checkOutTimes.size() > CHECK_OUT_TIMES_MAX_SIZE) {
          checkOutTimes.removeFirst();
        }
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

  ConnectionPoolStatistics getStatistics(long since) {
    DefaultConnectionPoolStatistics statistics = new DefaultConnectionPoolStatistics(connectionPool.user().username());
    long current = System.currentTimeMillis();
    statistics.setTimestamp(current);
    statistics.setResetDate(resetDate.get());
    statistics.setAvailableInPool(connectionPool.available());
    statistics.setConnectionsInUse(connectionPool.inUse());
    statistics.setConnectionsCreated(connectionsCreated.get());
    statistics.setConnectionsDestroyed(connectionsDestroyed.get());
    statistics.setCreationDate(creationDate);
    statistics.setConnectionRequests(connectionRequests.get());
    statistics.setConnectionRequestsFailed(connectionRequestsFailed.get());
    double seconds = (current - requestsPerSecondTime.get()) / THOUSAND;
    requestsPerSecondTime.set(current);
    statistics.setRequestsPerSecond((int) (requestsPerSecondCounter.get() / seconds));
    requestsPerSecondCounter.set(0);
    statistics.setRequestsFailedPerSecond((int) (requestsFailedPerSecondCounter.get() / seconds));
    requestsFailedPerSecondCounter.set(0);
    if (!checkOutTimes.isEmpty()) {
      populateCheckOutTime(statistics);
    }
    if (collectSnapshotStatistics && since >= 0) {
      synchronized (snapshotStatistics) {
        statistics.setSnapshot(snapshotStatistics.stream()
                .filter(state -> state.timestamp() >= since)
                .collect(toList()));
      }
    }

    return statistics;
  }

  private void populateCheckOutTime(DefaultConnectionPoolStatistics statistics) {
    int total = 0;
    int min = -1;
    int max = -1;
    synchronized (checkOutTimes) {
      for (Integer time : checkOutTimes) {
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
