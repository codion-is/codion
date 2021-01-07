/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

import java.util.Set;

/**
 * A Value holding a set of values, including a possible null value.
 * @param <V> the value type
 */
public interface ValueSet<V> extends Value<Set<V>> {

  /**
   * Adds a value to this set, returns true if the set did not contain the value before adding.
   * @param value the value to add
   * @return true if the value was added
   */
  boolean add(V value);

  /**
   * Removes a value from this set, returns true if the set contained the value before removing.
   * @param value the value to remove
   * @return true if the value was removed
   */
  boolean remove(V value);

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
  Value<V> value();
}
