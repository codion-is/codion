/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2012 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.scheduler;

import is.codion.common.value.Value;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static is.codion.common.scheduler.TaskScheduler.Builder.TaskStep;

/**
 * A task scheduler based on a {@link ScheduledExecutorService}, scheduled at a fixed rate,
 * using a daemon thread by default.
 * A TaskScheduler can be stopped and restarted.
 * {@snippet :
 * TaskScheduler scheduler = TaskScheduler.builder()
 *     .task(() -> System.out.println("Running wild..."))
 *     .interval(2, TimeUnit.SECONDS)
 *     .build();
 *
 * scheduler.start();
 * // ...
 * scheduler.interval().set(1);//task restarted using the new interval
 * // ...
 * scheduler.stop();
 *}
 * @see TaskScheduler#builder()
 */
public interface TaskScheduler {

	/**
	 * Controls the task interval and when set, in case this scheduler was running, re-schedules the task.
	 * If the scheduler was stopped it will remain so, the new interval coming into effect on next start.
	 * @return the {@link Value} controlling the interval
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
	boolean running();

	/**
	 * @return a new {@link TaskStep} instance.
	 */
	static TaskStep builder() {
		return DefaultTaskScheduler.DefaultBuilder.TASK;
	}

	/**
	 * A builder for {@link TaskScheduler}
	 */
	interface Builder {

		/**
		 * Provides a {@link Builder.IntervalStep}
		 */
		interface TaskStep {

			/**
			 * @param task the task to run
			 * @return a new {@link Builder.IntervalStep} instance.
			 */
			IntervalStep task(Runnable task);
		}

		/**
		 * Provides a {@link Builder}
		 */
		interface IntervalStep {

			/**
			 * @param interval the interval
			 * @param timeUnit the time unit
			 * @return a builder instance
			 */
			Builder interval(int interval, TimeUnit timeUnit);
		}

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
