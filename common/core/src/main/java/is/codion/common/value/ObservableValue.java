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
 * Copyright (c) 2019 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.value;

import is.codion.common.observer.Observable;
import is.codion.common.observer.Observer;

import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

class ObservableValue<T> implements Observable<T> {

	private final Value<T> value;

	ObservableValue(Value<T> value) {
		this.value = requireNonNull(value);
	}

	@Override
	public final @Nullable T get() {
		return value.get();
	}

	@Override
	public final boolean nullable() {
		return value.nullable();
	}

	@Override
	public Observer<T> observer() {
		return value.observer();
	}

	protected final <V extends Value<T>> V value() {
		return (V) value;
	}
}
