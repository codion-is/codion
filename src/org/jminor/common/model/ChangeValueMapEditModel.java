/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

public abstract class ChangeValueMapEditModel<T, V> {

  protected final ChangeValueMap<T, V> valueMap;
  private final Event evtEntityChanged = new Event();

  /**
   * The name of this ValueMap.
   */
  private final String name;

  /**
   * Holds events signaling property changes made to the active entity via the ui
   */
  private final Map<T, Event> propertyValueSetEventMap = new HashMap<T, Event>();

  /**
   * Holds events signaling property changes made to the active entity, via the model or ui
   */
  private final Map<T, Event> propertyChangeEventMap = new HashMap<T, Event>();

  public ChangeValueMapEditModel() {
    this(null);
  }

  public ChangeValueMapEditModel(final String name) {
    this.name = name;
    this.valueMap = initializeMap();
    this.valueMap.setAs(getDefaultMap());
    bindEventsInternal();
  }

  /**
   * @return the name of this ChangeValueMap, may be null
   */
  public String getName() {
    return name;
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
   * @see #getDefaultMap()
   */
  public final void setMap(final ChangeValueMap<T, V> valueMap) {
    if (valueMap != null && this.valueMap.equals(valueMap))
      return;

    this.valueMap.setAs(valueMap == null ? getDefaultMap() : valueMap);
    evtEntityChanged.fire();
  }

  /**
   * @param key the key for which to retrieve the event
   * @return an Event object which fires when the value of <code>key</code> is changed via
   * the <code>setValue()</code> methods
   * @see #setValue(Object, Object)
   */
  public Event getPropertyValueSetEvent(final T key) {
    if (!propertyValueSetEventMap.containsKey(key))
      propertyValueSetEventMap.put(key, new Event());

    return propertyValueSetEventMap.get(key);
  }

  /**
   * @param key the key for which to retrieve the event
   * @return an Event object which fires when the value of <code>key</code> changes
   */
  public Event getPropertyChangeEvent(final T key) {
    if (!propertyChangeEventMap.containsKey(key))
      propertyChangeEventMap.put(key, new Event());

    return propertyChangeEventMap.get(key);
  }

  /**
   * @return an Event fired when the active entity has been changed
   */
  public Event eventEntityChanged() {
    return evtEntityChanged;
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
        final Event propertyEvent = propertyChangeEventMap.get(key);
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

  protected abstract ChangeValueMap<T, V> getDefaultMap();

  protected abstract ChangeValueMap<T, V> initializeMap();

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
