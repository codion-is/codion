/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import org.jminor.common.model.EventInfoListener;
import org.jminor.common.model.EventInfoObserver;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.valuemap.exception.ValidationException;

import java.util.Collection;

/**
 * Specifies an object which facilitates the editing of values in a ValueMap
 * @param <K> the type of the value map keys
 * @param <V> the type of the value map values
 */
public interface ValueMapEditModel<K, V> {

  /**
   * @return a StateObserver indicating if any values in the underlying value map have been modified
   * @see #isModified()
   */
  StateObserver getModifiedObserver();

  /**
   * @return a StateObserver indicating the valid status of the underlying value map
   * @see #getValidator()
   * @see #isValid()
   */
  StateObserver getValidObserver();

  /**
   * Adds a listener notified each time the value associated with the given key is set via
   * {@link ValueMapEditModel#setValue(Object, Object)}, note that this event is only fired when the the value changes
   * @param key the key for which to monitor value changes
   * @param listener a listener notified each time the value of <code>key</code> is set via this model
   */
  void addValueSetListener(final K key, final EventInfoListener<ValueChangeEvent> listener);

  /**
   * @param key the key
   * @param listener the listener to remove
   */
  void removeValueSetListener(final K key, final EventInfoListener listener);

  /**
   * Adds a listener notified each time the value associated with the given key changes
   * @param key the key for which to monitor value changes
   * @param listener a listener notified each time the value of <code>key</code> changes
   */
  void addValueListener(final K key, final EventInfoListener<ValueChangeEvent> listener);

  /**
   * @param key the key
   * @param listener the listener to remove
   */
  void removeValueListener(final K key, final EventInfoListener listener);

  /**
   * @return an EventObserver notified each time a value changes
   */
  EventInfoObserver<ValueChangeEvent> getValueChangeObserver();

  /**
   * @param key the key for which to retrieve the event
   * @return an EventObserver notified when the value of <code>key</code> changes
   */
  EventInfoObserver<ValueChangeEvent> getValueChangeObserver(K key);

  /**
   * @return the validator
   */
  ValueMap.Validator<K, ? extends ValueMap<K, V>> getValidator();

  /**
   * @param key the key
   * @return true if this value is allowed to be null in the underlying value map
   */
  boolean isNullable(final K key);

  /**
   * @param key the key
   * @return true if the value of the given key is null
   */
  boolean isValueNull(final K key);

  /**
   * Sets the given value in the underlying value map
   * @param key the key to associate the given value with
   * @param value the value to associate with the given key
   */
  void setValue(final K key, final V value);

  /**
   * Returns the value associated with the given key in the underlying value map
   * @param key the key of the value to retrieve
   * @return the value associated with the given key
   */
  V getValue(final K key);

  /**
   * Checks if the value associated with the given key is valid, throws a ValidationException if not
   * @param key the key the value is associated with
   * @throws ValidationException if the given value is not valid for the given key
   */
  void validate(final K key) throws ValidationException;

  /**
   * Validates the current state of the ValueMap
   * @throws ValidationException in case the ValueMap is invalid
   */
  void validate() throws ValidationException;

  /**
   * Validates the given ValueMaps
   * @param valueMaps the value maps to validate
   * @throws ValidationException on finding the first invalid ValueMap
   */
  void validate(final Collection<? extends ValueMap<K, V>> valueMaps) throws ValidationException;

  /**
   * Returns true if the value associated with the given key is valid, using the <code>validate</code> method
   * @param key the key the value is associated with
   * @return true if the value is valid
   * @see #validate(Object)
   * @see ValueMap.Validator#validate(ValueMap)
   */
  boolean isValid(final K key);

  /**
   * @return true if the underlying value map contains only valid values
   * @see #getValidObserver()
   */
  boolean isValid();

  /**
   * @return true if the underlying value map is modified
   * @see #getModifiedObserver()
   */
  boolean isModified();
}
