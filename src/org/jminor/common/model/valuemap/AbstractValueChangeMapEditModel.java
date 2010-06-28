/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import org.jminor.common.model.Event;
import org.jminor.common.model.State;
import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.exception.ValidationException;

import java.util.HashMap;
import java.util.Map;

/**
 * A class which facilitates the editing of the contents of a ValueChangeMap instance by providing
 * a validation mechanism as well as value change events.
 * @param <K> the type of the keys in the value map
 * @param <V> the type of the values in the value map
 */
public abstract class AbstractValueChangeMapEditModel<K, V> implements ValueChangeMapEditModel<K, V> {

  /**
   * The value map instance edited by this edit model.
   */
  private final ValueChangeMap<K, V> valueMap;

  /**
   * Fired when the active value map is set.
   * @see #setValueMap(ValueMap)
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
  public AbstractValueChangeMapEditModel(final ValueChangeMap<K, V> initialMap) {
    Util.rejectNullValue(initialMap);
    this.valueMap = initialMap;
    bindEventsInternal();
  }

  public V getValue(final K key) {
    return valueMap.getValue(key);
  }

  public void setValue(final K key, final V value) {
    final boolean initialization = valueMap.containsValue(key);
    final V oldValue = getValue(key);
    final V newValue = doSetValue(key, value);

    if (!Util.equal(newValue, oldValue)) {
      notifyValueSet(key, new ValueChangeEvent<K, V>(this, valueMap, key, newValue, oldValue, false, initialization));
    }
  }

  public boolean isValueNull(final K key) {
    return valueMap.isValueNull(key);
  }

  public void setValueMap(final ValueMap<K, V> valueMap) {
    this.valueMap.setAs(valueMap == null ? getDefaultValueMap() : valueMap);
    evtValueMapSet.fire();
  }

  public boolean isValid(final K key, final int action) {
    try {
      validate(key, action);
      return true;
    }
    catch (ValidationException e) {
      return false;
    }
  }

  public Event getValueSetEvent(final K key) {
    Util.rejectNullValue(key);
    if (!valueSetEventMap.containsKey(key)) {
      valueSetEventMap.put(key, new Event());
    }

    return valueSetEventMap.get(key);
  }

  public Event getValueChangeEvent(final K key) {
    Util.rejectNullValue(key);
    if (!valueChangeEventMap.containsKey(key)) {
      valueChangeEventMap.put(key, new Event());
    }

    return valueChangeEventMap.get(key);
  }

  public Event eventValueMapSet() {
    return evtValueMapSet;
  }

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
