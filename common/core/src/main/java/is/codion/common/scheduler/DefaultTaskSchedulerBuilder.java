/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.scheduler;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

final class DefaultTaskSchedulerBuilder implements TaskScheduler.Builder {

  private final Runnable task;

  private int interval;
  private int initialDelay;
  private TimeUnit timeUnit;
  private ThreadFactory threadFactory = runnable -> {
    Thread thread = new Thread(runnable);
    thread.setDaemon(true);

    return thread;
  };

  DefaultTaskSchedulerBuilder(final Runnable task) {
    this.task = requireNonNull(task);
  }

  @Override
  public TaskScheduler.Builder interval(final int interval) {
    if (interval <= 0) {
      throw new IllegalArgumentException("Interval must be a positive integer");
    }
    this.interval = interval;
    return this;
  }

  @Override
  public TaskScheduler.Builder initialDelay(final int initialDelay) {
    if (initialDelay < 0) {
      throw new IllegalArgumentException("Initial delay can not be negative");
    }
    this.initialDelay = initialDelay;
    return this;
  }

  @Override
  public TaskScheduler.Builder timeUnit(final TimeUnit timeUnit) {
    this.timeUnit = requireNonNull(timeUnit);
    return this;
  }

  @Override
  public TaskScheduler.Builder threadFactory(final ThreadFactory threadFactory) {
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

    return new DefaultTaskScheduler(task, interval, initialDelay, timeUnit, threadFactory);
  }
}
