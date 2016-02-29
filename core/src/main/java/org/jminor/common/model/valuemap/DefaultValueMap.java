/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A default ValueMap implementation.
 * @param <K> the key type
 * @param <V> the value type
 */
public class DefaultValueMap<K, V> implements ValueMap<K, V> {

  /**
   * Holds the values contained in this value map.
   */
  private final Map<K, V> values = new HashMap<>();

  /**
   * Holds the original value for keys which values have changed since they were first set.
   */
  private Map<K, V> originalValues = null;

  /**
   * Fired when a value changes, null until initialized by a call to getValueChangedEvent().
   */
  private Event<ValueChange<K, ?>> valueChangedEvent;

  private static final int MAGIC_NUMBER = 23;

  /** {@inheritDoc} */
  @Override
  public boolean isValueNull(final K key) {
    return getValue(key) == null;
  }

  /** {@inheritDoc} */
  @Override
  public V setValue(final K key, final V value) {
    final boolean initialization = !values.containsKey(key);
    final V previousValue = values.put(key, value);
    if (!initialization && Util.equal(previousValue, value)) {
      return value;
    }
    if (!initialization) {
      updateOriginalValue(key, value, previousValue);
    }
    if (valueChangedEvent != null) {
      notifyValueChange(key, value, previousValue, initialization);
    }
    handleValueSet(key, value, previousValue, initialization);

    return previousValue;
  }

  /** {@inheritDoc} */
  @Override
  public V getValue(final K key) {
    return values.get(key);
  }

  /** {@inheritDoc} */
  @Override
  public String getValueAsString(final K key) {
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
  @SuppressWarnings({"unchecked"})
  @Override
  public boolean equals(final Object obj) {
    if (!(obj instanceof DefaultValueMap)) {
      return false;
    }

    final DefaultValueMap<K, V> otherMap = (DefaultValueMap<K, V>) obj;
    if (size() != otherMap.size()) {
      return false;
    }

    for (final K key : otherMap.getValueKeys()) {
      if (!containsValue(key) || !Util.equal(otherMap.getValue(key), getValue(key))) {
        return false;
      }
    }

    return true;
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    int hash = MAGIC_NUMBER;
    for (final Object value : getValues()) {
      if (value != null) {
        hash = hash + value.hashCode();
      }
    }

    return hash;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean containsValue(final K key) {
    return values.containsKey(key);
  }

  /** {@inheritDoc} */
  @Override
  public final V removeValue(final K key) {
    if (values.containsKey(key)) {
      final V value = values.remove(key);
      removeOriginalValue(key);
      if (valueChangedEvent != null) {
        notifyValueChange(key, null, value, false);
      }
      handleValueRemoved(key, value);

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
  public final Collection<K> getValueKeys() {
    return Collections.unmodifiableCollection(values.keySet());
  }

  /** {@inheritDoc} */
  @Override
  public final Collection<K> getOriginalValueKeys() {
    if (originalValues == null) {
      return Collections.emptyList();
    }

    return Collections.unmodifiableCollection(originalValues.keySet());
  }

  /** {@inheritDoc} */
  @Override
  public final Collection<V> getValues() {
    return Collections.unmodifiableCollection(values.values());
  }

  /** {@inheritDoc} */
  @Override
  public final V getOriginalValue(final K key) {
    if (isModified(key)) {
      return originalValues.get(key);
    }

    return getValue(key);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isModified() {
    return !Util.nullOrEmpty(originalValues);
  }

  /** {@inheritDoc} */
  @Override
  public ValueMap<K, V> getInstance() {
    return new DefaultValueMap<>();
  }

  /** {@inheritDoc} */
  @Override
  public final ValueMap<K, V> getCopy() {
    final ValueMap<K, V> copy = getInstance();
    copy.setAs(this);

    return copy;
  }

  /** {@inheritDoc} */
  @Override
  public final void setAs(final ValueMap<K, V> sourceMap) {
    if (sourceMap == this) {
      return;
    }
    final Set<K> affectedKeys = new HashSet<>(getValueKeys());
    clear();
    if (sourceMap != null) {
      final Collection<K> sourceValueKeys = sourceMap.getValueKeys();
      affectedKeys.addAll(sourceValueKeys);
      for (final K key : sourceValueKeys) {
        final V value = copyValue(sourceMap.getValue(key));
        values.put(key, value);
        handleValueSet(key, value, null, true);
      }
      if (sourceMap.isModified()) {
        originalValues = new HashMap<>();
        for (final K key : sourceMap.getOriginalValueKeys()) {
          originalValues.put(key, copyValue(sourceMap.getOriginalValue(key)));
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
  public final void revertValue(final K key) {
    if (isModified(key)) {
      setValue(key, getOriginalValue(key));
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void revertAll() {
    for (final K key : getValueKeys()) {
      revertValue(key);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void saveValue(final K key) {
    removeOriginalValue(key);
  }

  /** {@inheritDoc} */
  @Override
  public final void saveAll() {
    for (final K key : getValueKeys()) {
      saveValue(key);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final ValueMap<K, V> getOriginalCopy() {
    final ValueMap<K, V> copy = getCopy();
    copy.revertAll();

    return copy;
  }

  /** {@inheritDoc} */
  @Override
  public final void addValueListener(final EventInfoListener<ValueChange<K, ?>> valueListener) {
    getValueChangeObserver().addInfoListener(valueListener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeValueListener(final EventInfoListener valueListener) {
    if (valueChangedEvent != null) {
      valueChangedEvent.removeInfoListener(valueListener);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final StateObserver getModifiedObserver() {
    final State state = States.state(isModified());
    getValueChangeObserver().addInfoListener(new EventInfoListener<ValueChange<K, ?>>() {
      @Override
      public void eventOccurred(final ValueChange info) {
        state.setActive(isModified());
      }
    });

    return state.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final EventObserver<ValueChange<K, ?>> getValueChangeObserver() {
    return getValueChangedEvent().getObserver();
  }

  protected final void notifyValueChange(final K key, final V value, final V oldValue, final boolean initialization) {
    valueChangedEvent.fire(ValueChanges.valueChange(key, value, oldValue, initialization));
  }

  protected final void setOriginalValue(final K key, final V oldValue) {
    if (originalValues == null) {
      originalValues = new HashMap<>();
    }
    originalValues.put(key, oldValue);
  }

  protected final void removeOriginalValue(final K key) {
    if (originalValues != null && originalValues.containsKey(key)) {
      originalValues.remove(key);
      if (originalValues.isEmpty()) {
        originalValues = null;
      }
    }
  }

  /**
   * Returns a deep copy of the given value, immutable values are simply returned.
   * @param value the value to copy
   * @return a deep copy of the given value, or the same instance in case the value is immutable
   */
  protected V copyValue(final V value) {
    return value;
  }

  /**
   * Called after a value has been set. This base implementation does nothing.
   * @param key the key
   * @param value the value
   * @param previousValue the previous value
   * @param initialization true if the value was being initialized
   */
  protected void handleValueSet(final K key, final V value, final V previousValue, final boolean initialization) {/*Provided for subclasses*/}

  /**
   * Called after a value has been removed from this map. This base implementation does nothing.
   * @param key the key
   * @param value the value that was removed
   */
  protected void handleValueRemoved(final K key, final V value) {/*Provided for subclasses*/}

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
    if (modified && Util.equal(getOriginalValue(key), value)) {
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

  private Event<ValueChange<K, ?>> getValueChangedEvent() {
    if (valueChangedEvent == null) {
      valueChangedEvent = Events.event();
      handleValueChangedEventInitialized();
    }

    return valueChangedEvent;
  }
}
