/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import org.jminor.common.model.Event;
import org.jminor.common.model.Refreshable;
import org.jminor.common.model.State;
import org.jminor.common.model.valuemap.exception.ValidationException;

public interface ValueChangeMapEditModel<K, V> extends Refreshable {

  /**
   * @return a State indicating the modified status of this value map
   * @see #isModified()
   */
  State stateModified();
  /**
   * @return a State indicating the valid status of this value map
   * @see #getValidator()
   * @see #isValid()
   */
  State stateValid();

  /**
   * @param key the key for which to retrieve the event
   * @return an Event object which fires when the value of <code>key</code> is changed via
   * the <code>setValue()</code> methods
   * @see #setValue(Object, Object)
   */
  Event getValueSetEvent(final K key);

  /**
   * @return an Event fired when the active value map has been changed
   * @see #setValueMap(org.jminor.common.model.valuemap.ValueMap)
   */
  Event eventValueMapSet();

  /**
   * @param key the key for which to retrieve the event
   * @return an Event object which fires when the value of <code>key</code> changes
   */
  Event getValueChangeEvent(K key);

  /**
   * @return the validator
   */
  ValueMapValidator<K, V> getValidator();

  /**
   * Sets the active value map, that is, deep copies the value from the source map into the underlying map
   * @param valueMap the map to set as active, if null then the default map value is set as active
   * @see #getDefaultValueMap()
   * @see #eventValueMapSet()
   */
  void setValueMap(final ValueMap<K, V> valueMap);

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
  ValueMap<K, V> getDefaultValueMap();

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
   * @see #stateValid() ()
   */
  boolean isValid();

  /**
   * @return true if the underlying value map is modified
   * @see #stateModified()
   */
  boolean isModified();
}
