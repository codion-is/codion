/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Refreshable;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.valuemap.exception.ValidationException;

import java.awt.event.ActionListener;

/**
 * Specifies an object which facilitates the editing of values in a ValueChangeMap
 * @param <K> the type of the value map keys
 * @param <V> the type of the value map values
 */
public interface ValueChangeMapEditModel<K, V> extends Refreshable {

  /**
   * @return a StateObserver indicating if any values in this value map have been modified
   * @see #isModified()
   */
  StateObserver getModifiedObserver();

  /**
   * @return a StateObserver indicating the valid status of this value map
   * @see #getValidator()
   * @see #isValid()
   */
  StateObserver getValidObserver();

  /**
   * @param key the key for which to monitor value changes
   * @param listener a listener notified each time the value of <code>key</code> is set
   */
  void addValueSetListener(final K key, final ActionListener listener);

  /**
   * @param key the key
   * @param listener the listener to remove
   */
  void removeValueSetListener(final K key, final ActionListener listener);

  /**
   * @param listener a listener notified each time the value map is set
   */
  void addValueMapSetListener(final ActionListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeValueMapSetListener(final ActionListener listener);

  /**
   * @param key the key for which to monitor value changes
   * @param listener a listener notified each time the value of <code>key</code> changes
   */
  void addValueListener(final K key, final ActionListener listener);

  /**
   * @param key the key
   * @param listener the listener to remove
   */
  void removeValueListener(final K key, final ActionListener listener);

  /**
   * @param key the key for which to retrieve the event
   * @return an EventObserver which fires when the value of <code>key</code> changes
   */
  EventObserver getValueChangeObserver(K key);

  /**
   * @return the validatorAFTM
   */
  ValueMapValidator<K, V> getValidator();

  /**
   * Sets the active value map, that is, deep copies the value from the source map into the underlying map
   * @param valueMap the map to set as active, if null then the default map value is set as active
   * @see #getDefaultValueMap()
   * @see #addValueMapSetListener(java.awt.event.ActionListener)
   */
  void setValueMap(final ValueChangeMap<K, V> valueMap);

  /**
   * @param key the key
   * @return true if this value is allowed to be null in the underlying value map
   */
  boolean isNullable(K key);

  /**
   * @param key the key
   * @return true if the value of the given key is null
   */
  boolean isValueNull(K key);

  /**
   * Sets the given value in the underlying value map
   * @param key the key to associate the given value with
   * @param value the value to associate with the given key
   */
  void setValue(K key, V value);

  /**
   * Returns the value associated with the given key in the underlying value map
   * @param key the key of the value to retrieve
   * @return the value associated with the given key
   */
  V getValue(K key);

  /**
   * @return a value map containing the default values
   */
  ValueChangeMap<K, V> getDefaultValueMap();

  /**
   * Checks if the value associated with the give key is valid, throws a ValidationException if not
   * @param key the key
   * @param action describes the action requiring validation,
   * ValueChangeMapEditModel.INSERT, ValueChangeMapEditModel.UPDATE or ValueChangeMapEditModel.UNKNOWN
   * @throws org.jminor.common.model.valuemap.exception.ValidationException if the given value is not valid for the given key
   */
  void validate(final K key, final int action) throws ValidationException;

  /**
   * Returns true if the given value is valid for the given key, using the <code>validate</code> method
   * @param key the key
   * @param action describes the action requiring validation,
   * ValueChangeMapEditModel.INSERT, ValueChangeMapEditModel.UPDATE or ValueChangeMapEditModel.UNKNOWN
   * @return true if the value is valid
   * @see #validate(Object, int)
   * @see ValueMapValidator#validate(ValueMap, Object, int)
   */
  boolean isValid(final K key, final int action);

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
