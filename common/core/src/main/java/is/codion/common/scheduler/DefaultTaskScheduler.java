/*
 * Copyright (c) 2012 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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

  private DefaultTaskScheduler(DefaultBuilder builder) {
    this.task = builder.task;
    this.interval = builder.interval;
    this.initialDelay = builder.initialDelay;
    this.timeUnit = builder.timeUnit;
    this.threadFactory = builder.threadFactory;
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

  static final class DefaultBuilder implements Builder {

    private final Runnable task;

    private int interval;
    private int initialDelay;
    private TimeUnit timeUnit;
    private ThreadFactory threadFactory = new DaemonThreadFactory();

    DefaultBuilder(Runnable task) {
      this.task = requireNonNull(task);
    }

    @Override
    public Builder interval(int interval) {
      if (interval <= 0) {
        throw new IllegalArgumentException("Interval must be a positive integer");
      }
      this.interval = interval;
      return this;
    }

    @Override
    public Builder initialDelay(int initialDelay) {
      if (initialDelay < 0) {
        throw new IllegalArgumentException("Initial delay can not be negative");
      }
      this.initialDelay = initialDelay;
      return this;
    }

    @Override
    public Builder timeUnit(TimeUnit timeUnit) {
      this.timeUnit = requireNonNull(timeUnit);
      return this;
    }

    @Override
    public Builder threadFactory(ThreadFactory threadFactory) {
      this.threadFactory = requireNonNull(threadFactory);
      return this;
    }

    @Override
    public TaskScheduler start() {
      TaskScheduler taskScheduler = build();
      taskScheduler.start();

      return taskScheduler;
    }

    @Override
    public TaskScheduler build() {
      if (interval == 0) {
        throw new IllegalStateException("An interval > 0 is required for building a TaskScheduler");
      }
      if (timeUnit == null) {
        throw new IllegalStateException("A time unit is required for building a TaskScheduler");
      }

      return new DefaultTaskScheduler(this);
    }
  }
}
