/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.scheduler;

import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

final class DefaultTaskScheduler implements TaskScheduler {

  private final Object lock = new Object();
  private final Runnable task;
  private final int initialDelay;
  private final TimeUnit timeUnit;
  private final ThreadFactory threadFactory;
  private final Event<Integer> intervalChangedEvent = Event.event();

  private ScheduledExecutorService executorService;
  private int interval;

  DefaultTaskScheduler(Runnable task, int interval, int initialDelay, TimeUnit timeUnit,
                       ThreadFactory threadFactory) {
    if (interval <= 0) {
      throw new IllegalArgumentException("Interval must be a positive integer");
    }
    if (initialDelay < 0) {
      throw new IllegalArgumentException("Initial delay can not be negative");
    }
    this.task = requireNonNull(task, "task");
    this.interval = interval;
    this.initialDelay = initialDelay;
    this.timeUnit = requireNonNull(timeUnit, "timeUnit");
    this.threadFactory = requireNonNull(threadFactory, "threadFactory");
  }

  @Override
  public int getInterval() {
    return interval;
  }

  @Override
  public void setInterval(int interval) {
    if (interval <= 0) {
      throw new IllegalArgumentException("Interval must be a positive integer");
    }
    synchronized (lock) {
      if (this.interval != interval) {
        this.interval = interval;
        if (isRunning()) {
          start();
        }
      }
    }
    intervalChangedEvent.onEvent(interval);
  }

  @Override
  public DefaultTaskScheduler start() {
    synchronized (lock) {
      stop();
      executorService = Executors.newSingleThreadScheduledExecutor(threadFactory);
      executorService.scheduleAtFixedRate(task, initialDelay, interval, timeUnit);

      return this;
    }
  }

  @Override
  public void stop() {
    synchronized (lock) {
      if (isRunning()) {
        executorService.shutdownNow();
        executorService = null;
      }
    }
  }

  @Override
  public boolean isRunning() {
    synchronized (lock) {
      return executorService != null && !executorService.isShutdown();
    }
  }

  @Override
  public void addIntervalListener(EventDataListener<Integer> listener) {
    intervalChangedEvent.addDataListener(listener);
  }
}
