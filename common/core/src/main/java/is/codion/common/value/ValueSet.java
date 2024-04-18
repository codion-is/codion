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
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.value;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * An observable wrapper for one or more values, including a possible null value.
 * A factory for {@link ValueSet} instances.
 * @param <T> the value type
 */
public interface ValueSet<T> extends Value<Set<T>>, ValueSetObserver<T> {

	/**
	 * Sets the values. Note that duplicates are quietly dropped.
	 * @param values the values to set
	 */
	void set(Collection<T> values);

	/**
	 * Adds a value to this set, returns true if the set did not contain the value before adding.
	 * @param value the value to add
	 * @return true if the value was added
	 */
	boolean add(T value);

	/**
	 * Adds the given values to this set, returns true unless the set already contained all the values.
	 * @param values the values to add
	 * @return true if a value was added
	 */
	boolean addAll(T... values);

	/**
	 * Adds the given values to this set, returns true unless the set already contained all the values.
	 * @param values the values to add
	 * @return true if a value was added
	 */
	boolean addAll(Collection<T> values);

	/**
	 * Removes a value from this set, returns true if the set contained the value before removing.
	 * @param value the value to remove
	 * @return true if the value was removed
	 */
	boolean remove(T value);

	/**
	 * Removes the given values from this set, returns true if the set contained one or more of the values.
	 * @param values the values to remove
	 * @return true if value was removed
	 */
	boolean removeAll(T... values);

	/**
	 * Removes the given values from this set, returns true if the set contained one or more of the values.
	 * @param values the values to remove
	 * @return true if value was removed
	 */
	boolean removeAll(Collection<T> values);

	/**
	 * Returns a {@link Value} instance based on this {@link ValueSet}.
	 * Setting this value to null clears the value set.
	 * This value consistently returns the first value from the
	 * underlying {@link ValueSet} in case it contains multiple items.
	 * @return a single item value based on this value set
	 */
	Value<T> value();

	/**
	 * Returns a {@link ValueSetObserver} notified each time this value changes.
	 * @return a {@link ValueSetObserver} for this value
	 */
	@Override
	ValueSetObserver<T> observer();

	/**
	 * Creates a new empty {@link ValueSet}, using {@link Notify#WHEN_CHANGED}.
	 * @param <T> the value type
	 * @return a new {@link ValueSet}
	 */
	static <T> ValueSet<T> valueSet() {
		return builder(Collections.<T>emptySet()).build();
	}

	/**
	 * Creates a new {@link ValueSet}, using {@link Notify#WHEN_CHANGED}.
	 * @param initialValue the initial value, may not be null
	 * @param <T> the value type
	 * @return a new {@link ValueSet}
	 */
	static <T> ValueSet<T> valueSet(Set<T> initialValue) {
		return builder(initialValue).build();
	}

	/**
	 * Creates a new {@link ValueSet.Builder} instance.
	 * @return a new builder
	 * @param <T> the value set type
	 */
	static <T> Builder<T> builder() {
		return builder(Collections.<T>emptySet());
	}

	/**
	 * Creates a new {@link ValueSet.Builder} instance.
	 * @param initialValue the initial value
	 * @return a new builder
	 * @param <T> the value set type
	 * @throws NullPointerException in case {@code initialValue} is null
	 */
	static <T> Builder<T> builder(Set<T> initialValue) {
		return new DefaultValueSet.DefaultBuilder<T>()
						.initialValue(initialValue);
	}

	/**
	 * Builds a {@link ValueSet} instance.
	 * @param <T> the value type
	 */
	interface Builder<T> {

		/**
		 * @param initialValue the initial value
		 * @return this builder instance
		 */
		Builder<T> initialValue(Set<T> initialValue);

		/**
		 * @param notify the notify policy for this value set, default {@link Notify#WHEN_CHANGED}
		 * @return this builder instance
		 */
		Builder<T> notify(Notify notify);

		/**
		 * Adds a validator to the resulting value
		 * @param validator the validator to add
		 * @return this builder instance
		 */
		Builder<T> validator(Validator<Set<T>> validator);

		/**
		 * Links the given value to the resulting value
		 * @param originalValueSet the original value set to link
		 * @return this builder instance
		 * @see Value#link(Value)
		 */
		Builder<T> link(ValueSet<T> originalValueSet);

		/**
		 * Links the given value observer to the resulting value
		 * @param originalValueSet the value set observer to link
		 * @return this builder instance
		 * @see ValueSet#link(ValueObserver)
		 */
		Builder<T> link(ValueSetObserver<T> originalValueSet);

		/**
		 * @return a new {@link ValueSet} instance based on this builder
		 */
		ValueSet<T> build();
	}
}
