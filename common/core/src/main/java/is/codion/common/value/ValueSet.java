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
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson.
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
public interface ValueSet<T> extends Value<Set<T>> {

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
   * Removes a value from this set, returns true if the set contained the value before removing.
   * @param value the value to remove
   * @return true if the value was removed
   */
  boolean remove(T value);

  /**
   * @return true if this value set is empty
   */
  boolean empty();

  /**
   * @return true if this value set is not empty
   */
  boolean notEmpty();

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
