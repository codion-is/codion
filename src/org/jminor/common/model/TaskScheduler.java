/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A task scheduler based on a {@link ScheduledExecutorService}, scheduled with a fixed rate, using daemon threads.
 * A TaskScheduler can be stopped and restarted.
 */
public final class TaskScheduler {

  public static final String INTERVAL_PROPERTY = "interval";

  private final Runnable task;
  private final TimeUnit timeUnit;
  private final Event evtIntervalChanged = Events.event();
  private final int initialDelay;

  private ScheduledExecutorService updateService;
  private int interval;

  /**
   * Instantiates and starts a new TaskScheduler instance, with no initial delay
   * @param task the task to run
   * @param interval the interval
   * @param timeUnit the time unit to use
   */
  public TaskScheduler(final Runnable task, final int interval, final TimeUnit timeUnit) {
    this(task, interval, 0, timeUnit);
  }

  /**
   * Instantiates and starts new TaskScheduler instance.
   * @param task the task to run
   * @param interval the interval
   * @param initialDelay the delay before the task is run for the first time
   * @param timeUnit the time unit to use
   */
  public TaskScheduler(final Runnable task, final int interval, final int initialDelay, final TimeUnit timeUnit) {
    this.task = task;
    this.interval = interval;
    this.initialDelay = initialDelay;
    this.timeUnit = timeUnit;
    start();
  }

  /**
   * @return the interval
   */
  public synchronized int getInterval() {
    return interval;
  }

  /**
   * Sets the new task interval and re-schedules the task
   * @param interval the interval
   */
  public synchronized void setInterval(final int interval) {
    if (interval <= 0) {
      throw new IllegalArgumentException("Interval must be a positive integer");
    }
    if (this.interval != interval) {
      this.interval = interval;
      evtIntervalChanged.fire();
      start();
    }
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
    return updateService != null && !updateService.isShutdown();
  }

  /**
   * Stops this TaskScheduler, if this TaskScheduler is not running calling this method has no effect.
   */
  public synchronized void stop() {
    if (isRunning()) {
      updateService.shutdownNow();
      updateService = null;
    }
  }

  /**
   * Starts this TaskScheduler, if it is running it is restarted, using the initial delay specified during construction.
   */
  public synchronized void start() {
    stop();
    updateService = Executors.newSingleThreadScheduledExecutor(new Util.DaemonThreadFactory());
    updateService.scheduleAtFixedRate(task, initialDelay, interval, timeUnit);
  }
}
