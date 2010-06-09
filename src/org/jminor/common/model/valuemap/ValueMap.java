/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

/**
 * An interface describing an object mapping values to keys, null values are allowed.
 * @param <K> the type of the map keys
 * @param <V> the type of the map values
 */
public interface ValueMap<K, V> extends ValueProvider<K, V>, ValueCollectionProvider<V> {

  /**
   * Maps the given value to the given key, returning the old value if any.
   * @param key the key
   * @param value the value
   * @return the previous value mapped to the given key, null if no such value existed
   */
  V setValue(final K key, final V value);

  /**
   * Initializes the value associated with the given key. This method assumes
   * no value has been associated with the key prior to this call, use with care.
   * @param key the key with which to associate the given value
   * @param value the value to associate with the given key
   */
  void initializeValue(final K key, final V value);

  /**
   * Removes the given key and value from this value map along with the original value if any.
   * If no value is mapped to the given key, this method has no effect.
   * @param key the key to remove
   * @return the value that was removed
   */
  V removeValue(final K key);

  /**
   * Returns true if a null value is mapped to the given key.
   * @param key the key
   * @return true if the value mapped to the given key is null
   */
  boolean isValueNull(final K key);

  /**
   * Returns true if this ValueMap contains a value for the given key, that value can be null.
   * @param key the key
   * @return true if a value is mapped to this key
   */
  boolean containsValue(final K key);

  /**
   * Describes an object responsible for providing String representations of ValueMap instances
   * @param <K> the type of the map keys
   */
  interface ToString<K> {
    String toString(final ValueMap<K, ?> valueMap);
  }
}
