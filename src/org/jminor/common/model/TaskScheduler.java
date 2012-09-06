/*
 * Copyright (c) 2004 - 2012, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

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
 *   scheduler.stop();
 * </pre>
 */
public final class TaskScheduler {

  public static final String INTERVAL_PROPERTY = "interval";

  private final Runnable task;
  private final int initialDelay;
  private final TimeUnit timeUnit;
  private final ThreadFactory threadFactory;
  private final Event evtIntervalChanged = Events.event();

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
   * @param initialDelay the delay before the task is run for the first time
   * @param timeUnit the time unit to use
   */
  public TaskScheduler(final Runnable task, final int interval, final int initialDelay, final TimeUnit timeUnit) {
    this(task, interval, initialDelay, timeUnit, new Util.DaemonThreadFactory());
  }

  /**
   * Instantiates a new TaskScheduler instance.
   * @param task the task to run
   * @param interval the interval
   * @param initialDelay the delay before the task is run for the first time
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
    this.task = Util.rejectNullValue(task, "task");
    this.interval = interval;
    this.initialDelay = initialDelay;
    this.timeUnit = Util.rejectNullValue(timeUnit, "timeUnit");
    this.threadFactory = Util.rejectNullValue(threadFactory, "threadFactory");
  }

  /**
   * @return the interval
   */
  public int getInterval() {
    return interval;
  }

  /**
   * Sets the new task interval and re-schedules the task
   * @param interval the interval
   * @throws IllegalArgumentException in case <code>interval</code> isn't a positive integer
   */
  public void setInterval(final int interval) {
    if (interval <= 0) {
      throw new IllegalArgumentException("Interval must be a positive integer");
    }
    synchronized (this) {
      if (this.interval != interval) {
        this.interval = interval;
        start();
      }
    }
    evtIntervalChanged.fire();
  }

  /**
   * @return an EventObserver notified each time the interval changes
   */
  public EventObserver getIntervalObserver() {
    return evtIntervalChanged.getObserver();
  }

  /**
   * @return true if this TaskScheduler is running
   */
  public synchronized boolean isRunning() {
    return executorService != null && !executorService.isShutdown();
  }

  /**
   * Stops this TaskScheduler, if it is not running calling this method has no effect.
   */
  public synchronized void stop() {
    if (isRunning()) {
      executorService.shutdownNow();
      executorService = null;
    }
  }

  /**
   * Starts this TaskScheduler, if it is running it is restarted, using the initial delay specified during construction.
   * @return this TaskScheduler instance
   */
  public synchronized TaskScheduler start() {
    stop();
    executorService = Executors.newSingleThreadScheduledExecutor(threadFactory);
    executorService.scheduleAtFixedRate(task, initialDelay, interval, timeUnit);

    return this;
  }
}
