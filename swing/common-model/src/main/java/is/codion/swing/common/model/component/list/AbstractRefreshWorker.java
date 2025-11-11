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
package is.codion.swing.common.model.component.list;

import is.codion.common.model.filter.FilterModel;
import is.codion.common.model.filter.FilterModel.AbstractRefresher;
import is.codion.swing.common.model.worker.ProgressWorker;

import org.jspecify.annotations.Nullable;

import javax.swing.SwingUtilities;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A {@link ProgressWorker} based {@link FilterModel.Refresher}.
 * @param <T> the model row type
 */
public abstract class AbstractRefreshWorker<T> extends AbstractRefresher<T> {

	private final Consumer<Exception> onException;

	private @Nullable ProgressWorker<Collection<T>, ?> worker;

	/**
	 * @param items supplies the items
	 * @param async true if async refresh should be used
	 * @param onException exceptions are rethrown by default, override with this exception handler
	 */
	protected AbstractRefreshWorker(@Nullable Supplier<Collection<T>> items, boolean async,
																	@Nullable Consumer<Exception> onException) {
		super(items, async);
		this.onException = onException == null ? new RethrowExceptionHandler() : onException;
	}

	@Override
	protected final boolean isUserInterfaceThread() {
		return SwingUtilities.isEventDispatchThread();
	}

	protected final void refreshAsync(Consumer<Collection<T>> onResult) {
		items().ifPresent(items -> {
			cancelCurrentRefresh();
			worker = ProgressWorker.builder()
							.task(items::get)
							.onStarted(this::onRefreshStarted)
							.onResult(result -> onRefreshResult(result, onResult))
							.onException(this::onException)
							.execute();
		});
	}

	protected final void refreshSync(Consumer<Collection<T>> onResult) {
		items().ifPresent(items -> {
			onRefreshStarted();
			try {
				onRefreshResult(items.get(), onResult);
			}
			catch (Exception e) {
				onException(e);
			}
		});
	}

	private void onRefreshStarted() {
		setActive(true);
	}

	private void onException(Exception exception) {
		worker = null;
		setActive(false);
		onException.accept(exception);
	}

	private void onRefreshResult(Collection<T> result, Consumer<Collection<T>> onResult) {
		worker = null;
		setActive(false);
		processResult(result);
		if (onResult != null) {
			onResult.accept(result);
		}
		notifyResult(result);
	}

	private void cancelCurrentRefresh() {
		ProgressWorker<?, ?> progressWorker = worker;
		if (progressWorker != null) {
			progressWorker.cancel(true);
		}
	}

	private static final class RethrowExceptionHandler implements Consumer<Exception> {

		@Override
		public void accept(Exception exception) {
			if (exception instanceof RuntimeException) {
				throw (RuntimeException) exception;
			}

			throw new RuntimeException(exception);
		}
	}
}
