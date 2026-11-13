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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.reactive.observable;

import is.codion.common.reactive.observer.AbstractObserver;
import is.codion.common.reactive.observer.Observer;

import org.jspecify.annotations.Nullable;

import static is.codion.common.reactive.observable.Change.change;
import static java.util.Objects.deepEquals;

final class DefaultChangeObserver<T> extends AbstractObserver<Change<T>> implements ChangeObserver<T> {

	private final ValueObserver<T> previous = new ValueObserver<>();
	private final ValueObserver<T> current = new ValueObserver<>();

	private final Observable<T> observable;

	private @Nullable T value;

	DefaultChangeObserver(Observable<T> observable) {
		this.observable = observable;
		this.value = observable.get();
		//a listener rather than a consumer, reading the current value, not the one notified, which a listener before
		//it may have changed since, the change to the new one then reported already
		observable.addListener(this::notifyChange);
	}

	@Override
	public Observer<T> previous() {
		return previous;
	}

	@Override
	public Observer<T> current() {
		return current;
	}

	private void notifyChange() {
		T newValue = observable.get();
		T previousValue = value;
		//an equal value replaces the one held without notifying, so previous() is the latest instance notified
		value = newValue;
		if (!deepEquals(previousValue, newValue)) {
			notifyListeners(change(previousValue, newValue));
			previous.notify(previousValue);
			current.notify(newValue);
		}
	}

	private static final class ValueObserver<T> extends AbstractObserver<T> {

		private void notify(@Nullable T value) {
			notifyListeners(value);
		}
	}
}
