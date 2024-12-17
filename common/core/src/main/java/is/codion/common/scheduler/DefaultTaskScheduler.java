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
 * Copyright (c) 2012 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.scheduler;

import is.codion.common.value.Value;
import is.codion.common.value.Value.Validator;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

final class DefaultTaskScheduler implements TaskScheduler {

	private static final Validator<Integer> INTERVAL_VALIDATOR = new IntervalValidator();

	private final Lock lock = new Lock();
	private final Runnable task;
	private final Value<Integer> interval;
	private final int initialDelay;
	private final TimeUnit timeUnit;
	private final ThreadFactory threadFactory;

	private @Nullable ScheduledExecutorService executorService;

	private DefaultTaskScheduler(DefaultBuilder builder) {
		this.task = builder.task;
		this.interval = Value.builder()
						.nonNull(builder.interval)
						.validator(INTERVAL_VALIDATOR)
						.listener(this::onIntervalChanged)
						.build();
		this.initialDelay = builder.initialDelay;
		this.timeUnit = builder.timeUnit();
		this.threadFactory = builder.threadFactory;
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
			executorService.scheduleAtFixedRate(task, initialDelay, interval.getOrThrow(), timeUnit);

			return this;
		}
	}

	@Override
	public void stop() {
		synchronized (lock) {
			if (running() && executorService != null) {
				executorService.shutdownNow();
				executorService = null;
			}
		}
	}

	@Override
	public boolean running() {
		synchronized (lock) {
			return executorService != null && !executorService.isShutdown();
		}
	}

	private void onIntervalChanged() {
		synchronized (lock) {
			if (running()) {
				start();
			}
		}
	}

	static final class DefaultBuilder implements Builder {

		private final Runnable task;

		private int interval;
		private int initialDelay;
		private @Nullable TimeUnit timeUnit;
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

			return new DefaultTaskScheduler(this);
		}

		private TimeUnit timeUnit() {
			if (timeUnit == null) {
				throw new IllegalStateException("A time unit is required for building a TaskScheduler");
			}

			return timeUnit;
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

	private static final class IntervalValidator implements Validator<Integer> {

		@Override
		public void validate(Integer interval) {
			if (interval <= 0) {
				throw new IllegalArgumentException("Interval must be a positive integer");
			}
		}
	}
	
	private static final class Lock {}
}
