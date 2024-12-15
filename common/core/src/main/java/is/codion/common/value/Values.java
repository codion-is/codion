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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * An observable wrapper for one or more values, including a possible null value.
 * A factory for {@link Values} instances.
 * @param <T> the value type
 * @param <C> the collection type
 * @see #builder(Supplier, Function)
 */
public interface Values<T, C extends Collection<T>> extends Value<C>, ObservableValues<T, C> {

	@Override
	@NonNull C get();

	@Override
	default boolean nullable() {
		return false;
	}

	/**
	 * Sets the values.
	 * @param values the values to set
	 * @return true if this {@link Values} instance changed
	 */
	boolean set(@Nullable Collection<T> values);

	/**
	 * Adds a value to this Values instance.
	 * @param value the value to add
	 * @return true if the value was added
	 * @see Collection#add(Object)
	 */
	boolean add(@Nullable T value);

	/**
	 * Adds the given values to this Values instance.
	 * @param values the values to add
	 * @return true if a value was added
	 * @see Collection#addAll(Collection)
	 */
	boolean addAll(T... values);

	/**
	 * Adds the given values to this Values instance.
	 * @param values the values to add
	 * @return true if a value was added
	 * @see Collection#addAll(Collection)
	 */
	boolean addAll(Collection<T> values);

	/**
	 * Removes a single instance of the given value from this Values instance.
	 * @param value the value to remove
	 * @return true if the value was removed
	 * @see Collection#remove(Object)
	 */
	boolean remove(@Nullable T value);

	/**
	 * Removes the given values from this Values instance.
	 * @param values the values to remove
	 * @return true if a value was removed
	 * @see Collection#removeAll(Collection)
	 */
	boolean removeAll(T... values);

	/**
	 * Removes the given values from this Values instance.
	 * @param values the values to remove
	 * @return true if a value was removed
	 * @see Collection#removeAll(Collection)
	 */
	boolean removeAll(Collection<T> values);

	/**
	 * Returns a {@link Value} instance based on this {@link Values}.
	 * Setting this value to null clears the values.
	 * This value consistently returns the first value from the
	 * underlying {@link Values} in case it is sequenced and contains multiple items.
	 * @return a single item value based on this values instance
	 */
	Value<T> value();

	/**
	 * Returns an {@link ObservableValues} notified each time this value changes.
	 * @return an {@link ObservableValues} for this value
	 */
	@Override
	ObservableValues<T, C> observable();

	/**
	 * @param create creates an empty instance of the required collection type
	 * @param unmodifiable returns an unmodifiable view of the given collection
	 * @param <T> the value type
	 * @param <C> the collection type
	 * @return a {@link Values.Builder} instance
	 */
	static <T, C extends Collection<T>> Builder<T, C, ?> builder(Supplier<C> create, Function<C, C> unmodifiable) {
		return new DefaultValues.DefaultBuilder<>(create, unmodifiable);
	}

	/**
	 * Builds a {@link Values} instance.
	 * @param <T> the value type
	 * @param <C> the Collection type
	 * @param <B> the builder type
	 */
	interface Builder<T, C extends Collection<T>, B extends Builder<T, C, B>>
					extends Value.Builder<C, B> {

		/**
		 * @return a new {@link Values} instance based on this builder
		 */
		Values<T, C> build();
	}
}
