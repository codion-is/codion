/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * A task scheduler based on a {@link ScheduledExecutorService}, scheduled at a fixed rate,
 * using a daemon thread by default.
 * A TaskScheduler can be stopped and restarted.
 * <pre>
 *   TaskScheduler scheduler = new TaskScheduler(new Runnable() {
 *     public void run() {
 *       System.out.println("Running wild...");
 *     }
 *   }, 2, TimeUnit.SECONDS);
 *
 *   scheduler.start();
 *   ...
 *   scheduler.setInterval(1);//task restarted using the new interval
 *   ...
 *   scheduler.stop();
 * </pre>
 */
public final class TaskScheduler {

  public static final String INTERVAL_PROPERTY = "interval";

  private final Object lock = new Object();
  private final Runnable task;
  private final int initialDelay;
  private final TimeUnit timeUnit;
  private final ThreadFactory threadFactory;
  private final Event<Integer> intervalChangedEvent = Events.event();

  private ScheduledExecutorService executorService;
  private int interval;

  /**
   * Instantiates a new TaskScheduler instance, with no initial delay and a daemon thread
   * @param task the task to run
   * @param interval the interval
   * @param timeUnit the time unit to use
   */
  public TaskScheduler(final Runnable task, final int interval, final TimeUnit timeUnit) {
    this(task, interval, 0, timeUnit);
  }

  /**
   * Instantiates a new TaskScheduler instance with a daemon thread.
   * @param task the task to run
   * @param interval the interval
   * @param initialDelay the initial start delay, used on restarts as well
   * @param timeUnit the time unit to use
   */
  public TaskScheduler(final Runnable task, final int interval, final int initialDelay, final TimeUnit timeUnit) {
    this(task, interval, initialDelay, timeUnit, new DaemonThreadFactory());
  }

  /**
   * Instantiates a new TaskScheduler instance.
   * @param task the task to run
   * @param interval the interval
   * @param initialDelay the initial start delay, used on restarts as well
   * @param timeUnit the time unit to use
   * @param threadFactory the thread factory to use
   */
  public TaskScheduler(final Runnable task, final int interval, final int initialDelay, final TimeUnit timeUnit,
                       final ThreadFactory threadFactory) {
    if (interval <= 0) {
      throw new IllegalArgumentException("Interval must be a positive integer");
    }
    if (initialDelay < 0) {
      throw new IllegalArgumentException("Initial delay can not be negative");
    }
    this.task = Objects.requireNonNull(task, "task");
    this.interval = interval;
    this.initialDelay = initialDelay;
    this.timeUnit = Objects.requireNonNull(timeUnit, "timeUnit");
    this.threadFactory = Objects.requireNonNull(threadFactory, "threadFactory");
  }

  /**
   * @return the interval
   */
  public int getInterval() {
    return interval;
  }

  /**
   * Sets the new task interval and re-schedules the task, note that if the scheduler was stopped it will be restarted.
   * @param interval the interval
   * @throws IllegalArgumentException in case {@code interval} isn't a positive integer
   */
  public void setInterval(final int interval) {
    if (interval <= 0) {
      throw new IllegalArgumentException("Interval must be a positive integer");
    }
    synchronized (lock) {
      if (this.interval != interval) {
        this.interval = interval;
        start();
      }
    }
    intervalChangedEvent.fire(interval);
  }

  /**
   * @return an EventObserver notified each time the interval changes
   */
  public EventObserver<Integer> getIntervalObserver() {
    return intervalChangedEvent.getObserver();
  }

  /**
   * Starts this TaskScheduler, if it is running it is restarted, using the initial delay specified during construction.
   * @return this TaskScheduler instance
   */
  public TaskScheduler start() {
    synchronized (lock) {
      stop();
      executorService = Executors.newSingleThreadScheduledExecutor(threadFactory);
      executorService.scheduleAtFixedRate(task, initialDelay, interval, timeUnit);

      return this;
    }
  }

  /**
   * Stops this TaskScheduler, if it is not running calling this method has no effect.
   */
  public void stop() {
    synchronized (lock) {
      if (isRunning()) {
        executorService.shutdownNow();
        executorService = null;
      }
    }
  }

  /**
   * @return true if this TaskScheduler is running
   */
  public boolean isRunning() {
    synchronized (lock) {
      return executorService != null && !executorService.isShutdown();
    }
  }
}
