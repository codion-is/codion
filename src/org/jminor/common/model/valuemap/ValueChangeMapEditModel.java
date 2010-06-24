/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import org.jminor.common.model.Event;
import org.jminor.common.model.Refreshable;
import org.jminor.common.model.State;
import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.exception.ValidationException;

import java.util.HashMap;
import java.util.Map;

/**
 * A class which facilitates the editing of the contents of a ValueChangeMap instance.
 * @param <K> the type of the keys in the value map
 * @param <V> the type of the values in the value map
 */
public abstract class ValueChangeMapEditModel<K, V> implements Refreshable {

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

  /**
   * The value map instance edited by this edit model.
   */
  private final ValueChangeMap<K, V> valueMap;

  /**
   * Fired when the active value map is set.
   * @see #setValueMap(ValueChangeMap)
   */
  private final Event evtValueMapSet = new Event();

  /**
   * Holds events signaling value changes made via the ui
   */
  private final Map<K, Event> valueSetEventMap = new HashMap<K, Event>();

  /**
   * Holds events signaling value changes made via the model or ui
   */
  private final Map<K, Event> valueChangeEventMap = new HashMap<K, Event>();

  /**
   * Instantiates a new edit model instance for the given value map.
   * @param initialMap the value map to edit
   */
  public ValueChangeMapEditModel(final ValueChangeMap<K, V> initialMap) {
    this.valueMap = initialMap;
    bindEventsInternal();
  }

  /**
   * Returns the value associated with the given key in the underlying value map
   * @param key the key of the value to retrieve
   * @return the value associated with the given key
   */
  public V getValue(final K key) {
    return valueMap.getValue(key);
  }

  /**
   * Sets the given value in the underlying value map
   * @param key the key to associate the given value with
   * @param value the value to associate with the given key
   */
  public void setValue(final K key, final V value) {
    final boolean initialization = valueMap.containsValue(key);
    final V oldValue = getValue(key);
    final V newValue = doSetValue(key, value);

    if (!Util.equal(newValue, oldValue)) {
      notifyValueSet(key, new ValueChangeEvent<K, V>(this, valueMap, key, newValue, oldValue, false, initialization));
    }
  }

  /**
   * @param key the key
   * @return true if the value of the given key is null
   */
  public boolean isValueNull(final K key) {
    return valueMap.isValueNull(key);
  }

  /**
   * Sets the active value map, that is, deep copies the value from the source map into the underlying map
   * @param valueMap the map to set as active, if null then the default map value is set as active
   * @see #getDefaultValueMap()
   * @see #eventValueMapSet()
   */
  public void setValueMap(final ValueChangeMap<K, V> valueMap) {
    this.valueMap.setAs(valueMap == null ? getDefaultValueMap() : valueMap);
    evtValueMapSet.fire();
  }

  /**
   * Returns true if the given value is valid for the given key, using the <code>validate</code> method
   * @param key the key
   * @param action describes the action requiring validation,
   * ValueChangeMapEditModel.INSERT, ValueChangeMapEditModel.UPDATE or ValueChangeMapEditModel.UNKNOWN
   * @return true if the value is valid
   * @see #validate(Object, int)
   * @see #validate(ValueChangeMap, Object, int)
   */
  public boolean isValid(final K key, final int action) {
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
  public abstract ValueChangeMap<K, V> getDefaultValueMap();

  /**
   * @param key the key
   * @return true if this value is allowed to be null in the underlying value map
   */
  public abstract boolean isNullable(final K key);

  /**
   * @param valueMap the value map
   * @param key the key
   * @return true if this value is allowed to be null in the given value map
   */
  public abstract boolean isNullable(final ValueChangeMap<K, V> valueMap, final K key);

  /**
   * Checks if the value associated with the give key is valid, throws a ValidationException if not
   * @param key the key
   * @param action describes the action requiring validation,
   * ValueChangeMapEditModel.INSERT, ValueChangeMapEditModel.UPDATE or ValueChangeMapEditModel.UNKNOWN
   * @throws org.jminor.common.model.valuemap.exception.ValidationException if the given value is not valid for the given key
   */
  public abstract void validate(final K key, final int action) throws ValidationException;

  /**
   * Checks if the value associated with the give key is valid, throws a ValidationException if not
   * @param valueMap the value map to validate
   * @param key the key the value is associated with
   * @param action describes the action requiring validation,
   * ValueChangeMapEditModel.INSERT, ValueChangeMapEditModel.UPDATE or ValueChangeMapEditModel.UNKNOWN
   * @throws ValidationException if the given value is not valid for the given key
   */
  public abstract void validate(final ValueChangeMap<K, V> valueMap, final K key, final int action) throws ValidationException;

  /**
   * @param key the key for which to retrieve the event
   * @return an Event object which fires when the value of <code>key</code> is changed via
   * the <code>setValue()</code> methods
   * @see #setValue(Object, Object)
   */
  public Event getValueSetEvent(final K key) {
    if (!valueSetEventMap.containsKey(key)) {
      valueSetEventMap.put(key, new Event());
    }

    return valueSetEventMap.get(key);
  }

  /**
   * @param key the key for which to retrieve the event
   * @return an Event object which fires when the value of <code>key</code> changes
   */
  public Event getValueChangeEvent(final K key) {
    if (!valueChangeEventMap.containsKey(key)) {
      valueChangeEventMap.put(key, new Event());
    }

    return valueChangeEventMap.get(key);
  }

  /**
   * @return an Event fired when the active value map has been changed
   * @see #setValueMap(ValueChangeMap)
   */
  public Event eventValueMapSet() {
    return evtValueMapSet;
  }

  /**
   * @return a State indicating the modified status of this value map
   */
  public State stateModified() {
    return valueMap.stateModified();
  }

  /**
   * Sets the value in the underlying value map
   * @param key the key for which to set the value
   * @param value the value
   * @return the value that was just set
   */
  protected V doSetValue(final K key, final V value) {
    valueMap.setValue(key, value);

    return value;
  }

  /**
   * @return the value map instance being edited
   */
  protected ValueChangeMap<K, V> getValueMap() {
    return valueMap;
  }

  /**
   * Notifies that the value associated with the given key has changed using the given event
   * @param key the key
   * @param event the event describing the value change
   */
  protected void notifyValueSet(final K key, final ValueChangeEvent event) {
    getValueSetEvent(key).fire(event);
  }

  private void bindEventsInternal() {
    valueMap.addValueListener(new ValueChangeListener<K, V>() {
      @Override
      protected void valueChanged(final ValueChangeEvent<K, V> event) {
        final Event valueChangeEvent = valueChangeEventMap.get(event.getKey());
        if (valueChangeEvent != null) {
          valueChangeEvent.fire(event);
        }
      }
    });
  }
}
