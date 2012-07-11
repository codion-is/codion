/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import org.jminor.common.model.EventObserver;
import org.jminor.common.model.StateObserver;

import java.util.Collection;

/**
 * An interface describing an object mapping values to keys, null values are allowed.
 * @param <K> the type of the map keys
 * @param <V> the type of the map values
 */
public interface ValueMap<K, V> extends ValueProvider<K, V>, ValueAsStringProvider<K>, ValueCollectionProvider<V> {

  /**
   * Maps the given value to the given key, returning the old value if any.
   * @param key the key
   * @param value the value
   * @return the previous value mapped to the given key, null if no such value existed
   */
  V setValue(final K key, final V value);

  /**
   * Removes the given key and value from this value map along with the original value if any.
   * If no value is mapped to the given key, this method has no effect.
   * @param key the key to remove
   * @return the value that was removed
   */
  V removeValue(final K key);

  /**
   * Removes all values from this map.
   */
  void clear();

  /**
   * After a call to this method this ValueMap contains the same values and original values as the given map.
   * A null argument to this method clears the destination map of all values and original values.
   * @param sourceMap the map to copy or null for clearing the destination map
   */
  void setAs(final ValueMap<K, V> sourceMap);

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
   * @return an unmodifiable view of the values in this map.
   */
  @Override
  Collection<V> getValues();

  /**
   * @return an unmodifiable view of the keys mapping the values in this ValueMap
   */
  Collection<K> getValueKeys();

  /**
   * @return an unmodifiable view of the keys mapping the original values in this ValueMap
   */
  Collection<K> getOriginalValueKeys();

  /**
   * @return the number of values in this map
   */
  int size();

  /**
   * Returns a deep copy of the given value, immutable values are simply returned.
   * @param value the value to copy
   * @return a deep copy of the given value, or the same instance in case the value is immutable
   */
  V copyValue(final V value);

  /**
   * Returns the original value associated with the given key or the current value if it has not been changed.
   * @param key the key for which to retrieve the original value
   * @return the original value
   */
  V getOriginalValue(final K key);

  /**
   * @return true if a value has been modified.
   */
  boolean isModified();

  /**
   * Returns true if the value associated with the given key has been modified..
   * @param key the key
   * @return true if the value has changed
   */
  boolean isModified(final K key);

  /**
   * Reverts the value associated with the given key to its original value.
   * If the value has not been changed then calling this method has no effect.
   * @param key the key for which to revert the value
   */
  void revertValue(final K key);

  /**
   * Reverts all value changes that have been made.
   * This value map will be unmodified after a call to this method.
   * If no changes have been made then calling this method has no effect.
   */
  void revertAll();

  /**
   * Saves the value associated with the given key, that is, removes the original value.
   * If no original value exists calling this method has no effect.
   * @param key the key for which to save the value
   */
  void saveValue(final K key);

  /**
   * Saves all the value changes that have been made.
   * This value map will be unmodified after a call to this method.
   */
  void saveAll();

  /**
   * @return a deep copy of this value map in it's original state
   */
  ValueMap<K, V> getOriginalCopy();

  /**
   * @return a new ValueMap instance compatible with this instance
   */
  ValueMap<K, V> getInstance();

  /**
   * @return a deep copy of this value map
   */
  ValueMap<K, V> getCopy();

  /**
   * @return a StateObserver indicating if this value map has been modified.
   */
  StateObserver getModifiedState();

  /**
   * Returns an EventObserver notified each time a value changes, with a {@link ValueChangeEvent} argument.
   * @return an EventObserver notified when a value changes.
   * @see org.jminor.common.model.valuemap.ValueChangeEvent
   */
  EventObserver getValueChangeObserver();

  /**
   * Adds a ValueChangeListener, this listener will be notified each time a value changes
   * Adding the same listener multiple times has no effect.
   * @param valueListener the ValueChangeListener
   * @see org.jminor.common.model.valuemap.ValueChangeEvent
   */
  void addValueListener(final ValueChangeListener<K, V> valueListener);

  /**
   * Removes the given value listener if it has been registered with this value map.
   * @param valueListener the ValueChangeListener to remove
   */
  void removeValueListener(final ValueChangeListener valueListener);

  /**
   * Describes an object responsible for providing String representations of ValueMap instances
   * @param <K> the value map key type
   * @param <V> the valueMap type
   */
  interface ToString<K, V extends ValueMap<K, ?>> {
    /**
     * Returns a string representation of the given value map
     * @param valueMap the value map
     * @return a string representation of the value map
     */
    String toString(final V valueMap);
  }
}
