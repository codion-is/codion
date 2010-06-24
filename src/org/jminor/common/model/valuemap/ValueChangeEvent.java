/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import org.jminor.common.model.Util;

import java.awt.event.ActionEvent;

/**
 * Used when value change events are fired
 */
public class ValueChangeEvent<K, V> extends ActionEvent {

  /**
   * The value map owning the value
   */
  private final ValueChangeMap<K, V> valueOwner;

  /**
   * The key
   */
  private final K key;

  /**
   * The new property value
   */
  private final V newValue;

  /**
   * The old property value
   */
  private final V oldValue;

  /**
   * True if this change event is coming from the model, false if it is coming from the UI
   */
  private final boolean isModelChange;

  /**
   * True if this property change indicates an initialization, that is, the property did not
   * have a value before this value change
   */
  private final boolean initialization;

  /**
   * Instantiates a new PropertyEvent
   * @param source the source of the value change
   * @param valueOwner the map
   * @param key the key associated with the value
   * @param newValue the new value
   * @param oldValue the old value
   * @param isModelChange true if the value change originates from the model, false if it originates in the UI
   * @param initialization true if the value was being initialized
   */
  public ValueChangeEvent(final Object source, final ValueChangeMap<K, V> valueOwner, final K key, final V newValue,
                          final V oldValue, final boolean isModelChange, final boolean initialization) {
    super(source, 0, key.toString());
    this.valueOwner = valueOwner;
    this.key = key;
    this.newValue = newValue;
    this.oldValue = oldValue;
    this.isModelChange = isModelChange;
    this.initialization = initialization;
  }

  /**
   * @return the object owning the value
   */
  public ValueChangeMap<K, V> getValueOwner() {
    return valueOwner;
  }

  /**
   * @return the key of the associated with the changed value
   */
  public K getKey() {
    return key;
  }

  /**
   * @return the old value
   */
  public Object getOldValue() {
    return oldValue;
  }

  /**
   * @return the new value
   */
  public Object getNewValue() {
    return newValue;
  }

  /**
   * @return true if the new value is null
   */
  public boolean isNewValueNull() {
    return newValue == null;
  }

  /**
   * Returns true if the new value is equal to the given value
   * @param value the value
   * @return true if the given value is the new value
   */
  public boolean isNewValueEqual(final Object value) {
    return Util.equal(newValue, value);
  }

  /**
   * Returns true if the old value is equal to the given value
   * @param value the value
   * @return true if the given value is the old value
   */
  public boolean isOldValueEqual(final Object value) {
    return Util.equal(oldValue, value);
  }

  /**
   * @return true if the old value is null
   */
  public boolean isOldValueNull() {
    return oldValue == null;
  }

  /**
   * @return true if this property change is coming from the model,
   * false if it is coming from the UI
   */
  public boolean isModelChange() {
    return isModelChange;
  }

  /**
   * @return true if this property change is coming from the UI,
   * false if it is coming from the model
   */
  public boolean isUIChange() {
    return !isModelChange;
  }

  /**
   * @return true if this key had no associated value prior to this value change
   */
  public boolean isInitialization() {
    return initialization;
  }
}
