package org.jminor.common.model;

import java.awt.event.ActionEvent;

/**
 * A ValueMap extension which keeps track of value modifications.
 */
public interface ChangeValueMap<T, V> extends ValueMap<T, V> {

  /**
   * @return a State active when this value map has been modified
   */
  State stateModified();

  /**
   * @return an Event fired when a value changes
   */
  Event eventPropertyChanged();

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
   * Reverts the value associated with the given key to its original value
   * @param key the key for which to revert the value
   */
  void revertValue(final T key);

  /**
   * Returns an ActionEvent describing the value change.
   * @param key the key of the value being changed
   * @param newValue the new value
   * @param oldValue the old value
   * @param initialization true if the value is being initialized
   * @return an ActionEvent describing the value change
   */
  ActionEvent getValueChangeEvent(final T key, final V newValue, final V oldValue, final boolean initialization);
}
