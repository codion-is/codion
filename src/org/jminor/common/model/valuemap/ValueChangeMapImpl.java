/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import org.jminor.common.model.Event;
import org.jminor.common.model.State;
import org.jminor.common.model.Util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A default ValueChangeMap implementation.
 * @param <K> the type of the map keys
 * @param <V> the type of the map values
 */
public class ValueChangeMapImpl<K, V> implements ValueChangeMap<K, V>, Serializable {

  private static final long serialVersionUID = 1;

  /**
   * Holds the values contained in this value map.
   */
  private final Map<K, V> values;

  /**
   * Holds the original value for keys which values have changed.
   */
  private Map<K, V> originalValues;

  /**
   * Fired when a value changes, null until initialized by a call to eventValueChanged().
   */
  private transient Event evtValueChanged;

  private static final int DEFAULT_SIZE = 10;

  /**
   * Instantiate a new ValueChangeMapModel with a default size of 10.
   */
  public ValueChangeMapImpl() {
    this(DEFAULT_SIZE);
  }

  /**
   * Instantiates a new ValueChangeMapModel with a size of <code>initialSize</code>.
   * @param initialSize the initial size
   */
  public ValueChangeMapImpl(final int initialSize) {
    values = new HashMap<K, V>(initialSize);
  }

  /** {@inheritDoc} */
  public boolean containsValue(final K key) {
    Util.rejectNullValue(key);
    return values.containsKey(key);
  }

  /** {@inheritDoc} */
  public boolean isValueNull(final K key) {
    return getValue(key) == null;
  }

  /** {@inheritDoc} */
  public V getValue(final K key) {
    Util.rejectNullValue(key);
    return values.get(key);
  }

  /** {@inheritDoc} */
  public Collection<V> getValues() {
    return Collections.unmodifiableCollection(values.values());
  }

  /** {@inheritDoc} */
  public V getOriginalValue(final K key) {
    Util.rejectNullValue(key);
    if (isModified(key)) {
      return originalValues.get(key);
    }

    return getValue(key);
  }

  /** {@inheritDoc} */
  public boolean isModified() {
    return originalValues != null && originalValues.size() > 0;
  }

  /** {@inheritDoc} */
  public boolean isModified(final K key) {
    Util.rejectNullValue(key);
    return originalValues != null && originalValues.containsKey(key);
  }

  /** {@inheritDoc} */
  public void initializeValue(final K key, final V value) {
    Util.rejectNullValue(key);
    values.put(key, value);
    if (evtValueChanged != null) {
      notifyValueChange(key, value, null, true);
    }
  }

  /** {@inheritDoc} */
  public V setValue(final K key, final V value) {
    Util.rejectNullValue(key);
    final boolean initialization = !containsValue(key);
    V previousValue = null;
    if (!initialization) {
      previousValue = getValue(key);
      if (Util.equal(previousValue, value)) {
        return previousValue;
      }
    }

    if (!initialization) {
      updateModifiedState(key, value, previousValue);
    }

    values.put(key, value);
    if (evtValueChanged != null) {
      notifyValueChange(key, value, previousValue, initialization);
    }

    return previousValue;
  }

  /** {@inheritDoc} */
  public V removeValue(final K key) {
    final boolean keyExists = containsValue(key);
    final V value = values.get(key);
    values.remove(key);
    removeOriginalValue(key);

    if (keyExists && evtValueChanged != null) {//dont notify a non-existant key
      notifyValueChange(key, null, value, false);
    }

    return value;
  }

  /** {@inheritDoc} */
  public void revertValue(final K key) {
    Util.rejectNullValue(key);
    if (isModified(key)) {
      setValue(key, getOriginalValue(key));
    }
  }

  /** {@inheritDoc} */
  public void revertAll() {
    for (final K key : getValueKeys()) {
      revertValue(key);
    }
  }

  /** {@inheritDoc} */
  public void saveValue(final K key) {
    Util.rejectNullValue(key);
    removeOriginalValue(key);
  }

  /** {@inheritDoc} */
  public void saveAll() {
    for (final K key : getValueKeys()) {
      saveValue(key);
    }
  }

  /** {@inheritDoc} */
  public void clear() {
    values.clear();
    if (originalValues != null) {
      originalValues.clear();
    }
  }

