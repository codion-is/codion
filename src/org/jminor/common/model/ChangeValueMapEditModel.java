/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

public abstract class ChangeValueMapEditModel<T, V> {

  private final ChangeValueMap<T, V> valueMap;
  private final Event evtEntityChanged = new Event();
  private final Event evtModelCleared = new Event();

  private ChangeValueMap<T, V> defaultValueMap;

  /**
   * Holds events signaling value changes made via the ui
   */
  private final Map<T, Event> valueSetEventMap = new HashMap<T, Event>();

  /**
   * Holds events signaling value changes made via the model or ui
   */
  private final Map<T, Event> valueChangeEventMap = new HashMap<T, Event>();

  public ChangeValueMapEditModel(final ChangeValueMap<T, V> initialMap) {
    this.valueMap = initialMap;
    bindEventsInternal();
  }

  public V getValue(final T key) {
    return valueMap.getValue(key);
  }

  public void setValue(final T key, final V value) {
    final boolean initialization = valueMap.containsValue(key);
    final V oldValue = getValue(key);
    final V newValue = doSetValue(key, value);

    if (!Util.equal(newValue, oldValue))
      notifyPropertyValueSet(key, getValueChangeEvent(key, newValue, oldValue, initialization));
  }

  public void clear() {
    setValueMap(null);
    evtModelCleared.fire();
  }

  /**
   * @param key the key
   * @return true if the value of the given key is null
   */
  public boolean isValueNull(final T key) {
    return valueMap.isValueNull(key);
  }

  /**
   * Sets the active entity, that is, the entity to be edited
   * @param valueMap the map to set as active, if null then the default map value is set as active
   * @see #evtEntityChanged
   */
  public void setValueMap(final ChangeValueMap<T, V> valueMap) {
    if (valueMap != null && this.valueMap.equals(valueMap))
      return;

    this.valueMap.setAs(valueMap == null ? getDefaultValueMap() : valueMap);
    evtEntityChanged.fire();
  }

  /**
   * @return a value map containing the default values
   */
  public ChangeValueMap<T, V> getDefaultValueMap() {
    return defaultValueMap;
  }

  /**
   * Sets the value map containing the default values
   * @param defaultValueMap the default values
   */
  public void setDefaultValueMap(final ChangeValueMap<T, V> defaultValueMap) {
    this.defaultValueMap = defaultValueMap;
  }

  /**
   * @param key the key for which to retrieve the event
   * @return an Event object which fires when the value of <code>key</code> is changed via
   * the <code>setValue()</code> methods
   * @see #setValue(Object, Object)
   */
  public Event getPropertyValueSetEvent(final T key) {
    if (!valueSetEventMap.containsKey(key))
      valueSetEventMap.put(key, new Event());

    return valueSetEventMap.get(key);
  }

  /**
   * @param key the key for which to retrieve the event
   * @return an Event object which fires when the value of <code>key</code> changes
   */
  public Event getPropertyChangeEvent(final T key) {
    if (!valueChangeEventMap.containsKey(key))
      valueChangeEventMap.put(key, new Event());

    return valueChangeEventMap.get(key);
  }

  /**
   * @return an Event fired when the active entity has been changed
   */
  public Event eventEntityChanged() {
    return evtEntityChanged;
  }

  /**
   * @return an Event fired when the model has been cleared
   */
  public Event eventModelCleared() {
    return evtModelCleared;
  }

  public State stateModified() {
    return valueMap.stateModified();
  }

  private void notifyPropertyValueSet(final T key, final ActionEvent event) {
    getPropertyValueSetEvent(key).fire(event);
  }

  private void bindEventsInternal() {
    valueMap.addValueListener(new ActionListener() {
      @SuppressWarnings({"unchecked"})
      public void actionPerformed(final ActionEvent event) {
        final T key = (T) event.getSource();
        final Event propertyEvent = valueChangeEventMap.get(key);
        if (propertyEvent != null)
          propertyEvent.fire(event);
      }
    });
  }

  /**
   * Sets the value in the underlying entity
   * @param key the key for which to set the value
   * @param value the value
   * @return the value that was just set
   */
  protected V doSetValue(final T key, final V value) {
    valueMap.setValue(key, value);

    return value;
  }

  protected ChangeValueMap<T, V> getValueMap() {
    return valueMap;
  }

  /**
   * Initializes a ActionEvent describing the value change.
   * The ActionEvent must have <code>key</code> as source so that event.getSource() == key.
   * @param key the key of the value being changed
   * @param newValue the new value
   * @param oldValue the old value
   * @param initialization if true then the value was being initialized
   * @return the ActionEvent describing the value change
   */
  protected abstract ActionEvent getValueChangeEvent(final T key, final V newValue, final V oldValue, final boolean initialization);
}
