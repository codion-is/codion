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

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;

final class DefaultValueList<T> extends AbstractValueCollection<T, List<T>> implements ValueList<T> {

	private DefaultValueList(DefaultBuilder<T> builder) {
		super(builder);
	}

	/**
	 * <p>{@link Value#set(Object)}, typed to the list itself, which overload resolution prefers whenever a caller
	 * passes the declared type - {@code valueList.set(aList)} lands here rather than on
	 * {@link ValueCollection#set(Collection)}.
	 * <p>It must therefore take the same snapshot. Storing the caller's collection would leave the value's
	 * contents mutable from the outside and able to change behind its back, with no validation and no
	 * notification, and hand observers a snapshot that is not one.
	 * <p>Declared here rather than on {@link AbstractValueCollection}, where it would erase to the same
	 * signature as {@link ValueCollection#set(Collection)}.
	 */
	@Override
	public synchronized void set(@Nullable List<T> values) {
		set((Collection<T>) values);
	}

	@Override
	public synchronized ObservableValueList<T> observable() {
		return (ObservableValueList<T>) super.observable();
	}

	@Override
	protected ObservableValueList<T> createObservable() {
		return new DefaultObservableValueList<>(this);
	}

	static final class DefaultBuilder<T>
					extends AbstractValueCollectionBuilder<List<T>, T, ValueList.Builder<T>>
					implements ValueList.Builder<T> {

		DefaultBuilder() {
			super(emptyList(), ArrayList::new, Collections::unmodifiableList);
		}

		@Override
		public ValueList<T> build() {
			return new DefaultValueList<>(this);
		}
	}

	private static final class DefaultObservableValueList<T>
					extends AbstractObservableValueCollection<T, List<T>>
					implements ObservableValueList<T> {

		private DefaultObservableValueList(ValueList<T> valueList) {
			super(valueList);
		}
	}
}
