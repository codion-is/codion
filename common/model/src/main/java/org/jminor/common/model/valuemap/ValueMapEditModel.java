/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import org.jminor.common.db.valuemap.ValueChange;
import org.jminor.common.db.valuemap.ValueMap;
import org.jminor.common.db.valuemap.exception.ValidationException;
import org.jminor.common.event.EventDataListener;
import org.jminor.common.event.EventObserver;
import org.jminor.common.state.StateObserver;

import java.util.Collection;

/**
 * Specifies an object which facilitates the editing of values in a ValueMap
 * @param <K> the type of the value map keys
 * @param <V> the type of the value map values
 */
public interface ValueMapEditModel<K, V> {

  /**
   * @return a StateObserver indicating the valid status of the underlying value map
   * @see #getValidator()
   * @see #isValid()
   */
  StateObserver getValidObserver();

  /**
   * Adds a listener notified each time the value associated with the given key is set via
   * {@link ValueMapEditModel#put(Object, Object)}, note that this event is only fired when the value changes
   * @param key the key for which to monitor value changes
   * @param listener a listener notified each time the value of {@code key} is put via this model
   */
  void addValuePutListener(K key, EventDataListener<ValueChange<K, V>> listener);

  /**
   * @param key the key
   * @param listener the listener to remove
   */
  void removeValuePutListener(K key, EventDataListener listener);

  /**
   * Adds a listener notified each time the value associated with the given key changes
   * @param key the key for which to monitor value changes
   * @param listener a listener notified each time the value of {@code key} changes
   */
  void addValueListener(K key, EventDataListener<ValueChange<K, V>> listener);

  /**
   * @param key the key
   * @param listener the listener to remove
   */
  void removeValueListener(K key, EventDataListener listener);

  /**
   * @return an EventObserver notified each time a value changes
   */
  EventObserver<ValueChange<K, V>> getValueObserver();

  /**
   * @param key the key for which to retrieve the event
   * @return an EventObserver notified when the value of {@code key} changes
   */
  EventObserver<ValueChange<K, V>> getValueObserver(K key);

  /**
   * @return the validator
   */
  ValueMap.Validator getValidator();

  /**
   * @param key the key
   * @return true if this value is allowed to be null in the underlying value map
   */
  boolean isNullable(K key);

  /**
   * @param key the key
   * @return true if the value of the given key is null
   */
  boolean isNull(K key);

  /**
   * @param key the key
   * @return true if the value of the given key is not null
   */
  boolean isNotNull(K key);

  /**
   * Sets the given value in the underlying value map
   * @param key the key to associate the given value with
   * @param value the value to associate with the given key
   */
  void put(K key, V value);

  /**
   * Removes the given value from the map
   * @param key the key associated with the value to remove
   * @return the value, if any
   */
  V remove(K key);

  /**
   * Returns the value associated with the given key in the underlying value map
   * @param key the key of the value to retrieve
   * @return the value associated with the given key
   */
  V get(K key);

  /**
   * Checks if the value associated with the given key is valid, throws a ValidationException if not
   * @param key the key the value is associated with
   * @throws ValidationException if the given value is not valid for the given key
   */
  void validate(K key) throws ValidationException;

  /**
   * Validates the current state of the ValueMap
   * @throws ValidationException in case the ValueMap is invalid
   */
  void validate() throws ValidationException;

  /**
   * Validates the current state of the given ValueMap
   * @param valueMap the ValueMap to validate
   * @throws ValidationException in case the ValueMap is invalid
   */
  void validate(ValueMap<K, V> valueMap) throws ValidationException;

  /**
   * Validates the given ValueMaps
   * @param valueMaps the value maps to validate
   * @throws ValidationException on finding the first invalid ValueMap
   */
  void validate(Collection<? extends ValueMap<K, V>> valueMaps) throws ValidationException;

  /**
   * Returns true if the value associated with the given key is valid, using the {@code validate} method
   * @param key the key the value is associated with
   * @return true if the value is valid
   * @see #validate(Object)
   * @see ValueMap.Validator#validate(ValueMap)
   */
  boolean isValid(K key);

  /**
   * @return true if the underlying value map contains only valid values
   * @see #getValidObserver()
   */
  boolean isValid();
}
