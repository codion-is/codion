/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import org.jminor.common.model.Event;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Events;
import org.jminor.common.model.State;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.States;
import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.exception.ValidationException;

import java.awt.event.ActionListener;
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
   * @see #setValueMap(ValueChangeMap)
   */
  private final Event evtValueMapSet = Events.event();

  /**
   * Holds events signaling value changes made via the ui
   */
  private final Map<K, Event> valueSetEventMap = new HashMap<K, Event>();

  /**
   * Holds events signaling value changes made via the model or ui
   */
  private final Map<K, Event> valueChangeEventMap = new HashMap<K, Event>();

  /**
   * The validator used by this edit model
   */
  private final ValueMapValidator<K, V> validator;

  /**
   * A state indicating whether or not the value map being edited is in a valid state
   * according the the validator
   */
  private final State stValid = States.state();

  /**
   * Instantiates a new edit model instance for the given value map.
   * @param initialMap the value map to edit
   * @param validator the validator
   */
  public AbstractValueChangeMapEditModel(final ValueChangeMap<K, V> initialMap, final ValueMapValidator<K, V> validator) {
    Util.rejectNullValue(initialMap, "initialMap");
    this.valueMap = initialMap;
    this.validator = validator;
    bindEventsInternal();
  }

  /** {@inheritDoc} */
  public void clear() {}

  /** {@inheritDoc} */
  public void refresh() {}

  /** {@inheritDoc} */
  public final ValueMapValidator<K, V> getValidator() {
    return validator;
  }

  /** {@inheritDoc} */
  public final boolean isNullable(final K key) {
    return validator.isNullable(valueMap, key);
  }

  /** {@inheritDoc} */
  public final void validate(final K key, final int action) throws ValidationException {
    validator.validate(valueMap, key, action);
  }

  /** {@inheritDoc} */
  public final V getValue(final K key) {
    return valueMap.getValue(key);
  }

  /** {@inheritDoc} */
  public final void setValue(final K key, final V value) {
    Util.rejectNullValue(key, "key");
    final boolean initialization = !valueMap.containsValue(key);
    final V oldValue = valueMap.getValue(key);
    valueMap.setValue(key, prepareNewValue(key, value));
    if (!Util.equal(value, oldValue)) {
      notifyValueSet(key, new ValueChangeEvent<K, V>(this, valueMap, key, value, oldValue, false, initialization));
    }
  }

  /** {@inheritDoc} */
  public final boolean isValueNull(final K key) {
    return valueMap.isValueNull(key);
  }

  /** {@inheritDoc} */
  public final void setValueMap(final ValueChangeMap<K, V> valueMap) {
    this.valueMap.setAs(valueMap == null ? getDefaultValueMap() : valueMap);
    evtValueMapSet.fire();
  }

  /** {@inheritDoc} */
  public final boolean isValid(final K key, final int action) {
    Util.rejectNullValue(key, "key");
    try {
      validator.validate(valueMap, key, action);
      return true;
    }
    catch (ValidationException e) {
      return false;
    }
  }

  /** {@inheritDoc} */
  public final boolean isModified() {
    return getModifiedState().isActive();
  }

  /** {@inheritDoc} */
  public final boolean isValid() {
    return getValidState().isActive();
  }

  /** {@inheritDoc} */
  public final void addValueSetListener(final K key, final ActionListener listener) {
    getValueSetEvent(key).addListener(listener);
  }

  /** {@inheritDoc} */
  public final void removeValueSetListener(final K key, final ActionListener listener) {
    getValueSetEvent(key).removeListener(listener);
  }

  /** {@inheritDoc} */
  public final EventObserver getValueChangeObserver(final K key) {
    Util.rejectNullValue(key, "key");
    if (!valueChangeEventMap.containsKey(key)) {
      valueChangeEventMap.put(key, Events.event());
    }

    return valueChangeEventMap.get(key).getObserver();
  }

  /** {@inheritDoc} */
  public final void addValueListener(final K key, final ActionListener listener) {
    getValueChangeObserver(key).addListener(listener);
  }

  /** {@inheritDoc} */
  public final void removeValueListener(final K key, final ActionListener listener) {
    getValueChangeEvent(key).removeListener(listener);
  }

  /** {@inheritDoc} */
  public final void addValueMapSetListener(final ActionListener listener) {
    evtValueMapSet.addListener(listener);
  }

  /** {@inheritDoc} */
  public final void removeValueMapSetListener(final ActionListener listener) {
    evtValueMapSet.removeListener(listener);
  }

  /** {@inheritDoc} */
  public final StateObserver getModifiedState() {
    return valueMap.getModifiedState();
  }

  /** {@inheritDoc} */
  public final StateObserver getValidState() {
    return stValid.getObserver();
  }

  /**
   * Provides a hook into the value setting mechanism, override to
   * translate or otherwise manipulate the value being set
   * @param key the key
   * @param value the value
   * @return the prepared value
   */
  protected V prepareNewValue(final K key, final V value) {
    return value;
  }

  /**
   * @return the value map instance being edited
   */
  protected final ValueChangeMap<K, V> getValueMap() {
    return valueMap;
  }

  /**
   * Notifies that the value associated with the given key has changed using the given event
   * @param key the key
   * @param event the event describing the value change
   */
  private void notifyValueSet(final K key, final ValueChangeEvent event) {
    getValueSetEvent(key).fire(event);
  }

  private Event getValueSetEvent(final K key) {
    if (!valueSetEventMap.containsKey(key)) {
      valueSetEventMap.put(key, Events.event());
    }

    return valueSetEventMap.get(key);
  }

  private Event getValueChangeEvent(final K key) {
    if (!valueChangeEventMap.containsKey(key)) {
      valueChangeEventMap.put(key, Events.event());
    }

    return valueChangeEventMap.get(key);
  }

  private void bindEventsInternal() {
    valueMap.addValueListener(new ValueChangeListener<K, V>() {
      @Override
      protected void valueChanged(final ValueChangeEvent<K, V> event) {
        final Event valueChangeEvent = valueChangeEventMap.get(event.getKey());
        if (valueChangeEvent != null) {
          valueChangeEvent.fire(event);
        }
        stValid.setActive(validator.isValid(valueMap, ValueMapValidator.UNKNOWN));
      }
    });
  }
}
