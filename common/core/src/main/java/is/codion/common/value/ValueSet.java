/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
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
   * @param value
   * @return true if the value was removed
   */
  boolean remove(V value);

  /**
   * Clears all values from this ValueSet.
   */
  void clear();
}
