/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import org.jminor.common.model.Event;
import org.jminor.common.model.State;

import java.awt.event.ActionListener;
import java.util.Collection;

/**
 * A ValueMap extension which keeps track of value modifications.
 * @param <K> the type of the keys in the map
 * @param <V> the type of the values in the map
 */
public interface ValueChangeMap<K, V> extends ValueMap<K, V> {

  /**
   * @return a State active when this value map has been modified.
   */
  State stateModified();

  /**
   * Returns an Event fired each time a value changes, with a ValueChangeEvent argument.
   * @return an Event fired when a value changes.
   * @see org.jminor.common.model.valuemap.ValueChangeEvent
   */
  Event eventValueChanged();

  /**
   * Adds a ActionListener, this listener will be notified each time a value changes,
   * by calling actionPerformed() with a ValueChangeEvent argument.
   * Adding the same listener multiple times has no effect.
   * @param valueListener the ActionListener
   * @see org.jminor.common.model.valuemap.ValueChangeEvent
   */
  void addValueListener(final ActionListener valueListener);

  /**
   * Removes the given value listener if it has been registered with this value map.
   * @param valueListener the ActionListener to remove
   */
  void removeValueListener(final ActionListener valueListener);

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
   * @return a new ValueChangeMap instance compatible with this instance
   */
  ValueChangeMap<K, V> getInstance();

  /**
   * @return a deep copy of this value map
   */
  ValueChangeMap<K, V> getCopy();

  /**
   * @return a deep copy of this value map in it's original state
   */
  ValueChangeMap<K, V> getOriginalCopy();

  /**
   * Removes all values and change history from this map.
   */
  void clear();

  /**
   * After a call to this method this ValueMap contains the same values and original values as the given map.
   * A null argument to this method clears the destination map of all values and original values.
   * @param sourceMap the map to copy or null for clearing the destination map
   */
  void setAs(final ValueChangeMap<K, V> sourceMap);

  /**
   * Returns a deep copy of the given value, immutable values are simply returned.
   * @param value the value to copy
   * @return a deep copy of the given value, or the same instance in case the value is immutable
   */
  V copyValue(final V value);

  /**
   * @return an unmodifiable view of the values in this map.
   */
  Collection<V> getValues();

  /**
   * @return an unmodifiable view of the keys mapping the values in this ValueChangeMap
   */
  Collection<K> getValueKeys();

/**
   * @return an unmodifiable view of the keys mapping the original values in this ValueChangeMap
   */
  Collection<K> getOriginalValueKeys();
}
