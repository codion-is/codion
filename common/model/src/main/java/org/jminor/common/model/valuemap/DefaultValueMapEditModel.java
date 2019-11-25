/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import org.jminor.common.db.valuemap.ValueChange;
import org.jminor.common.db.valuemap.ValueMap;
import org.jminor.common.db.valuemap.exception.ValidationException;
import org.jminor.common.event.Event;
import org.jminor.common.event.EventDataListener;
import org.jminor.common.event.EventObserver;
import org.jminor.common.event.Events;
import org.jminor.common.state.State;
import org.jminor.common.state.StateObserver;
import org.jminor.common.state.States;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.jminor.common.db.valuemap.ValueChanges.valueChange;

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
  private final Map<K, Event<ValueChange<K, V>>> valueSetEventMap = new HashMap<>();

  /**
   * Holds events signaling value changes made via the model or ui
   */
  private final Map<K, Event<ValueChange<K, V>>> valueChangeEventMap = new HashMap<>();

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
  public final V get(final K key) {
    return valueMap.get(key);
  }

  /** {@inheritDoc} */
  @Override
  public final void put(final K key, final V value) {
    requireNonNull(key, KEY);
    final boolean initialization = !valueMap.containsKey(key);
    final V previousValue = valueMap.put(key, value);
    if (!Objects.equals(value, previousValue)) {
      notifyValueChange(key, valueChange(key, value, previousValue, initialization));
    }
  }

  /** {@inheritDoc} */
  @Override
  public final V remove(final K key) {
    requireNonNull(key, KEY);
    V value = null;
    if (valueMap.containsKey(key)) {
      value = valueMap.remove(key);
      notifyValueChange(key, valueChange(key, null, value));
    }

    return value;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isNullable(final K key) {
    return validator.isNullable(valueMap, key);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isNull(final K key) {
    return valueMap.isNull(key);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isNotNull(final K key) {
    return !isNull(key);
  }

  /** {@inheritDoc} */
  @Override
  public final StateObserver getValidObserver() {
    return validState.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isValid() {
    return validState.get();
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
    validate(singletonList(valueMap));
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
    try {
      validator.validate(valueMap, requireNonNull(key, KEY));
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
  public final EventObserver<ValueChange<K, V>> getValueObserver() {
    return valueMap.getValueObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final EventObserver<ValueChange<K, V>> getValueObserver(final K key) {
    return getValueChangeEvent(requireNonNull(key, KEY)).getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final void addValueSetListener(final K key, final EventDataListener<ValueChange<K, V>> listener) {
    getValueSetEvent(key).addDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeValueSetListener(final K key, final EventDataListener listener) {
    if (valueSetEventMap.containsKey(key)) {
      valueSetEventMap.get(key).removeDataListener(listener);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void addValueListener(final K key, final EventDataListener<ValueChange<K, V>> listener) {
    getValueObserver(key).addDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeValueListener(final K key, final EventDataListener listener) {
    if (valueChangeEventMap.containsKey(key)) {
      valueChangeEventMap.get(key).removeDataListener(listener);
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
  private void notifyValueChange(final K key, final ValueChange<K, V> event) {
    getValueSetEvent(key).fire(event);
  }

  private Event<ValueChange<K, V>> getValueSetEvent(final K key) {
    return valueSetEventMap.computeIfAbsent(key, k -> Events.event());
  }

  private Event<ValueChange<K, V>> getValueChangeEvent(final K key) {
    return valueChangeEventMap.computeIfAbsent(key, k -> Events.event());
  }

  private void bindEvents() {
    valueMap.addValueListener(valueChange -> {
      validState.set(validator.isValid(valueMap));
      final Event<ValueChange<K, V>> valueChangeEvent = valueChangeEventMap.get(valueChange.getKey());
      if (valueChangeEvent != null) {
        valueChangeEvent.fire(valueChange);
      }
    });
  }
}
