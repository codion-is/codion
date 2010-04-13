/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.awt.event.ActionEvent;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * User: Björn Darri
 * Date: 12.4.2010
 * Time: 20:03:15
 */
public abstract class AbstractValueMap implements ValueMap, Serializable {

  private static final long serialVersionUID = 1;

  protected final Map<String, Object> values;
  protected Map<String, Object> originalValues;
  protected transient State stModified;
  protected transient Event evtPropertyChanged;

  public AbstractValueMap() {
    this(10);
  }

  public AbstractValueMap(final int initialSize) {
    values = new HashMap<String, Object>(initialSize);
  }

  public State stateModified() {
    if (stModified == null)
      stModified = new State(isModified());

    return stModified.getLinkedState();
  }

  public Event eventPropertyChanged() {
    if (evtPropertyChanged == null)
      evtPropertyChanged = new Event();

    return evtPropertyChanged;
  }

  public boolean containsValue(final String key) {
    return values.containsKey(key);
  }

  public Object getValue(final String key) {
    return values.get(key);
  }

  public Object getOriginalValue(final String key) {
    if (isModified(key))
      return originalValues.get(key);

    return getValue(key);
  }

  public boolean isModified() {
    return originalValues != null && originalValues.size() > 0;
  }

  public boolean isModified(final String key) {
    return originalValues != null && originalValues.containsKey(key);
  }

  public boolean isValueNull(final String key) {
    return containsValue(key) && getValue(key) == null;
  }

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
        removeOriginalValue(key);//we're back to the original value
      else
        setOriginalValue(key, oldValue);

      if (stModified != null)
        stModified.setActive(isModified());
    }

    values.put(key, value);

    if (evtPropertyChanged != null)
      evtPropertyChanged.fire(getValueChangeEvent(key, value, oldValue, initialization));

    return oldValue;
  }

  protected abstract ActionEvent getValueChangeEvent(final String key, final Object newValue, final Object oldValue,
                                                     final boolean initialization);

  private void setOriginalValue(final String key, final Object oldValue) {
    (originalValues == null ? (originalValues = new HashMap<String, Object>()) : originalValues).put(key, oldValue);
  }

  private void removeOriginalValue(final String key) {
    originalValues.remove(key);
  }
}
