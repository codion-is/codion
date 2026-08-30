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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.model.filter;

import is.codion.common.model.filter.FilterModel.Refresher;
import is.codion.common.model.worker.ProgressWorker;
import is.codion.common.model.worker.ProgressWorker.ResultTaskHandler;
import is.codion.common.reactive.event.Event;
import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.Value;
import is.codion.common.utilities.dispatch.Dispatcher;
import is.codion.common.utilities.exceptions.Exceptions;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * The default {@link Refresher} implementation, performing an async refresh via {@link ProgressWorker} when
 * {@link #async()} is enabled and the refresh is triggered where a dispatch context is bound, the UI thread on
 * UI platforms (see {@link Dispatcher#bound()}), otherwise a synchronous refresh on the calling thread.
 * @param <T> the model item type
 */
final class DefaultRefresher<T> implements Refresher<T> {

	//One thread for the whole JVM, it only hands the start back to a dispatch context, never blocking.
	//Its thread starts on the first delayed refresh, not here - an application leaving delay() at zero,
	//which is the default, never starts it, so do not prestart the core thread.
	private static final ScheduledExecutorService SCHEDULER =
					Executors.newSingleThreadScheduledExecutor(runnable -> {
						Thread thread = new Thread(runnable, "RefreshScheduler");
						thread.setDaemon(true);

						return thread;
					});

	private final Event<Collection<T>> event = Event.event();
	private final State active = State.state();
	private final @Nullable Supplier<Collection<T>> items;
	private final State async;
	private final Value<Integer> delay;
	private final @Nullable Consumer<Collection<T>> onResult;
	private final Consumer<Exception> onException;

	private @Nullable ProgressWorker<Collection<T>, ?> worker;
	private @Nullable RefreshTask currentTask;
	private @Nullable ScheduledRefresh scheduledRefresh;

	private DefaultRefresher(DefaultBuilder<T> builder) {
		this.items = builder.items;
		this.async = State.state(FilterModel.ASYNC.getOrThrow());
		this.delay = Value.builder()
						.nonNull(0)
						.value(FilterModel.REFRESH_DELAY.getOrThrow())
						.validator(new DelayValidator())
						.build();
		this.onResult = builder.onResult;
		this.onException = builder.onException == null ? new RethrowExceptionHandler() : builder.onException;
	}

	@Override
	public State async() {
		return async;
	}

	@Override
	public Value<Integer> delay() {
		return delay;
	}

	@Override
	public ObservableState active() {
		return active.observable();
	}

	@Override
	public Observer<Collection<T>> result() {
		return event.observer();
	}

	@Override
	public void refresh(@Nullable Consumer<Collection<T>> onResult) {
		if (async.is() && Dispatcher.instance().bound()) {
			refreshAsync(onResult);
		}
		else {
			refreshSync(onResult);
		}
	}

	private void refreshAsync(@Nullable Consumer<Collection<T>> onResult) {
		items().ifPresent(items -> {
			cancelCurrentRefresh();
			int delayMillis = delay.getOrThrow();
			if (delayMillis == 0) {
				startRefresh(items, onResult);
			}
			else {
				//the executor is resolved here, on the dispatch context this was called from, so that an
				//implementation binding one per request or session resolves the right one
				scheduledRefresh = new ScheduledRefresh(items, onResult, Dispatcher.instance().executor());
				//a refresh is under way from the moment it is asked for, waiting is part of it
				active.set(true);
				scheduledRefresh.schedule(delayMillis);
			}
		});
	}

	private void startRefresh(Supplier<Collection<T>> items, @Nullable Consumer<Collection<T>> onResult) {
		currentTask = new RefreshTask(items, onResult);
		worker = ProgressWorker.builder()
						.task(currentTask)
						.execute();
	}

	private void refreshSync(@Nullable Consumer<Collection<T>> onResult) {
		items().ifPresent(items -> {
			//a synchronous refresh supersedes like any other: a fetch in flight or one waiting out
			//delay() is cancelled rather than left to fetch and deliver after this one has
			cancelCurrentRefresh();
			active.set(true);
			Collection<T> result;
			try {
				//only the refresh itself is guarded; a result consumer throwing must propagate,
				//as it does on the async path, rather than being reported as a refresh failure
				result = items.get();
			}
			catch (Exception e) {
				onRefreshException(e);
				return;
			}
			onRefreshResult(result, onResult);
		});
	}

	private Optional<Supplier<Collection<T>>> items() {
		return Optional.ofNullable(items);
	}

	private void onRefreshException(Exception exception) {
		active.set(false);
		onException.accept(exception);
	}

	private void onRefreshResult(Collection<T> result, @Nullable Consumer<Collection<T>> onResult) {
		active.set(false);
		if (this.onResult != null) {
			this.onResult.accept(result);
		}
		if (onResult != null) {
			onResult.accept(result);
		}
		event.accept(result);
	}

	private void cancelCurrentRefresh() {
		ScheduledRefresh scheduled = scheduledRefresh;
		if (scheduled != null) {
			scheduledRefresh = null;
			scheduled.cancel();
		}
		ProgressWorker<?, ?> progressWorker = worker;
		if (progressWorker != null) {
			worker = null;
			currentTask = null;
			progressWorker.cancel(true);
		}
	}

	/**
	 * A refresh waiting out {@link #delay()}, replaced by each one arriving while it waits, so that a burst
	 * results in a single fetch once they stop.
	 */
	private final class ScheduledRefresh implements Runnable {

		private final Supplier<Collection<T>> items;
		private final @Nullable Consumer<Collection<T>> onResult;
		private final Executor executor;

		private @Nullable ScheduledFuture<?> future;

		private ScheduledRefresh(Supplier<Collection<T>> items, @Nullable Consumer<Collection<T>> onResult,
														 Executor executor) {
			this.items = items;
			this.onResult = onResult;
			this.executor = executor;
		}

		@Override
		public void run() {
			//on the scheduler thread, which hands the start back to the dispatch context, where
			//ProgressWorker.execute() requires to be called
			executor.execute(this::start);
		}

		private void schedule(int delayMillis) {
			future = SCHEDULER.schedule(this, delayMillis, MILLISECONDS);
		}

		private void cancel() {
			ScheduledFuture<?> scheduledFuture = future;
			if (scheduledFuture != null) {
				scheduledFuture.cancel(false);
			}
		}

		private void start() {
			//cancel() may have arrived too late to stop this one, so the identity check has the final
			//say, as it does for a superseded RefreshTask
			if (scheduledRefresh == this) {
				scheduledRefresh = null;
				startRefresh(items, onResult);
			}
		}
	}

	private final class RefreshTask implements ResultTaskHandler<Collection<T>> {

		private final Supplier<Collection<T>> items;
		private final @Nullable Consumer<Collection<T>> onResult;

		private RefreshTask(Supplier<Collection<T>> items, @Nullable Consumer<Collection<T>> onResult) {
			this.items = items;
			this.onResult = onResult;
		}

		@Override
		public Collection<T> execute() throws Exception {
			return items.get();
		}

		@Override
		public void onStarted() {
			if (currentTask == this) {
				active.set(true);
			}
		}

		@Override
		public void onResult(Collection<T> result) {
			if (currentTask == this) {
				currentTask = null;
				worker = null;
				onRefreshResult(result, onResult);
			}
		}

		@Override
		public void onException(Exception exception) {
			if (currentTask == this) {
				currentTask = null;
				worker = null;
				onRefreshException(exception);
			}
		}
	}

	private static final class DelayValidator implements Value.Validator<Integer> {

		@Override
		public void validate(@Nullable Integer delay) {
			if (delay != null && delay < 0) {
				throw new IllegalArgumentException("Refresh delay can not be negative: " + delay);
			}
		}
	}

	private static final class RethrowExceptionHandler implements Consumer<Exception> {

		@Override
		public void accept(Exception exception) {
			throw Exceptions.runtime(exception);
		}
	}

	static final class DefaultBuilder<T> implements Builder<T> {

		private @Nullable Supplier<Collection<T>> items;
		private @Nullable Consumer<Collection<T>> onResult;
		private @Nullable Consumer<Exception> onException;

		@Override
		public Builder<T> items(@Nullable Supplier<Collection<T>> items) {
			this.items = items;
			return this;
		}

		@Override
		public Builder<T> onResult(@Nullable Consumer<Collection<T>> onResult) {
			this.onResult = onResult;
			return this;
		}

		@Override
		public Builder<T> onException(@Nullable Consumer<Exception> onException) {
			this.onException = onException;
			return this;
		}

		@Override
		public Refresher<T> build() {
			return new DefaultRefresher<>(this);
		}
	}
}
