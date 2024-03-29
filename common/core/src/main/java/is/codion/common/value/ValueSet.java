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
 * A factory class for {@link ValueSet} instances.
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
	 * Clears all values from this ValueSet.
	 */
	void clear();

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
		return valueSet(Collections.emptySet(), Notify.WHEN_CHANGED);
	}

	/**
	 * Creates a new empty {@link ValueSet}
	 * @param <T> the value type
	 * @param notify specifies when this value set notifies its listeners
	 * @return a new {@link ValueSet}
	 */
	static <T> ValueSet<T> valueSet(Notify notify) {
		return valueSet(Collections.emptySet(), notify);
	}

	/**
	 * Creates a new {@link ValueSet}, using {@link Notify#WHEN_CHANGED}.
	 * @param initialValues the initial values, may not be null
	 * @param <T> the value type
	 * @return a new {@link ValueSet}
	 */
	static <T> ValueSet<T> valueSet(Set<T> initialValues) {
		return valueSet(initialValues, Notify.WHEN_CHANGED);
	}

	/**
	 * Creates a new {@link ValueSet}
	 * @param initialValues the initial values, may not be null
	 * @param notify specifies when this value set notifies its listeners
	 * @param <T> the value type
	 * @return a new {@link ValueSet}
	 */
	static <T> ValueSet<T> valueSet(Set<T> initialValues, Notify notify) {
		return new DefaultValueSet<>(initialValues, notify);
	}
}
