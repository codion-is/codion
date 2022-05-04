/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

import java.util.Collections;
import java.util.Set;

/**
 * A Value holding a set of values, including a possible null value.
 * A factory class for {@link ValueSet} instances.
 * @param <T> the value type
 */
public interface ValueSet<T> extends Value<Set<T>> {

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
  boolean isEmpty();

  /**
   * @return true if this value set is not empty
   */
  boolean isNotEmpty();

  /**
   * Clears all values from this ValueSet.
   */
  void clear();

  /**
   * Creates a new {@link Value} instance based on this {@link ValueSet}.
   * Setting the value to null clears the value set.
   * This value returns a random value from the underlying {@link ValueSet} in
   * case it contains multiple items.
   * @return a single item value based on this value set
   */
  Value<T> value();

  /**
   * Instantiates a new empty ValueSet
   * @param <T> the value type
   * @return a ValueSet
   */
  static <T> ValueSet<T> valueSet() {
    return valueSet(Collections.emptySet());
  }

  /**
   * Instantiates a new ValueSet
   * @param initialValues the initial values, may not be null
   * @param <T> the value type
   * @return a ValueSet
   */
  static <T> ValueSet<T> valueSet(Set<T> initialValues) {
    return new DefaultValueSet<>(initialValues);
  }
}
