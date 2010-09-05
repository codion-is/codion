/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import org.jminor.common.model.Event;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Events;
import org.jminor.common.model.State;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.States;
import org.jminor.common.model.Util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
public class ValueChangeMapImpl<K, V> extends ValueMapImpl<K, V> implements ValueChangeMap<K, V> {

  /**
   * Holds the original value for keys which values have changed.
   */
  private Map<K, V> originalValues = null;

  /**
   * Fired when a value changes, null until initialized by a call to eventValueChanged().
   */
  private Event evtValueChanged;

  /** {@inheritDoc} */
  public void initializeValue(final K key, final V value) {
    super.setValue(key, value);
    if (evtValueChanged != null) {
      notifyValueChange(key, value, null, true);
    }
    handleValueInitialized(key, value);
  }

  /** {@inheritDoc} */
  public final V getOriginalValue(final K key) {
    if (isModified(key)) {
      return originalValues.get(key);
    }

    return getValue(key);
  }

  /** {@inheritDoc} */
  public boolean isModified() {
    return originalValues != null && !originalValues.isEmpty();
  }

  /** {@inheritDoc} */
  @Override
  public ValueChangeMap<K, V> getInstance() {
    return new ValueChangeMapImpl<K, V>();
  }

  /** {@inheritDoc} */
  public final boolean isModified(final K key) {
    return originalValues != null && originalValues.containsKey(key);
  }

  /** {@inheritDoc} */
  public final void revertValue(final K key) {
    if (isModified(key)) {
      setValue(key, getOriginalValue(key));
    }
  }

  /** {@inheritDoc} */
  public final void revertAll() {
    for (final K key : getValueKeys()) {
      revertValue(key);
    }
  }

  /** {@inheritDoc} */
  public final void saveValue(final K key) {
    removeOriginalValue(key);
  }

  /** {@inheritDoc} */
  public final void saveAll() {
    for (final K key : getValueKeys()) {
      saveValue(key);
    }
  }

  /** {@inheritDoc} */
  public final ValueChangeMap<K, V> getOriginalCopy() {
    final ValueChangeMap<K, V> copy = (ValueChangeMap<K, V>) getCopy();
    copy.revertAll();

    return copy;
  }

  /** {@inheritDoc} */
  public final Collection<K> getOriginalValueKeys() {
    return originalValues == null ? new ArrayList<K>() :
            Collections.unmodifiableCollection(originalValues.keySet());
  }

  /** {@inheritDoc} */
  public final void addValueListener(final ActionListener valueListener) {
    getValueChangeObserver().addListener(valueListener);
  }

  /** {@inheritDoc} */
  public final void removeValueListener(final ActionListener valueListener) {
    if (evtValueChanged != null) {
      evtValueChanged.removeListener(valueListener);
    }
  }

  /** {@inheritDoc} */
  public final StateObserver getModifiedState() {
    final State state = States.state(isModified());
    getValueChangeObserver().addListener(new ActionListener() {
      /** {@inheritDoc} */
      public void actionPerformed(final ActionEvent e) {
        state.setActive(isModified());
      }
    });

    return state.getObserver();
  }

  /** {@inheritDoc} */
  public final EventObserver getValueChangeObserver() {
    return getValueChangedEvent().getObserver();
  }

  protected final void notifyValueChange(final K key, final V value, final V oldValue, final boolean initialization) {
    getValueChangedEvent().fire(new ValueChangeEvent<K, V>(this, this, key, value, oldValue, true, initialization));
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
      if (evtValueChanged != null) {
        notifyValueChange(key, getValue(key), null, true);
      }
    }
  }

  @SuppressWarnings({"UnusedDeclaration"})
  protected void handleValueInitialized(final K key, final V value) {}

  protected void handleValueChangedEventInitialized() {}

  @Override
  protected void handleValueRemoved(final K key, final V value) {
    removeOriginalValue(key);
    if (evtValueChanged != null) {
      notifyValueChange(key, null, value, false);
    }
  }

  @Override
  protected void handleValueSet(final K key, final V value, final V previousValue, final boolean initialization) {
    if (!initialization && Util.equal(previousValue, value)) {
      return;
    }

    if (!initialization) {
      updateModifiedState(key, value, previousValue);
    }

    if (evtValueChanged != null) {
      notifyValueChange(key, value, previousValue, initialization);
    }
  }

  @Override
  protected void handleClear() {
    if (originalValues != null) {
      originalValues.clear();
    }
  }

  @Override
  protected void handleSetAs(final ValueMap<K, V> sourceMap) {
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
