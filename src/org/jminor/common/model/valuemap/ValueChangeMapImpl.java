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
public class ValueChangeMapImpl<K, V> extends ValueMapImpl<K, V> implements ValueChangeMap<K, V>, Serializable {

  private static final long serialVersionUID = 1;

  /**
   * Holds the original value for keys which values have changed.
   */
  private Map<K, V> originalValues;

  /**
   * Fired when a value changes, null until initialized by a call to eventValueChanged().
   */
  private transient Event evtValueChanged;

  /**
   * Instantiates a new ValueChangeMapImpl with a size of <code>initialSize</code>.
   * @param initialSize the initial size
   */
  public ValueChangeMapImpl(final int initialSize) {
    super(initialSize);
  }

  public ValueChangeMapImpl() {}

  public V getOriginalValue(final K key) {
    Util.rejectNullValue(key, "key");
    if (isModified(key)) {
      return originalValues.get(key);
    }

    return getValue(key);
  }

  public boolean isModified() {
    return originalValues != null && originalValues.size() > 0;
  }

  public boolean isModified(final K key) {
    return originalValues != null && originalValues.containsKey(key);
  }

  public void initializeValue(final K key, final V value) {
    super.setValue(key, value);
    if (evtValueChanged != null) {
      notifyValueChange(key, value, null, true);
    }
  }

  @Override
  public V setValue(final K key, final V value) {
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

    super.setValue(key, value);
    if (evtValueChanged != null) {
      notifyValueChange(key, value, previousValue, initialization);
    }

    return previousValue;
  }

  @Override
  public V removeValue(final K key) {
    final boolean keyExists = containsValue(key);
    final V value = getValue(key);
    super.removeValue(key);
    removeOriginalValue(key);

    if (keyExists && evtValueChanged != null) {//dont notify a non-existant key
      notifyValueChange(key, null, value, false);
    }

    return value;
  }

  public void revertValue(final K key) {
    if (isModified(key)) {
      setValue(key, getOriginalValue(key));
    }
  }

  public void revertAll() {
    for (final K key : getValueKeys()) {
      revertValue(key);
    }
  }

  public void saveValue(final K key) {
    removeOriginalValue(key);
  }

  public void saveAll() {
    for (final K key : getValueKeys()) {
      saveValue(key);
    }
  }

  @Override
  public void clear() {
    super.clear();
    if (originalValues != null) {
      originalValues.clear();
    }
  }

  @Override
  public void setAs(final ValueMap<K, V> sourceMap) {
    super.setAs(sourceMap);
    if (sourceMap instanceof ValueChangeMap) {
      final ValueChangeMap<K, V> sourceChangeMap = (ValueChangeMap<K, V>) sourceMap;
      if (sourceChangeMap.isModified()) {
        if (originalValues == null) {
          originalValues = new HashMap<K, V>();
        }
        for (final K entryKey : sourceChangeMap.getOriginalValueKeys()) {
          originalValues.put(entryKey, copyValue(sourceChangeMap.getOriginalValue(entryKey)));
        }
      }
    }
  }

  @Override
  public ValueChangeMap<K, V> getInstance() {
    return new ValueChangeMapImpl<K, V>();
  }

  public ValueChangeMap<K, V> getOriginalCopy() {
    final ValueChangeMap<K, V> copy = (ValueChangeMap<K, V>) getCopy();
    copy.revertAll();

    return copy;
  }

  public Collection<K> getOriginalValueKeys() {
    return originalValues == null ? new ArrayList<K>() :
            Collections.unmodifiableCollection(originalValues.keySet());
  }

  public void addValueListener(final ActionListener valueListener) {
    eventValueChanged().addListener(valueListener);
  }

  public void removeValueListener(final ActionListener valueListener) {
    if (evtValueChanged != null) {
      evtValueChanged.removeListener(valueListener);
    }
  }

  public State stateModified() {
    final State state = new State(isModified());
    eventValueChanged().addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        state.setActive(isModified());
      }
    });

    return state.getLinkedState();
  }

  public Event eventValueChanged() {
    if (evtValueChanged == null) {
      evtValueChanged = new Event();
    }

    return evtValueChanged;
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
        notifyValueChange(key, getValue(key), null, true);
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
