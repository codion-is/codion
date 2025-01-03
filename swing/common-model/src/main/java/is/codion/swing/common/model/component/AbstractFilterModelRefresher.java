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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.component;

import is.codion.common.model.FilterModel;
import is.codion.swing.common.model.worker.ProgressWorker;

import javax.swing.SwingUtilities;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A default swing based {@link FilterModel.Refresher}.
 * @param <T> the model row type
 */
public abstract class AbstractFilterModelRefresher<T> extends FilterModel.AbstractRefresher<T> {

	private ProgressWorker<Collection<T>, ?> refreshWorker;

	/**
	 * @param supplier supplies the items
	 */
	protected AbstractFilterModelRefresher(Supplier<Collection<T>> supplier) {
		super(supplier);
	}

	@Override
	protected final boolean isUserInterfaceThread() {
		return SwingUtilities.isEventDispatchThread();
	}

	protected final void refreshAsync(Consumer<Collection<T>> onRefresh) {
		cancelCurrentRefresh();
		refreshWorker = ProgressWorker.builder(supplier()::get)
						.onStarted(this::onRefreshStarted)
						.onResult(items -> onRefreshResult(items, onRefresh))
						.onException(this::onRefreshFailedAsync)
						.execute();
	}

	protected final void refreshSync(Consumer<Collection<T>> onRefresh) {
		onRefreshStarted();
		try {
			onRefreshResult(supplier().get(), onRefresh);
		}
		catch (Exception e) {
			onRefreshFailedSync(e);
		}
	}

	private void onRefreshStarted() {
		setActive(true);
	}

	private void onRefreshFailedAsync(Exception exception) {
		refreshWorker = null;
		setActive(false);
		notifyFailure(exception);
	}

	private void onRefreshFailedSync(Exception exception) {
		setActive(false);
		if (exception instanceof RuntimeException) {
			throw (RuntimeException) exception;
		}

		throw new RuntimeException(exception);
	}

	private void onRefreshResult(Collection<T> items, Consumer<Collection<T>> onRefresh) {
		refreshWorker = null;
		setActive(false);
		processResult(items);
		if (onRefresh != null) {
			onRefresh.accept(items);
		}
		notifySuccess(items);
	}

	private void cancelCurrentRefresh() {
		ProgressWorker<?, ?> worker = refreshWorker;
		if (worker != null) {
			worker.cancel(true);
		}
	}
}
