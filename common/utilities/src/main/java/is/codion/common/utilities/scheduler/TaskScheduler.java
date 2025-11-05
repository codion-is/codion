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
package is.codion.common.utilities.scheduler;

import is.codion.common.reactive.value.Value;
import is.codion.common.reactive.value.Value.Validator;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

/**
 * A task scheduler based on a {@link ScheduledExecutorService}, scheduled at a fixed rate,
 * using a daemon thread by default.
 * A TaskScheduler can be stopped and restarted.
 * {@snippet :
 * TaskScheduler scheduler = builder()
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
public final class TaskScheduler {

	private static final Validator<Integer> INTERVAL_VALIDATOR = new IntervalValidator();

	private final Lock lock = new Lock() {};
	private final Runnable task;
	private final Value<Integer> interval;
	private final int initialDelay;
	private final TimeUnit timeUnit;
	private final ThreadFactory threadFactory;

	private @Nullable ScheduledExecutorService executorService;

	private TaskScheduler(DefaultBuilder builder) {
		this.task = builder.task;
		this.interval = Value.builder()
						.nonNull(builder.interval)
						.validator(INTERVAL_VALIDATOR)
						.listener(this::onIntervalChanged)
						.build();
		this.initialDelay = builder.initialDelay;
		this.timeUnit = builder.timeUnit;
		this.threadFactory = builder.threadFactory();
	}

	/**
	 * Controls the task interval and when set, in case this scheduler was running, re-schedules the task.
	 * If the scheduler was stopped it will remain so, the new interval coming into effect on next start.
	 * @return the {@link Value} controlling the interval
	 */
	public Value<Integer> interval() {
		return interval;
	}

	/**
	 * @return the time unit
	 */
	public TimeUnit timeUnit() {
		return timeUnit;
	}

	/**
	 * Starts this TaskScheduler, if it is running it is restarted, using the initial delay specified during construction.
	 * @return this TaskScheduler instance
	 */
	public TaskScheduler start() {
		synchronized (lock) {
			stop();
			executorService = Executors.newSingleThreadScheduledExecutor(threadFactory);
			executorService.scheduleAtFixedRate(task, initialDelay, interval.getOrThrow(), timeUnit);

			return this;
		}
	}

	/**
	 * Stops this TaskScheduler, if it is not running calling this method has no effect.
	 */
	public void stop() {
		synchronized (lock) {
			if (running() && executorService != null) {
				executorService.shutdownNow();
				executorService = null;
			}
		}
	}

	/**
	 * @return true if this TaskScheduler is running
	 */
	public boolean running() {
		synchronized (lock) {
			return executorService != null && !executorService.isShutdown();
		}
	}

	/**
	 * @return a new {@link Builder.TaskStep} instance.
	 */
	public static Builder.TaskStep builder() {
		return DefaultBuilder.TASK;
	}

	/**
	 * A builder for {@link TaskScheduler}
	 */
	public sealed interface Builder {

		/**
		 * Provides a {@link Builder.IntervalStep}
		 */
		sealed interface TaskStep {

			/**
			 * @param task the task to run
			 * @return a new {@link Builder.IntervalStep} instance.
			 */
			Builder.IntervalStep task(Runnable task);
		}

		/**
		 * Provides a {@link Builder}
		 */
		sealed interface IntervalStep {

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
		 * Note that this overrides {@link #name(String)}
		 * @param threadFactory the thread factory to use
		 * @return this builder instance
		 */
		Builder threadFactory(ThreadFactory threadFactory);

		/**
		 * Note that this is overridden by {@link #threadFactory(ThreadFactory)}
		 * @param name the name to use for the scheduler thread
		 * @return this builder instance
		 */
		Builder name(String name);

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

	private void onIntervalChanged() {
		synchronized (lock) {
			if (running()) {
				start();
			}
		}
	}

	private static final class DefaultTaskStep implements Builder.TaskStep {

		@Override
		public Builder.IntervalStep task(Runnable task) {
			return new DefaultIntervalStep(requireNonNull(task));
		}
	}

	private static final class DefaultIntervalStep implements Builder.IntervalStep {

		private final Runnable task;

		private DefaultIntervalStep(Runnable task) {
			this.task = task;
		}

		@Override
		public Builder interval(int interval, TimeUnit timeUnit) {
			return new DefaultBuilder(task, interval, requireNonNull(timeUnit));
		}
	}

	private static final class DefaultBuilder implements Builder {

		private static final Builder.TaskStep TASK = new DefaultTaskStep();

		private final Runnable task;
		private final int interval;
		private final TimeUnit timeUnit;

		private int initialDelay;
		private @Nullable ThreadFactory threadFactory;
		private @Nullable String name;

		private DefaultBuilder(Runnable task, int interval, TimeUnit timeUnit) {
			if (interval <= 0) {
				throw new IllegalArgumentException("Interval must be a positive integer");
			}
			this.task = task;
			this.interval = interval;
			this.timeUnit = timeUnit;
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
		public Builder name(String name) {
			this.name = requireNonNull(name);
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
			return new TaskScheduler(this);
		}

		private ThreadFactory threadFactory() {
			return threadFactory == null ? new DaemonThreadFactory(name) : threadFactory;
		}
	}

	private static final class DaemonThreadFactory implements ThreadFactory {

		private final @Nullable String name;

		private DaemonThreadFactory(@Nullable String name) {
			this.name = name;
		}

		@Override
		public Thread newThread(Runnable runnable) {
			Thread thread = new Thread(runnable);
			thread.setDaemon(true);
			if (name != null) {
				thread.setName(name);
			}

			return thread;
		}
	}

	private static final class IntervalValidator implements Validator<Integer> {

		@Override
		public void validate(@Nullable Integer interval) {
			if (interval == null || interval <= 0) {
				throw new IllegalArgumentException("Interval must be a positive integer");
			}
		}
	}

	private interface Lock {}
}
