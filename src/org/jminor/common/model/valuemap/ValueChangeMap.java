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
   * @return an Event fired when a value changes.
   */
  Event eventValueChanged();

  /**
   * @return a String identifying the map type, for example a table name.
   * in case this value map represents a table row or a class name.
   */
  String getMapTypeID();

  /**
   * Adds a ActionListener, this listener will be notified each time a value changes.
   * @param valueListener the ActionListener
   */
  void addValueListener(final ActionListener valueListener);

  /**
   * Removes the given value listener.
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
   * @return true if a value has been modified since this value map was initialized.
   */
  boolean isModified();

  /**
   * Returns true if the value associated with the given key has been modified since it was initialized.
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
   * If no changes have been made then calling this method has no effect.
   */
  void revertAll();

  /**
   * @return a new ValueChangeMap instance compatible with this instance,
   * preferably with a matching mapTypeID.
   * @see #getMapTypeID()
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
   * Removes all items and change history from this map.
   */
  void clear();

  /**
   * After a call to this method this ValueMap should contain the same values and original values as the given map.
   * A null argument to this method clears the destination map of all values and original values.
   * @param map the map to copy or null for clearing the destination map
   */
  void setAs(final ValueChangeMap<K, V> map);

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
