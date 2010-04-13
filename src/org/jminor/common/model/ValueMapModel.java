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
public class ValueMapModel implements ChangeValueMap, Serializable {

  private static final long serialVersionUID = 1;

  /**
   * Holds the values contained in this value map.
   */
  protected final Map<String, Object> values;

  /**
   * Holds the original value for keys which values have changed.
   */
  protected Map<String, Object> originalValues;

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
    values = new HashMap<String, Object>(initialSize);
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
  public boolean containsValue(final String key) {
    return values.containsKey(key);
  }

  /** {@inheritDoc} */
  public boolean isValueNull(final String key) {
    return containsValue(key) && getValue(key) == null;
  }

  /** {@inheritDoc} */
  public Object getValue(final String key) {
    return values.get(key);
  }

  /** {@inheritDoc} */
  public Object getOriginalValue(final String key) {
    if (isModified(key))
      return originalValues.get(key);

    return getValue(key);
  }

  /** {@inheritDoc} */
  public boolean isModified() {
    return originalValues != null && originalValues.size() > 0;
  }

  /** {@inheritDoc} */
  public boolean isModified(final String key) {
    return originalValues != null && originalValues.containsKey(key);
  }

  /** {@inheritDoc} */
  public Object setValue(final String key, final Object value) {
    final boolean initialization = !containsValue(key);
    Object oldValue = null;
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
  public Object removeValue(final String key) {
    final Object value = getValue(key);
    doRemoveValue(key);
    doRemoveOriginalValue(key);

    if (stModified != null)
      stModified.setActive(isModified());
    if (evtPropertyChanged != null)
      evtPropertyChanged.fire(getValueChangeEvent(key, null, value, false));

    return value;
  }

  /** {@inheritDoc} */
  public void revertValue(final String key) {
    if (isModified(key))
      setValue(key, getOriginalValue(key));
  }

  /** {@inheritDoc} */
  public ActionEvent getValueChangeEvent(final String key, final Object newValue, final Object oldValue,
                                         final boolean initialization) {
    return new ActionEvent(this, 0, key);
  }

  @Override
  public boolean equals(final Object object) {
    if (!(object instanceof ValueMapModel))
      return false;

    final ValueMapModel otherMap = (ValueMapModel) object;
    if (values.size() != otherMap.values.size())
      return false;
    for (final Map.Entry<String, Object> entry : otherMap.values.entrySet()) {
      if (containsValue(entry.getKey())) {
        if (!Util.equal(entry.getValue(), getValue(entry.getKey())))
          return false;
      }
      else
        return false;
    }

    return true;
  }

  private void doSetOriginalValue(final String key, final Object oldValue) {
    (originalValues == null ? (originalValues = new HashMap<String, Object>()) : originalValues).put(key, oldValue);
  }

  private void doRemoveOriginalValue(final String key) {
    if (originalValues != null)
      originalValues.remove(key);
  }

  private void doRemoveValue(final String key) {
    values.remove(key);
  }
}
