/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import org.jminor.common.model.Event;
import org.jminor.common.model.EventAdapter;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Events;
import org.jminor.common.model.State;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.States;
import org.jminor.common.model.Util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A default ValueMap implementation.
 * @param <K> the key type
 * @param <V> the value type
 */
public class ValueMapImpl<K, V> implements ValueMap<K, V> {

  /**
   * Holds the values contained in this value map.
   */
  private final Map<K, V> values = new HashMap<K, V>();

  /**
   * Holds the original value for keys which values have changed.
   */
  private Map<K, V> originalValues = null;

  /**
   * Fired when a value changes, null until initialized by a call to getValueChangedEvent().
   */
  private Event evtValueChanged;

  private static final int MAGIC_NUMBER = 23;
  private static final String EMPTY_STRING = "";

  public ValueMapImpl() {}

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
      return previousValue;
    }
    if (!initialization) {
      updateModifiedState(key, value, previousValue);
    }
    notifyValueChange(key, value, previousValue, initialization);
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
      return EMPTY_STRING;
    }

    return value.toString();
  }

  /** {@inheritDoc} */
  @Override
  public V copyValue(final V value) {
    return value;
  }

  /**
   * Two ValueMapImpl objects are equal if they contain the
   * same number of values and all their values are equal.
   */
  @SuppressWarnings({"unchecked"})
  @Override
  public boolean equals(final Object obj) {
    if (!(obj instanceof ValueMapImpl)) {
      return false;
    }

    final ValueMapImpl<K, V> otherMap = (ValueMapImpl<K, V>) obj;
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
      if (evtValueChanged != null) {
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
  @SuppressWarnings({"unchecked"})
  public final Collection<K> getOriginalValueKeys() {
    if (originalValues == null) {
      return (Collection<K>) Collections.EMPTY_LIST;
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
    return originalValues != null && !originalValues.isEmpty();
  }

  /** {@inheritDoc} */
  @Override
  public ValueMap<K, V> getInstance() {
    return new ValueMapImpl<K, V>();
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
    clear();
    if (sourceMap != null) {
      for (final K entryKey : sourceMap.getValueKeys()) {
        final V value = copyValue(sourceMap.getValue(entryKey));
        setValue(entryKey, value);
      }
      if (sourceMap.isModified()) {
        if (originalValues == null) {
          originalValues = new HashMap<K, V>();
        }
        for (final K entryKey : sourceMap.getOriginalValueKeys()) {
          originalValues.put(entryKey, copyValue(sourceMap.getOriginalValue(entryKey)));
        }
      }
    }
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
  public final void addValueListener(final ValueChangeListener<K, V> valueListener) {
    getValueChangeObserver().addListener(valueListener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeValueListener(final ValueChangeListener valueListener) {
    if (evtValueChanged != null) {
      evtValueChanged.removeListener(valueListener);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final StateObserver getModifiedState() {
    final State state = States.state(isModified());
    getValueChangeObserver().addListener(new EventAdapter() {
      /** {@inheritDoc} */
      @Override
      public void eventOccurred() {
        state.setActive(isModified());
      }
    });

    return state.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final EventObserver getValueChangeObserver() {
    return getValueChangedEvent().getObserver();
  }

  protected final void notifyValueChange(final K key, final V value, final V oldValue, final boolean initialization) {
    if (evtValueChanged != null) {
      evtValueChanged.fire(new ValueChangeEvent<K, V>(this, key, value, oldValue, initialization));
    }
  }

  protected final void setOriginalValue(final K key, final V oldValue) {
    if (originalValues == null) {
      originalValues = new HashMap<K, V>();
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
   * Called after a value has been set.
   * @param key the key
   * @param value the value
   * @param previousValue the previous value
   * @param initialization true if the value was being initialized
   */
  protected void handleValueSet(final K key, final V value, final V previousValue, final boolean initialization) {}

  /**
   * Called after a value has been removed from this map.
   * @param key the key
   * @param value the value that was removed
   */
  protected void handleValueRemoved(final K key, final V value) {}

  /**
   * Called after the value map has been cleared.
   */
  protected void handleClear() {}

  /**
   * Called after the valueChangeEvent has been initialized, via the first call to {@link #getValueChangedEvent()}
   */
  protected void handleValueChangedEventInitialized() {}

  private void updateModifiedState(final K key, final V value, final V previousValue) {
    final boolean modified = isModified(key);
    if (modified && Util.equal(getOriginalValue(key), value)) {
      removeOriginalValue(key);//we're back to the original value
    }
    else if (!modified) {
      setOriginalValue(key, previousValue);
    }
  }

  private Event getValueChangedEvent() {
    if (evtValueChanged == null) {
      evtValueChanged = Events.event();
      handleValueChangedEventInitialized();
    }

    return evtValueChanged;
  }
}