  /** {@inheritDoc} */
  public void setAs(final ValueChangeMap<K, V> sourceMap) {
    clear();
    if (sourceMap != null) {
      if (sourceMap.isModified()) {
        if (originalValues == null) {
          originalValues = new HashMap<K, V>();
        }
        for (final K entryKey : sourceMap.getOriginalValueKeys()) {
          originalValues.put(entryKey, copyValue(sourceMap.getOriginalValue(entryKey)));
        }
      }
      for (final K entryKey : sourceMap.getValueKeys()) {
        final V value = copyValue(sourceMap.getValue(entryKey));
        values.put(entryKey, value);
        if (evtValueChanged != null) {
          notifyValueChange(entryKey, value, null, true);
        }
      }
    }
  }

  /** {@inheritDoc} */
  public ValueChangeMap<K, V> getInstance() {
    return new ValueChangeMapImpl<K, V>();
  }

  /** {@inheritDoc} */
  public ValueChangeMap<K, V> getCopy() {
    final ValueChangeMap<K, V> copy = getInstance();
    copy.setAs(this);

    return copy;
  }

  /** {@inheritDoc} */
  public ValueChangeMap<K, V> getOriginalCopy() {
    final ValueChangeMap<K, V> copy = getCopy();
    copy.revertAll();

    return copy;
  }

  /** {@inheritDoc} */
  public V copyValue(final V value) {
    return value;
  }

  /** {@inheritDoc} */
  public Collection<K> getOriginalValueKeys() {
    return originalValues == null ? new ArrayList<K>() :
            Collections.unmodifiableCollection(originalValues.keySet());
  }

  /** {@inheritDoc} */
  public Collection<K> getValueKeys() {
    return Collections.unmodifiableCollection(values.keySet());
  }

  /** {@inheritDoc} */
  public void addValueListener(final ActionListener valueListener) {
    eventValueChanged().addListener(valueListener);
  }

  /** {@inheritDoc} */
  public void removeValueListener(final ActionListener valueListener) {
    if (evtValueChanged != null) {
      evtValueChanged.removeListener(valueListener);
    }
  }

  /**
   * Two ValueChangeMapImpl objects are equal if all current property values are equal.
   */
  @SuppressWarnings({"unchecked"})
  @Override
  public boolean equals(final Object obj) {
    if (!(obj instanceof ValueChangeMapImpl)) {
      return false;
    }

    final ValueChangeMapImpl<K, V> otherMap = (ValueChangeMapImpl<K, V>) obj;
    if (values.size() != otherMap.values.size()) {
      return false;
    }

    for (final K key : otherMap.values.keySet()) {
      if (!containsValue(key) || !valuesEqual(otherMap.getValue(key), getValue(key))) {
        return false;
      }
    }

    return true;
  }

  @Override
  public int hashCode() {
    int hash = 23;
    for (final Object value : values.values()) {
      hash = hash + (value == null ? 0 : value.hashCode());
    }

    return hash;
  }

  /** {@inheritDoc} */
  public State stateModified() {
    final State state = new State(isModified());
    eventValueChanged().addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        state.setActive(isModified());
      }
    });

    return state.getLinkedState();
  }

  /** {@inheritDoc} */
  public Event eventValueChanged() {
    if (evtValueChanged == null) {
      evtValueChanged = initializeValueChangedEvent();
    }

    return evtValueChanged;
  }

  /**
   * @return a Event to be used as value change event
   */
  protected Event initializeValueChangedEvent() {
    return new Event();
  }

  protected boolean valuesEqual(final V valueOne, final V valueTwo) {
    return Util.equal(valueOne, valueTwo);
  }

  protected void notifyValueChange(final K key, final V value, final V oldValue, final boolean initialization) {
    eventValueChanged().fire(new ValueChangeEvent<K, V>(this, this, key, value, oldValue, true, initialization));
  }

  protected void setOriginalValue(final K key, final V oldValue) {
    if (originalValues == null) {
      originalValues = new HashMap<K, V>();
    }
    originalValues.put(key, oldValue);
  }

  protected void removeOriginalValue(final K key) {
    if (originalValues != null && originalValues.containsKey(key)) {
      originalValues.remove(key);
      if (evtValueChanged != null) {
        notifyValueChange(key, values.get(key), null, true);
      }
    }
  }

  protected void updateModifiedState(final K key, final V value, final V previousValue) {
    final boolean modified = isModified(key);
    if (modified && Util.equal(getOriginalValue(key), value)) {
      removeOriginalValue(key);//we're back to the original value
    }
    else if (!modified) {
      setOriginalValue(key, previousValue);
    }
  }
}
