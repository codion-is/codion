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
import is.codion.common.model.worker.Dispatcher;
import is.codion.common.model.worker.ProgressWorker;
import is.codion.common.model.worker.ProgressWorker.ResultTaskHandler;
import is.codion.common.reactive.event.Event;
import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.common.utilities.exceptions.Exceptions;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The default {@link Refresher} implementation, performing an async refresh via {@link ProgressWorker} when
 * {@link #async()} is enabled and the refresh is triggered on the UI thread (as determined by {@link Dispatcher}),
 * otherwise a synchronous refresh on the calling thread.
 * @param <T> the model item type
 */
final class DefaultRefresher<T> implements Refresher<T> {

	private final Event<Collection<T>> event = Event.event();
	private final State active = State.state();
	private final @Nullable Supplier<Collection<T>> items;
	private final State async;
	private final @Nullable Consumer<Collection<T>> onResult;
	private final Consumer<Exception> onException;

	private @Nullable ProgressWorker<Collection<T>, ?> worker;
	private @Nullable RefreshTask currentTask;

	private DefaultRefresher(DefaultBuilder<T> builder) {
		this.items = builder.items;
		this.async = State.state(builder.async);
		this.onResult = builder.onResult;
		this.onException = builder.onException == null ? new RethrowExceptionHandler() : builder.onException;
	}

	@Override
	public State async() {
		return async;
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
		if (async.is() && Dispatcher.instance().isUserInterfaceThread()) {
			refreshAsync(onResult);
		}
		else {
			refreshSync(onResult);
		}
	}

	private void refreshAsync(@Nullable Consumer<Collection<T>> onResult) {
		items().ifPresent(items -> {
			cancelCurrentRefresh();
			currentTask = new RefreshTask(items, onResult);
			worker = ProgressWorker.builder()
							.task(currentTask)
							.execute();
		});
	}

	private void refreshSync(@Nullable Consumer<Collection<T>> onResult) {
		items().ifPresent(items -> {
			active.set(true);
			try {
				onRefreshResult(items.get(), onResult);
			}
			catch (Exception e) {
				onRefreshException(e);
			}
		});
	}

	private Optional<Supplier<Collection<T>>> items() {
		return Optional.ofNullable(items);
	}

	private void onRefreshException(Exception exception) {
		worker = null;
		active.set(false);
		onException.accept(exception);
	}

	private void onRefreshResult(Collection<T> result, @Nullable Consumer<Collection<T>> onResult) {
		worker = null;
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
		ProgressWorker<?, ?> progressWorker = worker;
		if (progressWorker != null) {
			worker = null;
			currentTask = null;
			progressWorker.cancel(true);
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
				onRefreshResult(result, onResult);
			}
		}

		@Override
		public void onException(Exception exception) {
			if (currentTask == this) {
				currentTask = null;
				onRefreshException(exception);
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
		private boolean async = FilterModel.ASYNC.getOrThrow();

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
		public Builder<T> async(boolean async) {
			this.async = async;
			return this;
		}

		@Override
		public Refresher<T> build() {
			return new DefaultRefresher<>(this);
		}
	}
}
