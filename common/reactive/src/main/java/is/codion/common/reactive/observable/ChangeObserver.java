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

import org.jspecify.annotations.Nullable;

import static is.codion.common.reactive.observable.Change.change;
import static java.util.Objects.deepEquals;

final class ChangeObserver<T> extends AbstractObserver<Change<T>> {

	private @Nullable T current;

	ChangeObserver(Observable<T> observable) {
		current = observable.get();
		observable.addConsumer(this::notify);
	}

	private void notify(@Nullable T newValue) {
		T previous = current;
		current = newValue;
		if (!deepEquals(previous, newValue)) {
			notifyListeners(change(previous, newValue));
		}
	}
}
