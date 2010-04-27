/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import org.jminor.common.model.Event;
import org.jminor.common.model.Refreshable;
import org.jminor.common.model.State;
import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.exception.ValidationException;

import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * A class which facilitates the editing the contents a a ValueChangeMap instance.
 * @param <T> the type of the keys in the value map
 * @param <V> the type of the values in the value map
 */
public abstract class ValueChangeMapEditModel<T, V> implements Refreshable {

  /**
   * Code for the insert action, used during validation
   */
  public static final int INSERT = 1;

  /**
   * Code for the update action, used during validation
   */
  public static final int UPDATE = 2;

  /**
   * Code for an unknown action, used during validation
   */
  public static final int UNKNOWN = 3;

  private final ValueChangeMap<T, V> valueMap;
  private final Event evtValueMapChanged = new Event();
  private final Event evtModelCleared = new Event();

  /**
   * Holds events signaling value changes made via the ui
   */
  private final Map<T, Event> valueSetEventMap = new HashMap<T, Event>();

  /**
   * Holds events signaling value changes made via the model or ui
   */
  private final Map<T, Event> valueChangeEventMap = new HashMap<T, Event>();

  public ValueChangeMapEditModel(final ValueChangeMap<T, V> initialMap) {
    this.valueMap = initialMap;
    bindEventsInternal();
  }

  public V getValue(final T key) {
    return valueMap.getValue(key);
  }

  public void setValue(final T key, final V value) {
    final boolean initialization = valueMap.containsValue(key);
    final V oldValue = getValue(key);
    final V newValue = doSetValue(key, value);

    if (!Util.equal(newValue, oldValue))
      notifyPropertyValueSet(key, new ValueChangeEvent<T, V>(this, getValueMap().getMapTypeID(), key, newValue, oldValue, true, initialization));
  }

  public void clear() {
    setValueMap(null);
    evtModelCleared.fire();
  }

  /**
   * @param key the key
   * @return true if the value of the given key is null
   */
  public boolean isValueNull(final T key) {
    return valueMap.isValueNull(key);
  }

  /**
   * Sets the active value map, that is, the map to be edited
   * @param valueMap the map to set as active, if null then the default map value is set as active
   * @see #evtValueMapChanged
   * @see #eventValueMapChanged()
   */
  public void setValueMap(final ValueChangeMap<T, V> valueMap) {
    this.valueMap.setAs(valueMap == null ? getDefaultValueMap() : valueMap);
    evtValueMapChanged.fire();
  }

  /**
   * Returns true if the given value is valid for the given property, using the <code>validate</code> method
   * @param key the key
   * @param action describes the action requiring validation,
   * ValueChangeMapEditModel.INSERT, ValueChangeMapEditModel.UPDATE or ValueChangeMapEditModel.UNKNOWN
   * @return true if the value is valid
   * @see #validate(Object, int)
   * @see #validate(ValueChangeMap , String, int)
   */
  public boolean isValid(final T key, final int action) {
    try {
      validate(key, action);
      return true;
    }
    catch (ValidationException e) {
      return false;
    }
  }

  /**
   * @return a value map containing the default values
   */
  public abstract ValueChangeMap<T, V> getDefaultValueMap();

  /**
   * @param key the key
   * @param value the value
   * @return true it the value is or represents null
   */
  public abstract boolean isNull(final T key, final V value);

  /**
   * @param key the key
   * @return true if this value is allowed to be null
   */
  public abstract boolean isNullable(final T key);

  /**
   * Checks if the value of the given property is valid, throws a ValidationException if not,
   * this default implementation performs a null value validation if the corresponding configuration parameter is set
   * @param key the key
   * @param action describes the action requiring validation,
   * ValueChangeMapEditModel.INSERT, ValueChangeMapEditModel.UPDATE or ValueChangeMapEditModel.UNKNOWN
   * @throws org.jminor.common.model.valuemap.exception.ValidationException if the given value is not valid for the given property
   * @see org.jminor.framework.domain.Property#setNullable(boolean)
   * @see org.jminor.framework.Configuration#PERFORM_NULL_VALIDATION
   */
  public abstract void validate(final T key, final int action) throws ValidationException;

  /**
   * Checks if the value of the given property is valid, throws a ValidationException if not,
   * this default implementation performs a null value validation if the corresponding configuration parameter is set
   * @param valueMap the value map to validate
   * @param propertyID the propertyID
   * @param action describes the action requiring validation,
   * ValueChangeMapEditModel.INSERT, ValueChangeMapEditModel.UPDATE or ValueChangeMapEditModel.UNKNOWN
   * @throws ValidationException if the given value is not valid for the given property
   * @see org.jminor.framework.domain.Property#setNullable(boolean)
   * @see org.jminor.framework.Configuration#PERFORM_NULL_VALIDATION
   */
  public abstract void validate(final ValueChangeMap<T, V> valueMap, final String propertyID, final int action) throws ValidationException;

  /**
   * @param key the key for which to retrieve the event
   * @return an Event object which fires when the value of <code>key</code> is changed via
   * the <code>setValue()</code> methods
   * @see #setValue(Object, Object)
   */
  public Event getPropertyValueSetEvent(final T key) {
    if (!valueSetEventMap.containsKey(key))
      valueSetEventMap.put(key, new Event());

    return valueSetEventMap.get(key);
  }

  /**
   * @param key the key for which to retrieve the event
   * @return an Event object which fires when the value of <code>key</code> changes
   */
  public Event getPropertyChangeEvent(final T key) {
    if (!valueChangeEventMap.containsKey(key))
      valueChangeEventMap.put(key, new Event());

    return valueChangeEventMap.get(key);
  }

  /**
   * @return an Event fired when the active value map has been changed
   */
  public Event eventValueMapChanged() {
    return evtValueMapChanged;
  }

  /**
   * @return an Event fired when the model has been cleared
   */
  public Event eventModelCleared() {
    return evtModelCleared;
  }

  public State stateModified() {
    return valueMap.stateModified();
  }

  private void notifyPropertyValueSet(final T key, final ActionEvent event) {
    getPropertyValueSetEvent(key).fire(event);
  }

  private void bindEventsInternal() {
    valueMap.addValueListener(new ValueChangeListener<T, V>() {
      @Override
      protected void valueChanged(final ValueChangeEvent<T, V> event) {
        final Event propertyEvent = valueChangeEventMap.get(event.getKey());
        if (propertyEvent != null)
          propertyEvent.fire(event);
      }
    });
  }

  /**
   * Sets the value in the underlying value map
   * @param key the key for which to set the value
   * @param value the value
   * @return the value that was just set
   */
  protected V doSetValue(final T key, final V value) {
    valueMap.setValue(key, value);

    return value;
  }

  protected ValueChangeMap<T, V> getValueMap() {
    return valueMap;
  }
}
