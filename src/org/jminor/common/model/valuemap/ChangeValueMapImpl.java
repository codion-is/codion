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
import java.util.HashMap;
import java.util.Map;

/**
 * A default ChangeValueMap implementation.
 */
public class ChangeValueMapImpl<T, V> implements ChangeValueMap<T, V>, Serializable {

  private static final long serialVersionUID = 1;

  /**
   * Holds the values contained in this value map.
   */
  private final Map<T, V> values;

  /**
   * Holds the original value for keys which values have changed.
   */
  private Map<T, V> originalValues;

  /**
   * Holds the modified state of this value map.
   */
  private transient State stModified;

  /**
   * Fired when a value changes.
   */
  protected transient Event evtValueChanged;

  /**
   * Instantiate a new ValueMapModel with a default size of 10.
   */
  public ChangeValueMapImpl() {
    this(10);
  }

  /**
   * Instantiates a new ValueMapModel with a size of <code>initialSize</code>.
   * @param initialSize the initial size
   */
  public ChangeValueMapImpl(final int initialSize) {
    values = new HashMap<T, V>(initialSize);
  }

  /** {@inheritDoc} */
  public State stateModified() {
    if (stModified == null)
      stModified = new State(isModified());

    return stModified.getLinkedState();
  }

  /** {@inheritDoc} */
  public Event eventValueChanged() {
    if (evtValueChanged == null)
      evtValueChanged = new Event();

    return evtValueChanged;
  }

  /** {@inheritDoc} */
  public boolean containsValue(final T key) {
    return values.containsKey(key);
  }

  /** {@inheritDoc} */
  public boolean isValueNull(final T key) {
    return containsValue(key) && getValue(key) == null;
  }

  /** {@inheritDoc} */
  public V getValue(final T key) {
    return values.get(key);
  }

  /** {@inheritDoc} */
  public Collection<V> getValues() {
    return new ArrayList<V>(values.values());
  }

  /** {@inheritDoc} */
  public V getOriginalValue(final T key) {
    if (isModified(key))
      return originalValues.get(key);

    return getValue(key);
  }

  /** {@inheritDoc} */
  public boolean isModified() {
    return originalValues != null && originalValues.size() > 0;
  }

  /** {@inheritDoc} */
  public boolean isModified(final T key) {
    return originalValues != null && originalValues.containsKey(key);
  }

  /** {@inheritDoc} */
  public void clearOriginalValues() {
    if (originalValues != null)
      originalValues.clear();

    if (stModified != null)
      stModified.setActive(isModified());
  }

  /** {@inheritDoc} */
  public V setValue(final T key, final V value) {
    final boolean initialization = !containsValue(key);
    V oldValue = null;
    if (!initialization) {
      oldValue = getValue(key);
      if (Util.equal(oldValue, value))
        return oldValue;
    }

    if (!initialization) {
      if (isModified(key) && Util.equal(getOriginalValue(key), value))
        removeOriginalValue(key);//we're back to the original value
      else if (!isModified(key))
        setOriginalValue(key, oldValue);

      if (stModified != null)
        stModified.setActive(isModified());
    }

    values.put(key, value);
    if (evtValueChanged != null)
      notifyValueChange(key, value, initialization, oldValue);

    return oldValue;
  }

  /** {@inheritDoc} */
  public V removeValue(final T key) {
    final V value = values.get(key);
    values.remove(key);
    removeOriginalValue(key);

    if (stModified != null)
      stModified.setActive(isModified());
    if (evtValueChanged != null)
      notifyValueChange(key, null, false, value);

    return value;
  }

  /** {@inheritDoc} */
  public void revertValue(final T key) {
    if (isModified(key))
      setValue(key, getOriginalValue(key));
  }

  /** {@inheritDoc} */
  public void revertAll() {
    for (final T key : getValueKeys())
      revertValue(key);
  }

  /** {@inheritDoc} */
  public void clear() {
    values.clear();
    if (originalValues != null)
      originalValues.clear();
  }

  /** {@inheritDoc} */
  public void setAs(final ChangeValueMap<T, V> changeValueMap) {
    clear();
    if (changeValueMap != null) {
      for (final T entryKey : changeValueMap.getValueKeys()) {
        final V value = copyValue(changeValueMap.getValue(entryKey));
        values.put(entryKey, value);
        if (evtValueChanged != null)
          notifyValueChange(entryKey, value, true, null);
      }
      if (changeValueMap.isModified()) {
        if (originalValues == null)
          originalValues = new HashMap<T, V>();
        for (final T entryKey : changeValueMap.getOriginalValueKeys())
          originalValues.put(entryKey, copyValue(changeValueMap.getOriginalValue(entryKey)));
      }
    }
    if (stModified != null)
      stModified.setActive(isModified());
  }

  /** {@inheritDoc} */
  public V copyValue(final V value) {
    return value;
  }

  /** {@inheritDoc} */
  public Collection<T> getOriginalValueKeys() {
    return originalValues == null ? new ArrayList<T>() : originalValues.keySet();
  }

  /** {@inheritDoc} */
  public Collection<T> getValueKeys() {
    return new ArrayList<T>(values.keySet());
  }

  /** {@inheritDoc} */
  public void addValueListener(final ActionListener valueListener) {
    eventValueChanged().addListener(valueListener);
  }

  /** {@inheritDoc} */
  public void removeValueListener(final ActionListener valueListener) {
    if (evtValueChanged != null)
      evtValueChanged.removeListener(valueListener);
  }

  /** {@inheritDoc} */
  public ActionEvent getValueChangeEvent(final T key, final V newValue, final V oldValue, final boolean initialization) {
    return new ActionEvent(key, 0, key.toString());
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public boolean equals(final Object object) {
    if (!(object instanceof ChangeValueMapImpl))
      return false;

    final ChangeValueMapImpl<T, V> otherMap = (ChangeValueMapImpl<T, V>) object;
    if (values.size() != otherMap.values.size())
      return false;

    for (final T key : otherMap.values.keySet()) {
      if (!containsValue(key) || !valuesEqual(otherMap.getValue(key), getValue(key))) {
        return false;
      }
    }

    return true;
  }

  protected boolean valuesEqual(final V valueOne, final V valueTwo) {
    return Util.equal(valueOne, valueTwo);
  }

  protected void notifyValueChange(final T key, final V value, final boolean initialization, final V oldValue) {
    evtValueChanged.fire(getValueChangeEvent(key, value, oldValue, initialization));
  }

  protected void setOriginalValue(final T key, final V oldValue) {
    (originalValues == null ? (originalValues = new HashMap<T, V>()) : originalValues).put(key, oldValue);
  }

  protected void removeOriginalValue(final T key) {
    if (originalValues != null)
      originalValues.remove(key);
  }
}
