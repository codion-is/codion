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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.reactive.value;

import is.codion.common.reactive.observer.DefaultObserver;
import is.codion.common.reactive.value.Value.Change;

import org.jspecify.annotations.Nullable;

import static java.util.Objects.deepEquals;

final class ChangeObserver<T> extends DefaultObserver<Change<T>> {

	private @Nullable T current;

	ChangeObserver(Value<T> value) {
		current = value.get();
		value.addConsumer(this::notify);
	}

	private void notify(@Nullable T newValue) {
		if (!deepEquals(current, newValue)) {
			T previous = current;
			current = newValue;
			notifyListeners(new DefaultChange<>(previous, newValue));
		}
	}

	private static final class DefaultChange<T> implements Change<T> {

		private final @Nullable T previous;
		private final @Nullable T current;

		private DefaultChange(@Nullable T previous, @Nullable T current) {
			this.previous = previous;
			this.current = current;
		}

		@Override
		public @Nullable T previous() {
			return previous;
		}

		@Override
		public @Nullable T current() {
			return current;
		}
	}
}
