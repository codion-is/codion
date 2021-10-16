/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.pool;

import is.codion.common.scheduler.TaskScheduler;

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

  private long checkOutTimerStart;

  DefaultConnectionPoolCounter(final AbstractConnectionPoolWrapper<?> connectionPool) {
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

  boolean isCollectCheckOutTimes() {
    return collectCheckOutTimes;
  }

  void setCollectCheckOutTimes(final boolean collectCheckOutTimes) {
    synchronized (checkOutTimes) {
      if (!collectCheckOutTimes) {
        checkOutTimes.clear();
      }
      this.collectCheckOutTimes = collectCheckOutTimes;
    }
  }

  void startCheckOutTimer() {
    if (collectCheckOutTimes) {
      checkOutTimerStart = System.nanoTime();
    }
  }

  void stopCheckOutTimer() {
    if (collectCheckOutTimes && checkOutTimerStart > 0L) {
      addCheckOutTime((int) (System.nanoTime() - checkOutTimerStart) / 1_000_000);
      checkOutTimerStart = 0L;
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
                .filter(state -> state.getTimestamp() >= since).collect(Collectors.toList()));
      }
    }

    return statistics;
  }

  private void addCheckOutTime(final int time) {
    if (collectCheckOutTimes) {
      synchronized (checkOutTimes) {
        checkOutTimes.add(time);
        if (checkOutTimes.size() > CHECK_OUT_TIMES_MAX_SIZE) {
          checkOutTimes.removeFirst();
        }
      }
    }
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
