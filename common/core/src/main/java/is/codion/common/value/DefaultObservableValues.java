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
 * Copyright (c) 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.value;

import java.util.Collection;
import java.util.Iterator;

class DefaultObservableValues<T, C extends Collection<T>> extends ObservableValue<C>
				implements ObservableValues<T, C> {

	DefaultObservableValues(Values<T, C> values) {
		super(values);
	}

	@Override
	public final Iterator<T> iterator() {
		Values<T, C> values = value();

		return values.iterator();
	}

	@Override
	public final boolean contains(T value) {
		Values<T, C> values = value();

		return values.contains(value);
	}

	@Override
	public final boolean containsAll(Collection<T> values) {
		Values<T, C> valueCollection = value();

		return valueCollection.containsAll(values);
	}

	@Override
	public final boolean empty() {
		Values<T, C> values = value();

		return values.empty();
	}

	@Override
	public final boolean notEmpty() {
		Values<T, C> values = value();

		return values.notEmpty();
	}

	@Override
	public final int size() {
		Values<T, C> values = value();

		return values.size();
	}


}
