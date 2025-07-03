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
 * Copyright (c) 2019 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.event;

import is.codion.common.observable.Observer;

import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

final class DefaultEvent<T> implements Event<T> {

	private final Lock lock = new Lock() {};

	private volatile @Nullable DefaultObserver<T> observer;

	@Override
	public void run() {
		accept(null);
	}

	@Override
	public void accept(@Nullable T data) {
		if (observer != null) {
			observer.notifyListeners(data);
		}
	}

	@Override
	public Observer<T> observer() {
		synchronized (lock) {
			if (observer == null) {
				observer = new DefaultObserver<>();
			}

			return observer;
		}
	}

	@Override
	public boolean addListener(Runnable listener) {
		return observer().addListener(listener);
	}

	@Override
	public boolean removeListener(Runnable listener) {
		return observer().removeListener(listener);
	}

	@Override
	public boolean addConsumer(Consumer<? super T> consumer) {
		return observer().addConsumer(consumer);
	}

	@Override
	public boolean removeConsumer(Consumer<? super T> consumer) {
		return observer().removeConsumer(consumer);
	}

	@Override
	public boolean addWeakListener(Runnable listener) {
		return observer().addWeakListener(listener);
	}

	@Override
	public boolean removeWeakListener(Runnable listener) {
		return observer().removeWeakListener(listener);
	}

	@Override
	public boolean addWeakConsumer(Consumer<? super T> consumer) {
		return observer().addWeakConsumer(consumer);
	}

	@Override
	public boolean removeWeakConsumer(Consumer<? super T> consumer) {
		return observer().removeWeakConsumer(consumer);
	}

	private interface Lock {}
}
