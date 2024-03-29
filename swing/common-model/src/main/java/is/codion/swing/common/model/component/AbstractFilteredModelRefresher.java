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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.component;

import is.codion.common.model.FilteredModel;
import is.codion.swing.common.model.worker.ProgressWorker;

import javax.swing.SwingUtilities;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A default swing based {@link FilteredModel.Refresher}.
 * @param <T> the model row type
 */
public abstract class AbstractFilteredModelRefresher<T> extends FilteredModel.AbstractRefresher<T> {

	private ProgressWorker<Collection<T>, ?> refreshWorker;

	/**
	 * @param itemSupplier the item supplier
	 */
	protected AbstractFilteredModelRefresher(Supplier<Collection<T>> itemSupplier) {
		super(itemSupplier);
	}

	@Override
	protected final boolean supportsAsyncRefresh() {
		return SwingUtilities.isEventDispatchThread();
	}

	protected final void refreshAsync(Consumer<Collection<T>> afterRefresh) {
		cancelCurrentRefresh();
		refreshWorker = ProgressWorker.builder(itemSupplier().get()::get)
						.onStarted(this::onRefreshStarted)
						.onResult(items -> onRefreshResult(items, afterRefresh))
						.onException(this::onRefreshFailedAsync)
						.execute();
	}

	protected final void refreshSync(Consumer<Collection<T>> afterRefresh) {
		onRefreshStarted();
		try {
			onRefreshResult(itemSupplier().get().get(), afterRefresh);
		}
		catch (Exception e) {
			onRefreshFailedSync(e);
		}
	}

	private void onRefreshStarted() {
		setRefreshing(true);
	}

	private void onRefreshFailedAsync(Throwable throwable) {
		refreshWorker = null;
		setRefreshing(false);
		refreshFailedEvent(throwable);
	}

	private void onRefreshFailedSync(Throwable throwable) {
		setRefreshing(false);
		if (throwable instanceof RuntimeException) {
			throw (RuntimeException) throwable;
		}

		throw new RuntimeException(throwable);
	}

	private void onRefreshResult(Collection<T> items, Consumer<Collection<T>> afterRefresh) {
		refreshWorker = null;
		setRefreshing(false);
		processResult(items);
		if (afterRefresh != null) {
			afterRefresh.accept(items);
		}
		refreshEvent();
	}

	private void cancelCurrentRefresh() {
		ProgressWorker<?, ?> worker = refreshWorker;
		if (worker != null) {
			worker.cancel(true);
		}
	}
}
