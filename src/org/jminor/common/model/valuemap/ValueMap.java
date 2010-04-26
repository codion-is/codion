/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

/**
 * An interface describing an object containing values mapped to keys.
 * @param <T> the type of the map keys
 * @param <V> the type of the map values
 */
public interface ValueMap<T, V> extends ValueProvider<T, V>, ValueListProvider<V> {

  /**
   * Maps the given value to the given key, returning the old value if any
   * @param key the key
   * @param value the value
   * @return the previous value mapped to the given key, null if no such value existed
   */
  V setValue(final T key, final V value);

  /**
   * Removes the given key and value from this value map
   * @param key the key to remove
   * @return the value
   */
  V removeValue(final T key);

  /**
   * Returns true if the value mapped to the given key is null
   * @param key the key
   * @return true if the value mapped to the given key is null
   */
  boolean isValueNull(final T key);

  /**
   * Returns true if the value is or represents null.
   * @param value the value
   * @return true if the value is or represents null
   */
  boolean isNull(final V value);

  /**
   * Returns true if this ValueMap contains a value for the given key
   * @param key the key
   * @return true if a value is mapped to this key
   */
  boolean containsValue(final T key);

  /**
   * Describes an object responsible for providing String representations of ValueMap instances
   */
  public interface ToString<T, V> {
    String toString(final ValueMap<T, V> valueMap);
  }
}
