/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.valuemap;

import org.jminor.common.Util;
import org.jminor.common.event.Event;
import org.jminor.common.event.EventDataListener;
import org.jminor.common.event.EventObserver;
import org.jminor.common.event.Events;
import org.jminor.common.state.State;
import org.jminor.common.state.StateObserver;
import org.jminor.common.state.States;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * A default ValueMap implementation.
 * Note that this class is not thread safe.
 * @param <K> the key type
 * @param <V> the value type
 */
public class DefaultValueMap<K, V> implements ValueMap<K, V> {

  /**
   * Holds the values contained in this value map.
   */
  private final Map<K, V> values;

  /**
   * Holds the original value for keys which values have changed since they were first set.
   */
  private Map<K, V> originalValues;

  /**
   * Fired when a value changes, null until initialized by a call to getValueChangedEvent().
   */
  private Event<ValueChange<K, V>> valueChangedEvent;

  private static final int MAGIC_NUMBER = 23;

  /**
   * Instantiates a new empty instance
   */
  public DefaultValueMap() {
    this(new HashMap<>(), null);
  }

  /**
   * Instantiates a new instance using the given maps for the values and original values respectively.
   * Note that the given map instances are used internally, modifying the contents of those maps outside this
   * DefaultValueMap instance will result in undefined behaviour, to put things politely.
   * @param values the values
   * @param originalValues the originalValues
   */
  protected DefaultValueMap(final Map<K, V> values, final Map<K, V> originalValues) {
    this.values = values == null ? new HashMap<>() : values;
    this.originalValues = originalValues;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isNull(final K key) {
    return get(key) == null;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isNotNull(final K key) {
    return !isNull(key);
  }

  /** {@inheritDoc} */
  @Override
  public final V put(final K key, final V value) {
    requireNonNull(key, "key");
    final V newValue = validateAndPrepare(key, value);
    final boolean initialization = !values.containsKey(key);
    final V previousValue = values.put(key, newValue);
    if (!initialization && Objects.equals(previousValue, newValue)) {
      return newValue;
    }
    if (!initialization) {
      updateOriginalValue(key, newValue, previousValue);
    }
    handlePut(key, newValue, previousValue, initialization);
    if (valueChangedEvent != null) {
      notifyValueChange(key, newValue, previousValue, initialization);
    }

    return previousValue;
  }

  /** {@inheritDoc} */
  @Override
  public V get(final K key) {
    return values.get(key);
  }

  /** {@inheritDoc} */
  @Override
  public String getAsString(final K key) {
    final V value = values.get(key);
    if (value == null) {
      return "";
    }

    return value.toString();
  }

  /**
   * Two DefaultValueMap objects are equal if they contain the
   * same number of values and all their values are equal.
   */
  @Override
  public boolean equals(final Object obj) {
    if (!(obj instanceof DefaultValueMap)) {
      return false;
    }

    final DefaultValueMap<K, V> otherMap = (DefaultValueMap<K, V>) obj;
    if (size() != otherMap.size()) {
      return false;
    }

    return otherMap.keySet().stream().noneMatch(key -> !containsKey(key) || !Objects.equals(otherMap.get(key), get(key)));
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return MAGIC_NUMBER + values().stream().filter(Objects::nonNull).mapToInt(Object::hashCode).sum();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean containsKey(final K key) {
    return values.containsKey(key);
  }

  /** {@inheritDoc} */
  @Override
  public final V remove(final K key) {
    if (values.containsKey(key)) {
      final V value = values.remove(key);
      removeOriginalValue(key);
      handleRemove(key, value);
      if (valueChangedEvent != null) {
        notifyValueChange(key, null, value, false);
      }

      return value;
    }

    return null;
  }

  /** {@inheritDoc} */
  @Override
  public final void clear() {
    values.clear();
    if (originalValues != null) {
      originalValues = null;
    }
    handleClear();
  }

  /** {@inheritDoc} */
  @Override
  public final int size() {
    return values.size();
  }

  /** {@inheritDoc} */
  @Override
  public final Set<K> keySet() {
    return Collections.unmodifiableSet(values.keySet());
  }

  /** {@inheritDoc} */
  @Override
  public final Set<K> originalKeySet() {
    if (originalValues == null) {
      return Collections.emptySet();
    }

    return Collections.unmodifiableSet(originalValues.keySet());
  }

  /** {@inheritDoc} */
  @Override
  public final Collection<V> values() {
    return Collections.unmodifiableCollection(values.values());
  }

  /** {@inheritDoc} */
  @Override
  public final V getOriginal(final K key) {
    if (isModified(key)) {
      return originalValues.get(key);
    }

    return get(key);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isModified() {
    return !Util.nullOrEmpty(originalValues);
  }

  /** {@inheritDoc} */
  @Override
  public final void setAs(final ValueMap<K, V> sourceMap) {
    if (sourceMap == this) {
      return;
    }
    final Set<K> affectedKeys = new HashSet<>(keySet());
    clear();
    if (sourceMap != null) {
      final Collection<K> sourceValueKeys = sourceMap.keySet();
      affectedKeys.addAll(sourceValueKeys);
      for (final K key : sourceValueKeys) {
        final V value = sourceMap.get(key);
        values.put(key, value);
        handlePut(key, value, null, true);
      }
      if (sourceMap.isModified()) {
        originalValues = new HashMap<>();
        for (final K key : sourceMap.originalKeySet()) {
          originalValues.put(key, sourceMap.getOriginal(key));
        }
      }
    }
    notifyInitialized(affectedKeys);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isModified(final K key) {
    return originalValues != null && originalValues.containsKey(key);
  }

  /** {@inheritDoc} */
  @Override
  public final void revert(final K key) {
    if (isModified(key)) {
      put(key, getOriginal(key));
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void revertAll() {
    for (final K key : keySet()) {
      revert(key);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void save(final K key) {
    removeOriginalValue(key);
  }

  /** {@inheritDoc} */
  @Override
  public final void saveAll() {
    originalValues = null;
  }

  /** {@inheritDoc} */
  @Override
  public final void addValueListener(final EventDataListener<ValueChange<K, V>> valueListener) {
    getValueObserver().addDataListener(valueListener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeValueListener(final EventDataListener valueListener) {
    if (valueChangedEvent != null) {
      valueChangedEvent.removeDataListener(valueListener);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final StateObserver getModifiedObserver() {
    final State state = States.state(isModified());
    getValueObserver().addDataListener(valueChange -> state.set(isModified()));

    return state.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final EventObserver<ValueChange<K, V>> getValueObserver() {
    return getValueChangedEvent().getObserver();
  }

  /**
   * Validates the value for the given key
   * @param key the key
   * @param value the value to validate
   * @return the value
   * @throws IllegalArgumentException in case the value is invalid
   */
  protected V validateAndPrepare(final K key, final V value) {
    return value;
  }

  protected final void notifyValueChange(final K key, final V currentValue, final V previousValue, final boolean initialization) {
    valueChangedEvent.fire(ValueChanges.valueChange(key, currentValue, previousValue, initialization));
  }

  protected final void setOriginalValue(final K key, final V previousValue) {
    if (originalValues == null) {
      originalValues = new HashMap<>();
    }
    originalValues.put(key, previousValue);
  }

  protected final void removeOriginalValue(final K key) {
    if (originalValues != null) {
      originalValues.remove(key);
      if (originalValues.isEmpty()) {
        originalValues = null;
      }
    }
  }

  /**
   * Called after a value has been put. This base implementation does nothing.
   * @param key the key
   * @param value the value
   * @param previousValue the previous value
   * @param initialization true if the value was being initialized
   */
  protected void handlePut(final K key, final V value, final V previousValue, final boolean initialization) {/*Provided for subclasses*/}

  /**
   * Called after a value has been removed from this map. This base implementation does nothing.
   * @param key the key
   * @param value the value that was removed
   */
  protected void handleRemove(final K key, final V value) {/*Provided for subclasses*/}

  /**
   * Called after the value map has been cleared. This base implementation does nothing.
   */
  protected void handleClear() {/*Provided for subclasses*/}

  /**
   * Called after the valueChangeEvent has been initialized, via the first call to {@link #getValueChangedEvent()}
   */
  protected void handleValueChangedEventInitialized() {/*Provided for subclasses*/}

  private void updateOriginalValue(final K key, final V value, final V previousValue) {
    final boolean modified = isModified(key);
    if (modified && Objects.equals(getOriginal(key), value)) {
      removeOriginalValue(key);//we're back to the original value
    }
    else if (!modified) {//only the first original value is kept
      setOriginalValue(key, previousValue);
    }
  }

  private void notifyInitialized(final Set<K> valueKeys) {
    if (valueChangedEvent != null) {
      for (final K key : valueKeys) {
        final V value = values.get(key);
        valueChangedEvent.fire(ValueChanges.valueChange(key, value, null, true));
      }
    }
  }

  private Event<ValueChange<K, V>> getValueChangedEvent() {
    if (valueChangedEvent == null) {
      valueChangedEvent = Events.event();
      handleValueChangedEventInitialized();
    }

    return valueChangedEvent;
  }
}
