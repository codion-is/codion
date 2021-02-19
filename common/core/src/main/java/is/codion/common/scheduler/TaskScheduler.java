/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.scheduler;

import is.codion.common.event.EventDataListener;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * A task scheduler based on a {@link ScheduledExecutorService}, scheduled at a fixed rate,
 * using a daemon thread by default.
 * A TaskScheduler can be stopped and restarted.
 * <pre>
 *   TaskScheduler scheduler = TaskScheduler.taskScheduler(new Runnable() {
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
public interface TaskScheduler {

  /**
   * @return the interval
   */
  int getInterval();

  /**
   * Sets the new task interval and, in case this scheduler was running, re-schedules the task.
   * If the scheduler was stopped it will remain so, the new interval coming into effect on next start.
   * @param interval the interval
   * @throws IllegalArgumentException in case {@code interval} isn't a positive integer
   */
  void setInterval(final int interval);

  /**
   * Starts this TaskScheduler, if it is running it is restarted, using the initial delay specified during construction.
   * @return this TaskScheduler instance
   */
  TaskScheduler start();

  /**
   * Stops this TaskScheduler, if it is not running calling this method has no effect.
   */
  void stop();

  /**
   * @return true if this TaskScheduler is running
   */
  boolean isRunning();

  /**
   * Adds a listener notified when the interval is set.
   * @param listener a listener notified each time the interval is set
   */
  void addIntervalListener(final EventDataListener<Integer> listener);

  /**
   * Instantiates a new TaskScheduler instance, with no initial delay and a daemon thread
   * @param task the task to run
   * @param interval the interval
   * @param timeUnit the time unit to use
   * @return a new TaskScheduler instance
   */
  static TaskScheduler taskScheduler(final Runnable task, final int interval, final TimeUnit timeUnit) {
    return taskScheduler(task, interval, 0, timeUnit);
  }

  /**
   * Instantiates a new TaskScheduler instance with a daemon thread.
   * @param task the task to run
   * @param interval the interval
   * @param initialDelay the initial start delay, used on restarts as well
   * @param timeUnit the time unit to use
   * @return a new TaskScheduler instance
   */
  static TaskScheduler taskScheduler(final Runnable task, final int interval, final int initialDelay, final TimeUnit timeUnit) {
    return taskScheduler(task, interval, initialDelay, timeUnit, runnable -> {
      final Thread thread = new Thread(runnable);
      thread.setDaemon(true);

      return thread;
    });
  }

  /**
   * Instantiates a new TaskScheduler instance.
   * @param task the task to run
   * @param interval the interval
   * @param initialDelay the initial start delay, used on restarts as well
   * @param timeUnit the time unit to use
   * @param threadFactory the thread factory to use
   * @return a new TaskScheduler instance
   */
  static TaskScheduler taskScheduler(final Runnable task, final int interval, final int initialDelay, final TimeUnit timeUnit,
                                     final ThreadFactory threadFactory) {
    return new DefaultTaskScheduler(task, interval, initialDelay, timeUnit, threadFactory);
  }
}
