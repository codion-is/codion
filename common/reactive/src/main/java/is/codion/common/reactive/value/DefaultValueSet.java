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
 * Copyright (c) 2020 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.reactive.value;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

final class DefaultValueSet<T> extends AbstractValueCollection<T, Set<T>> implements ValueSet<T> {

	private DefaultValueSet(DefaultBuilder<T> builder) {
		super(builder);
	}

	@Override
	public ObservableValueSet<T> observable() {
		return (ObservableValueSet<T>) super.observable();
	}

	@Override
	protected ObservableValueSet<T> createObservable() {
		return new DefaultObservableValueSet<>(this);
	}

	static final class DefaultBuilder<T>
					extends AbstractValueCollectionBuilder<Set<T>, T, ValueSet.Builder<T>>
					implements ValueSet.Builder<T> {

		DefaultBuilder() {
			super(LinkedHashSet::new, Collections::unmodifiableSet);
		}

		@Override
		public ValueSet<T> build() {
			return new DefaultValueSet<>(this);
		}
	}

	private static final class DefaultObservableValueSet<T>
					extends AbstractObservableValueCollection<T, Set<T>>
					implements ObservableValueSet<T> {

		private DefaultObservableValueSet(ValueSet<T> valueSet) {
			super(valueSet);
		}
	}
}
