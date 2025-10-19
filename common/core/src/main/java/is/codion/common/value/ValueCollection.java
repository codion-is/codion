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
package is.codion.common.value;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;

/**
 * <p>An observable wrapper for one or more values.
 * <p>All implementations are thread-safe and support concurrent access.
 * @param <T> the value type
 * @param <C> the collection type
 * @see ValueSet
 * @see ValueList
 */
public interface ValueCollection<T, C extends Collection<T>> extends Value<C>, ObservableValueCollection<T, C> {

	@Override
	@NonNull C get();

	@Override
	default boolean isNullable() {
		return false;
	}

	/**
	 * Sets the values.
	 * @param values the values to set
	 */
	void set(@Nullable Collection<T> values);

	/**
	 * Adds a value to this {@link ValueCollection} instance.
	 * @param value the value to add
	 * @return true if the value was added
	 * @see Collection#add(Object)
	 */
	boolean add(@Nullable T value);

	/**
	 * Adds the given values to this {@link ValueCollection} instance.
	 * @param values the values to add
	 * @return true if a value was added
	 * @see Collection#addAll(Collection)
	 */
	boolean addAll(T... values);

	/**
	 * Adds the given values to this {@link ValueCollection} instance.
	 * @param values the values to add
	 * @return true if a value was added
	 * @see Collection#addAll(Collection)
	 */
	boolean addAll(Collection<T> values);

	/**
	 * Removes a single instance of the given value from this {@link ValueCollection} instance.
	 * @param value the value to remove
	 * @return true if the value was removed
	 * @see Collection#remove(Object)
	 */
	boolean remove(@Nullable T value);

	/**
	 * Removes the given values from this {@link ValueCollection} instance.
	 * @param values the values to remove
	 * @return true if a value was removed
	 * @see Collection#removeAll(Collection)
	 */
	boolean removeAll(T... values);

	/**
	 * Removes the given values from this {@link ValueCollection} instance.
	 * @param values the values to remove
	 * @return true if a value was removed
	 * @see Collection#removeAll(Collection)
	 */
	boolean removeAll(Collection<T> values);

	/**
	 * Returns a {@link Value} instance based on this {@link ValueCollection}.
	 * Setting this value to null clears the values.
	 * This value consistently returns the first value from the
	 * underlying {@link ValueCollection} in case it is sequenced and contains multiple items.
	 * @return a single item value based on this {@link ValueCollection} instance
	 */
	Value<T> value();

	/**
	 * @return the values or an empty Optional if the collection is empty.
	 */
	@Override
	Optional<C> optional();

	/**
	 * Returns an {@link ObservableValueCollection} notified each time this value changes.
	 * @return an {@link ObservableValueCollection} for this value
	 */
	@Override
	ObservableValueCollection<T, C> observable();

	/**
	 * Builds a {@link ValueCollection} instance.
	 * @param <T> the value type
	 * @param <C> the Collection type
	 * @param <B> the builder type
	 */
	interface Builder<T, C extends Collection<T>, B extends Builder<T, C, B>> extends Value.Builder<C, B> {

		/**
		 * @return a new {@link ValueCollection} instance based on this builder
		 */
		ValueCollection<T, C> build();
	}
}
