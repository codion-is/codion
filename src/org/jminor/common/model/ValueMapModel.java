/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.awt.event.ActionEvent;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * A ValueMap implementation which keeps track of value modifications.
 */
public class ValueMapModel<T, V> implements ChangeValueMap<T, V>, Serializable {

  private static final long serialVersionUID = 1;

  /**
   * Holds the values contained in this value map.
   */
  protected final Map<T, V> values;

  /**
   * Holds the original value for keys which values have changed.
   */
  protected Map<T, V> originalValues;

  /**
   * Holds the modified state of this value map.
   */
  protected transient State stModified;

  /**
   * Fired when a value changes.
   */
  protected transient Event evtPropertyChanged;

  /**
   * Instantiate a new ValueMapModel with a default size of 10.
   */
  public ValueMapModel() {
    this(10);
  }

  /**
   * Instantiates a new ValueMapModel with a size of <code>initialSize</code>.
   * @param initialSize the initial size
   */
  public ValueMapModel(final int initialSize) {
    values = new HashMap<T, V>(initialSize);
  }

  /** {@inheritDoc} */
  public State stateModified() {
    if (stModified == null)
      stModified = new State(isModified());

    return stModified.getLinkedState();
  }

  /** {@inheritDoc} */
  public Event eventPropertyChanged() {
    if (evtPropertyChanged == null)
      evtPropertyChanged = new Event();

    return evtPropertyChanged;
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
        doRemoveOriginalValue(key);//we're back to the original value
      else
        doSetOriginalValue(key, oldValue);

      if (stModified != null)
        stModified.setActive(isModified());
    }

    values.put(key, value);

    if (evtPropertyChanged != null)
      evtPropertyChanged.fire(getValueChangeEvent(key, value, oldValue, initialization));

    return oldValue;
  }

  /** {@inheritDoc} */
  public V removeValue(final T key) {
    final V value = getValue(key);
    doRemoveValue(key);
    doRemoveOriginalValue(key);

    if (stModified != null)
      stModified.setActive(isModified());
    if (evtPropertyChanged != null)
      evtPropertyChanged.fire(getValueChangeEvent(key, null, value, false));

    return value;
  }

  /** {@inheritDoc} */
  public void revertValue(final T key) {
    if (isModified(key))
      setValue(key, getOriginalValue(key));
  }

  /** {@inheritDoc} */
  public ActionEvent getValueChangeEvent(final T key, final V newValue, final V oldValue, final boolean initialization) {
    return new ActionEvent(this, 0, key.toString());
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public boolean equals(final Object object) {
    if (!(object instanceof ValueMapModel))
      return false;

    final ValueMapModel<T, V> otherMap = (ValueMapModel<T, V>) object;
    if (values.size() != otherMap.values.size())
      return false;

    for (final T key : otherMap.values.keySet()) {
      if (!containsValue(key)) {
        System.out.println("Missing key: " + key);
        return false;
      }
      else if (!Util.equal(otherMap.getValue(key), getValue(key))) {
        System.out.println("Not equal key: " + key);
        System.out.println(otherMap.getValue(key));
        System.out.println(getValue(key));
        return false;
      }
    }

    return true;
  }

  private void doSetOriginalValue(final T key, final V oldValue) {
    (originalValues == null ? (originalValues = new HashMap<T, V>()) : originalValues).put(key, oldValue);
  }

  private void doRemoveOriginalValue(final T key) {
    if (originalValues != null)
      originalValues.remove(key);
  }

  private void doRemoveValue(final T key) {
    values.remove(key);
  }
}
