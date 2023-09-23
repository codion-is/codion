/*
 * Copyright (c) 2012 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.scheduler;

import is.codion.common.value.Value;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

final class DefaultTaskScheduler implements TaskScheduler {

  private final Object lock = new Object();
  private final Runnable task;
  private final Value<Integer> interval;
  private final int initialDelay;
  private final TimeUnit timeUnit;
  private final ThreadFactory threadFactory;

  private ScheduledExecutorService executorService;

  private DefaultTaskScheduler(DefaultBuilder builder) {
    this.task = builder.task;
    this.interval = Value.value(builder.interval, builder.interval);
    this.interval.addValidator(value -> {
      if (value <= 0) {
        throw new IllegalArgumentException("Interval must be a positive integer");
      }
    });
    this.initialDelay = builder.initialDelay;
    this.timeUnit = builder.timeUnit;
    this.threadFactory = builder.threadFactory;
    this.interval.addListener(this::onIntervalChanged);
  }

  @Override
  public Value<Integer> interval() {
    return interval;
  }

  @Override
  public TimeUnit timeUnit() {
    return timeUnit;
  }

  @Override
  public DefaultTaskScheduler start() {
    synchronized (lock) {
      stop();
      executorService = Executors.newSingleThreadScheduledExecutor(threadFactory);
      executorService.scheduleAtFixedRate(task, initialDelay, interval.get(), timeUnit);

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

  private void onIntervalChanged() {
    synchronized (lock) {
      if (isRunning()) {
        start();
      }
    }
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
    public Builder interval(int interval, TimeUnit timeUnit) {
      if (interval <= 0) {
        throw new IllegalArgumentException("Interval must be a positive integer");
      }
      this.interval = interval;
      this.timeUnit = requireNonNull(timeUnit);
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

  private static final class DaemonThreadFactory implements ThreadFactory {

    @Override
    public Thread newThread(Runnable runnable) {
      Thread thread = new Thread(runnable);
      thread.setDaemon(true);

      return thread;
    }
  }
}
