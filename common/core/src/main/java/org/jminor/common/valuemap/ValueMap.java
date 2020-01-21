/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.valuemap;

import java.util.Collection;
import java.util.Set;

/**
 * An interface describing an object mapping values to keys, null values are allowed.
 * A ValueMap keeps track of the first value associated with a given key, so that if a value
 * is modified, {@link #getOriginal(Object)} returns that original value and
 * {@link #isModified(Object)} returns true until the value is either saved via
 * {@link #save(Object)} or reverted to its original value via {@link #revert(Object)},
 * note that setting the original value manually has the same effect as calling {@link #revert(Object)}
 * @param <K> the type of the map keys
 * @param <V> the type of the map values
 */
public interface ValueMap<K, V> {

  /**
   * Retrieves the value mapped to the given key
   * @param key the key
   * @return the value mapped to the given key, null if no such mapping exists
   */
  V get(K key);

  /**
   * Maps the given value to the given key, returning the old value if any.
   * @param key the key
   * @param value the value
   * @return the previous value mapped to the given key
   */
  V put(K key, V value);

  /**
   * Removes the given key and value from this value map along with the original value if any.
   * If no value is mapped to the given key, this method has no effect.
   * @param key the key to remove
   * @return the value that was removed, null if no value was found
   */
  V remove(K key);

  /**
   * Retrieves a string representation of the value mapped to the given key, an empty string is returned
   * in case of null values or if key is not found.
   * @param key the key
   * @return the value mapped to the given key as a string, an empty string if no such mapping exists
   */
  String getAsString(K key);

  /**
   * After a call to this method this ValueMap contains the same values and original values as the source map.
   * A null argument to this method clears the destination map of all values and original values.
   * Value change events for affected keys are fired after all values have been set, in no particular order.
   * @param sourceMap the map to copy or null for clearing the destination map
   */
  void setAs(ValueMap<K, V> sourceMap);

  /**
   * Returns true if a null value is mapped to the given key or the key is not found.
   * @param key the key
   * @return true if the value mapped to the given key is null
   */
  boolean isNull(K key);

  /**
   * Returns true if a this ValueMap contains a non-null value mapped to the given key
   * @param key the key
   * @return true if the value mapped to the given key is not null
   */
  boolean isNotNull(K key);

  /**
   * Returns true if this ValueMap contains a value for the given key, that value can be null.
   * @param key the key
   * @return true if a value is mapped to this key
   */
  boolean containsKey(K key);

  /**
   * Retrieves the values contained in this ValueMap.
   * @return a collection containing the values in this ValueMap
   */
  Collection<V> values();

  /**
   * @return an unmodifiable view of the keys mapping the values in this ValueMap
   */
  Set<K> keySet();

  /**
   * @return an unmodifiable view of the keys mapping the original values in this ValueMap
   */
  Set<K> originalKeySet();

  /**
   * @return the number of values in this map
   */
  int size();

  /**
   * Returns the original value associated with the given key or the current value if it has not been modified.
   * @param key the key for which to retrieve the original value
   * @return the original value
   */
  V getOriginal(K key);

  /**
   * @return true if one or more values have been modified.
   */
  boolean isModified();

  /**
   * Returns true if the value associated with the given key has been modified..
   * @param key the key
   * @return true if the value has changed
   */
  boolean isModified(K key);

  /**
   * Reverts the value associated with the given key to its original value.
   * If the value has not been modified or the key is not found then calling this method has no effect.
   * @param key the key for which to revert the value
   */
  void revert(K key);

  /**
   * Reverts all value modifications that have been made.
   * This value map will be unmodified after a call to this method.
   * If no modifications have been made then calling this method has no effect.
   */
  void revertAll();

  /**
   * Saves the value associated with the given key, that is, removes the original value.
   * If no original value exists calling this method has no effect.
   * @param key the key for which to save the value
   */
  void save(K key);

  /**
   * Saves all the value modifications that have been made.
   * This value map will be unmodified after a call to this method.
   */
  void saveAll();
}
