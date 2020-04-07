/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import org.jminor.common.TaskScheduler;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A connection pool statistics collector.
 */
final class DefaultConnectionPoolCounter {

  private static final double THOUSAND = 1000d;
  private static final int FINE_GRAINED_STATS_SIZE = 1000;
  private static final int FINE_GRAINED_COLLECTION_INTERVAL = 10;

  private final AbstractConnectionPool connectionPool;
  private final long creationDate = System.currentTimeMillis();
  private final LinkedList<ConnectionPoolState> fineGrainedStatistics = new LinkedList<>();
  private boolean collectFineGrainedStatistics = false;
  private final TaskScheduler fineGrainedStatisticsCollector = new TaskScheduler(new StatisticsCollector(),
          FINE_GRAINED_COLLECTION_INTERVAL, TimeUnit.MILLISECONDS);

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
  private final List<Long> checkOutTimes = new ArrayList<>();
  private long requestsPerSecondTime = creationDate;

  DefaultConnectionPoolCounter(final AbstractConnectionPool connectionPool) {
    this.connectionPool = connectionPool;
  }

  synchronized boolean isCollectFineGrainedStatistics() {
    return collectFineGrainedStatistics;
  }

  synchronized void setCollectFineGrainedStatics(final boolean collectFineGrainedStatistics) {
    if (collectFineGrainedStatistics) {
      IntStream.range(0, FINE_GRAINED_STATS_SIZE).forEach(i -> fineGrainedStatistics.add(new DefaultConnectionPoolState()));
      fineGrainedStatisticsCollector.start();
    }
    else {
      fineGrainedStatisticsCollector.stop();
      fineGrainedStatistics.clear();
    }
    this.collectFineGrainedStatistics = collectFineGrainedStatistics;
  }

  synchronized void addCheckOutTime(final long time) {
    checkOutTimes.add(time);
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

  synchronized void resetPoolStatistics() {
    connectionsCreated = 0;
    connectionsDestroyed = 0;
    connectionRequests = 0;
    connectionRequestsFailed = 0;
    checkOutTimes.clear();
    resetDate = System.currentTimeMillis();
  }

  synchronized ConnectionPoolStatistics getStatistics(final long since) {
    final DefaultConnectionPoolStatistics statistics = new DefaultConnectionPoolStatistics(connectionPool.getUser());
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
    if (collectFineGrainedStatistics && since >= 0) {
      statistics.setFineGrainedStatistics(fineGrainedStatistics.stream()
              .filter(state -> state.getTimestamp() >= since).collect(Collectors.toList()));
    }

    return statistics;
  }

  private synchronized void addFineGrainedStatistics() {
    fineGrainedStatistics.addLast(connectionPool.updateState(fineGrainedStatistics.removeFirst()));
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

  private final class StatisticsCollector implements Runnable {

    /**
     * Adds the current state of the pool to the fine grained connection pool log
     */
    @Override
    public void run() {
      addFineGrainedStatistics();
    }
  }
}
