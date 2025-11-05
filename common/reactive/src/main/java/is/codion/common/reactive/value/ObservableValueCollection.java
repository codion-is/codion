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
 * Copyright (c) 2024 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.reactive.value;

import is.codion.common.reactive.observer.Observable;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;

/**
 * A read only values observable
 * @param <T> the values type
 * @param <C> the collection type
 */
public interface ObservableValueCollection<T, C extends Collection<T>> extends Observable<C>, Iterable<T> {

	@Override
	@NonNull C get();

	/**
	 * Returns true if this {@link ValueCollection} instance contains the specified element
	 * @param value the element
	 * @return true if this {@link ValueCollection} instance contains the specified element
	 */
	boolean contains(@Nullable T value);

	/**
	 * Returns true if this {@link ValueCollection} instance contains all the elements of the specified collection
	 * @param values the elements to check
	 * @return true if this {@link ValueCollection} instance contains all the elements of the specified collection
	 */
	boolean containsAll(Collection<T> values);

	/**
	 * @return true if this {@link ValueCollection} instance is empty
	 */
	boolean isEmpty();

	/**
	 * @return the number of elements in this {@link ValueCollection} instance
	 */
	int size();

	/**
	 * @return the values or an empty Optional if the collection is empty.
	 */
	@Override
	Optional<C> optional();
}
