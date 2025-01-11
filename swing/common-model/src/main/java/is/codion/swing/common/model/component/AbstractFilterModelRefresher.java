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

	protected final void refreshAsync(Consumer<Collection<T>> onResult) {
		cancelCurrentRefresh();
		refreshWorker = ProgressWorker.builder(supplier()::get)
						.onStarted(this::onRefreshStarted)
						.onResult(items -> onRefreshResult(items, onResult))
						.onException(this::onRefreshFailedAsync)
						.execute();
	}

	protected final void refreshSync(Consumer<Collection<T>> onResult) {
		onRefreshStarted();
		try {
			onRefreshResult(supplier().get(), onResult);
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
		notifyException(exception);
	}

	private void onRefreshFailedSync(Exception exception) {
		setActive(false);
		if (exception instanceof RuntimeException) {
			throw (RuntimeException) exception;
		}

		throw new RuntimeException(exception);
	}

	private void onRefreshResult(Collection<T> result, Consumer<Collection<T>> onResult) {
		refreshWorker = null;
		setActive(false);
		processResult(result);
		if (onResult != null) {
			onResult.accept(result);
		}
		notifyResult(result);
	}

	private void cancelCurrentRefresh() {
		ProgressWorker<?, ?> worker = refreshWorker;
		if (worker != null) {
			worker.cancel(true);
		}
	}
}
