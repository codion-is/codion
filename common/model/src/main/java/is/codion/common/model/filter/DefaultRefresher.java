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

import is.codion.common.model.filter.FilterModel.AbstractRefresher;
import is.codion.common.utilities.exceptions.Exceptions;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class DefaultRefresher<T> extends AbstractRefresher<T> {

	private final @Nullable Consumer<Collection<T>> onResult;
	private final Consumer<Exception> onException;

	DefaultRefresher(@Nullable Supplier<Collection<T>> items,
									 @Nullable Consumer<Collection<T>> onResult,
	                 @Nullable Consumer<Exception> onException) {
		super(items, false);
		this.onResult = onResult;
		this.onException = onException == null ? new RethrowHandler() : onException;
	}

	@Override
	protected boolean isUserInterfaceThread() {
		return false;
	}

	@Override
	protected void refreshAsync(@Nullable Consumer<Collection<T>> onResult) {
		refreshSync(onResult);
	}

	@Override
	protected void refreshSync(@Nullable Consumer<Collection<T>> onResult) {
		super.items().ifPresent(items -> {
			setActive(true);
			try {
				Collection<T> result = items.get();
				setActive(false);
				processResult(result);
				if (onResult != null) {
					onResult.accept(result);
				}
				notifyResult(result);
			}
			catch (Exception e) {
				setActive(false);
				onException.accept(e);
			}
		});
	}

	@Override
	protected void processResult(Collection<T> result) {
		if (onResult != null) {
			onResult.accept(result);
		}
	}

	private static final class RethrowHandler implements Consumer<Exception> {

		@Override
		public void accept(Exception exception) {
			throw Exceptions.runtime(exception);
		}
	}
}