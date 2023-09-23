/*
 * Copyright (c) 2012 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.scheduler;

import is.codion.common.value.Value;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * A task scheduler based on a {@link ScheduledExecutorService}, scheduled at a fixed rate,
 * using a daemon thread by default.
 * A TaskScheduler can be stopped and restarted.
 * <pre>
 * TaskScheduler scheduler = TaskScheduler.builder(() -&gt; System.out.println("Running wild..."))
 *     .interval(2)
 *     .timeUnit(TimeUnit.SECONDS)
 *     .build();
 *
 * scheduler.start();
 * ...
 * scheduler.interval().set(1);//task restarted using the new interval
 * ...
 * scheduler.stop();
 * </pre>
 * @see TaskScheduler#builder(Runnable)
 */
public interface TaskScheduler {

  /**
   * Controls the task interval and when set, in case this scheduler was running, re-schedules the task.
   * If the scheduler was stopped it will remain so, the new interval coming into effect on next start.
   * @return the value controlling the interval
   */
  Value<Integer> interval();

  /**
   * @return the time unit
   */
  TimeUnit timeUnit();

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
   * @param task the task to run
   * @return a new {@link TaskScheduler.Builder} instance.
   */
  static TaskScheduler.Builder builder(Runnable task) {
    return new DefaultTaskScheduler.DefaultBuilder(task);
  }

  /**
   * A builder for {@link TaskScheduler}
   */
  interface Builder {

    /**
     * @param interval the interval
     * @param timeUnit the time unit
     * @return this builder instance
     */
    Builder interval(int interval, TimeUnit timeUnit);

    /**
     * @param initialDelay the initial start delay, used on restarts as well
     * @return this builder instance
     */
    Builder initialDelay(int initialDelay);

    /**
     * @param threadFactory the thread factory to use
     * @return this builder instance
     */
    Builder threadFactory(ThreadFactory threadFactory);

    /**
     * Builds and starts a new {@link TaskScheduler}.
     * @return a new {@link TaskScheduler}.
     */
    TaskScheduler start();

    /**
     * @return a new {@link TaskScheduler}.
     */
    TaskScheduler build();
  }
}
