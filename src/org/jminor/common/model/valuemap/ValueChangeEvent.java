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
   * The ID of the type of object owning the property
   */
  private final String propertyOwnerTypeID;

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
   * @param source the source of the property value change
   * @param propertyOwnerTypeID the ID of the type of object which owns the property
   * @param key the property ID
   * @param newValue the new value
   * @param oldValue the old value
   * @param isModelChange true if the value change originates from the model, false if it originates in the UI
   * @param initialization true if the property value was being initialized
   */
  public ValueChangeEvent(final Object source, final String propertyOwnerTypeID, final K key, final V newValue,
                          final V oldValue, final boolean isModelChange, final boolean initialization) {
    super(source, 0, key.toString());
    this.propertyOwnerTypeID = propertyOwnerTypeID;
    this.key = key;
    this.newValue = newValue;
    this.oldValue = oldValue;
    this.isModelChange = isModelChange;
    this.initialization = initialization;
  }

  /**
   * @return the ID of the type of object owning the property
   */
  public String getPropertyOwnerTypeID() {
    return propertyOwnerTypeID;
  }

  /**
   * @return the property which value just changed
   */
  public K getKey() {
    return key;
  }

  /**
   * @return the property's old value
   */
  public Object getOldValue() {
    return oldValue;
  }

  /**
   * @return the property's new value
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
   * @return true if this property did not have a value prior to this value change
   */
  public boolean isInitialization() {
    return initialization;
  }
}
