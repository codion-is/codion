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
 * @param <T> the type of the keys in the map
 * @param <V> the type of the values in the map
 */
public interface ValueChangeMap<T, V> extends ValueMap<T, V> {

  /**
   * @return a State active when this value map has been modified
   */
  State stateModified();

  /**
   * @return an Event fired when a value changes
   */
  Event eventValueChanged();

  /**
   * @return a String identifying the map type, for example a table name
   * in case this value map represents a table row
   */
  String getMapTypeID();

  /**
   * Adds a ActionListener, this listener will be notified each time a value changes
   * @param valueListener the ActionListener
   */
  void addValueListener(final ActionListener valueListener);

  /**
   * Removes the given value listener
   * @param valueListener the ActionListener to remove
   */
  void removeValueListener(final ActionListener valueListener);

  /**
   * Returns the original value associated with the given key
   * @param key the key for which to retrieve the original value
   * @return the original value
   */
  V getOriginalValue(final T key);

  /**
   * @return true if a value has been modified since this value map was initialized
   */
  boolean isModified();

  /**
   * Returns true if the value associated with the given key has been modified since it was initialized
   * @param key the key
   * @return true if the value has changed
   */
  boolean isModified(final T key);

  /**
   * Clears the orignal values rendering the map unmodified
   */
  public void clearOriginalValues();

  /**
   * Reverts the value associated with the given key to its original value
   * @param key the key for which to revert the value
   */
  void revertValue(final T key);

  /**
   * Reverts all value changes that have been made
   */
  void revertAll();

  /**
   * Removes all items and change history from this map
   */
  void clear();

  /**
   * After a call to this method this ValueMap should contain the same values and original values as the given map
   * @param map the map to copy
   */
  void setAs(final ValueChangeMap<T, V> map);

  /**
   * Returns a deep copy of the given value, if the value is immutable then returning the same instance is fine
   * @param value the value to copy
   * @return a deep copy of the given value, or the same instance in case the value is immutable
   */
  V copyValue(final V value);

  /**
   * @return the keys mapping the values in this ValueChangeMap
   */
  Collection<T> getValueKeys();

/**
   * @return the keys mapping the original values in this ValueChangeMap
   */
  Collection<T> getOriginalValueKeys();
}
