/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import org.jminor.common.model.Event;
import org.jminor.common.model.EventInfoListener;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Events;
import org.jminor.common.model.State;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.States;
import org.jminor.common.model.Util;
import org.jminor.common.model.Value;
import org.jminor.common.model.valuemap.exception.ValidationException;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A default ValueMapEditModel implementation, handling value change events and validation
 * @param <K> the type of the value map keys
 * @param <V> the type of values in this map
 */
public class DefaultValueMapEditModel<K, V> implements ValueMapEditModel<K, V> {

  private static final String KEY = "key";

  /**
   * The ValueMap being edited by this model
   */
  private final ValueMap<K, V> valueMap;

  /**
   * The validator used by this edit model
   */
  private final ValueMap.Validator validator;

  /**
   * A state indicating whether or not the entity being edited is in a valid state according the the validator
   */
  private final State validState = States.state();

  /**
   * Holds events signaling value changes made via the ui
   */
  private final Map<K, Event<ValueChange<K, ?>>> valueSetEventMap = new HashMap<>();

  /**
   * Holds events signaling value changes made via the model or ui
   */
  private final Map<K, Event<ValueChange<K, ?>>> valueChangeEventMap = new HashMap<>();

  /**
   * @param valueMap the ValueMap to edit
   * @param validator the validator
   */
  protected DefaultValueMapEditModel(final ValueMap<K, V> valueMap, final ValueMap.Validator validator) {
    this.valueMap = valueMap;
    this.validator = validator;
    bindEvents();
  }

  /** {@inheritDoc} */
  @Override
  public final V getValue(final K key) {
    return valueMap.getValue(key);
  }

  /** {@inheritDoc} */
  @Override
  public final void setValue(final K key, final V value) {
    Util.rejectNullValue(key, KEY);
    final boolean initialization = !valueMap.containsValue(key);
    final Object oldValue = valueMap.getValue(key);
    valueMap.setValue(key, value);
    if (!Util.equal(value, oldValue)) {
      notifyValueChange(key, ValueChanges.valueChange(key, value, oldValue, initialization));
    }
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isNullable(final K key) {
    return validator.isNullable(valueMap, key);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isValueNull(final K key) {
    return valueMap.isValueNull(key);
  }

  /** {@inheritDoc} */
  @Override
  public final StateObserver getValidObserver() {
    return validState.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isValid() {
    return validState.isActive();
  }

  /** {@inheritDoc} */
  @Override
  public final void validate(final K key) throws ValidationException {
    validator.validate(valueMap, key);
  }

  /** {@inheritDoc} */
  @Override
  public final void validate() throws ValidationException {
    validate(valueMap);
  }

  /** {@inheritDoc} */
  @Override
  public final void validate(final ValueMap<K, V> valueMap) throws ValidationException {
    validate(Collections.singletonList(valueMap));
  }

  /** {@inheritDoc} */
  @Override
  public final void validate(final Collection<? extends ValueMap<K, V>> valueMaps) throws ValidationException {
    for (final ValueMap<K, V> valueMapToValidate : valueMaps) {
      validator.validate(valueMapToValidate);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isValid(final K key) {
    Util.rejectNullValue(key, KEY);
    try {
      validator.validate(valueMap, key);
      return true;
    }
    catch (final ValidationException e) {
      return false;
    }
  }

  /** {@inheritDoc} */
  @Override
  public final ValueMap.Validator getValidator() {
    return validator;
  }

  /** {@inheritDoc} */
  @Override
  public final EventObserver<ValueChange<K, ?>> getValueChangeObserver() {
    return valueMap.getValueChangeObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final EventObserver<ValueChange<K, ?>> getValueChangeObserver(final K key) {
    Util.rejectNullValue(key, KEY);
    return getValueChangeEvent(key).getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final void addValueSetListener(final K key, final EventInfoListener<ValueChange<K, ?>> listener) {
    getValueSetEvent(key).addInfoListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeValueSetListener(final K key, final EventInfoListener listener) {
    if (valueSetEventMap.containsKey(key)) {
      valueSetEventMap.get(key).removeInfoListener(listener);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void addValueListener(final K key, final EventInfoListener<ValueChange<K, ?>> listener) {
    getValueChangeObserver(key).addInfoListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeValueListener(final K key, final EventInfoListener listener) {
    if (valueChangeEventMap.containsKey(key)) {
      valueChangeEventMap.get(key).removeInfoListener(listener);
    }
  }

  /**
   * @return the underlying ValueMap object
   */
  protected final ValueMap<K, V> getValueMap() {
    return valueMap;
  }

  /**
   * Notifies that the value associated with the given key has changed using the given event
   * @param key the key
   * @param event the event describing the value change
   */
  private void notifyValueChange(final K key, final ValueChange<K, ?> event) {
    getValueSetEvent(key).fire(event);
  }

  private Event<ValueChange<K, ?>> getValueSetEvent(final K key) {
    if (!valueSetEventMap.containsKey(key)) {
      valueSetEventMap.put(key, Events.<ValueChange<K, ?>>event());
    }

    return valueSetEventMap.get(key);
  }

  private Event<ValueChange<K, ?>> getValueChangeEvent(final K key) {
    if (!valueChangeEventMap.containsKey(key)) {
      valueChangeEventMap.put(key, Events.<ValueChange<K, ?>>event());
    }

    return valueChangeEventMap.get(key);
  }

  private void bindEvents() {
    valueMap.addValueListener(new EventInfoListener<ValueChange<K, ?>>() {
      @Override
      public void eventOccurred(final ValueChange<K, ?> valueChange) {
        validState.setActive(validator.isValid(valueMap));
        final Event<ValueChange<K, ?>> valueChangeEvent = valueChangeEventMap.get(valueChange.getKey());
        if (valueChangeEvent != null) {
          valueChangeEvent.fire(valueChange);
        }
      }
    });
  }
}
