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
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * An observable wrapper for one or more values, including a possible null value.
 * A factory for {@link Values} instances.
 * @param <T> the value type
 * @param <C> the collection type
 */
public interface Values<T, C extends Collection<T>> extends Value<C>, ValuesObserver<T, C> {

	/**
	 * Sets the values.
	 * @param values the values to set
	 */
	void set(Collection<T> values);

	/**
	 * Adds a value to this Values instance, returns true if this instance did not contain the value before adding.
	 * @param value the value to add
	 * @return true if the value was added
	 */
	boolean add(T value);

	/**
	 * Adds the given values to this Values instance, returns true unless this instance already contained all the values.
	 * @param values the values to add
	 * @return true if a value was added
	 */
	boolean addAll(T... values);

	/**
	 * Adds the given values to this Values instance, returns true unless this instance already contained all the values.
	 * @param values the values to add
	 * @return true if a value was added
	 */
	boolean addAll(Collection<T> values);

	/**
	 * Removes a value from this Values instance, returns true if this instance contained the value before removing.
	 * @param value the value to remove
	 * @return true if the value was removed
	 */
	boolean remove(T value);

	/**
	 * Removes the given values from this Values instance, returns true if this instance contained one or more of the values.
	 * @param values the values to remove
	 * @return true if a value was removed
	 */
	boolean removeAll(T... values);

	/**
	 * Removes the given values from this Values instance, returns true if this instance contained one or more of the values.
	 * @param values the values to remove
	 * @return true if a value was removed
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
	 * Returns a {@link ValuesObserver} notified each time this value changes.
	 * @return a {@link ValuesObserver} for this value
	 */
	@Override
	ValuesObserver<T, C> observer();

	/**
	 * @param empty creates an empty instance of the required collection type
	 * @param unmodifiable returns an unmodifiable view of the given collection
	 * @return a {@link Values.Builder} instance
	 * @param <T> the value type
	 * @param <C> the collection type
	 */
	static <T, C extends Collection<T>> Builder<T, C, ?> builder(Supplier<C> empty, Function<C, C> unmodifiable) {
		return new DefaultValues.DefaultBuilder<>(empty, unmodifiable);
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
