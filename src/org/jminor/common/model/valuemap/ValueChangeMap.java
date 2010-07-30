/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import org.jminor.common.model.EventObserver;
import org.jminor.common.model.StateObserver;

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
  StateObserver getModifiedState();

  /**
   * Returns an Event fired each time a value changes, with a ValueChangeEvent argument.
   * @return an Event fired when a value changes.
   * @see org.jminor.common.model.valuemap.ValueChangeEvent
   */
  EventObserver getValueChangeObserver();

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
   * Initializes the value associated with the given key. This method assumes
   * no value has been associated with the key prior to this call, use with care.
   * @param key the key with which to associate the given value
   * @param value the value to associate with the given key
   */
  void initializeValue(final K key, final V value);

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
  ValueChangeMap<K, V> getOriginalCopy();

/**
   * @return an unmodifiable view of the keys mapping the original values in this ValueChangeMap
   */
  Collection<K> getOriginalValueKeys();
}
