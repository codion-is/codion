/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import org.jminor.common.model.EventAdapter;

/**
 * Used when listening to ValueChangeEvents.
 * @param <K> the type of the object used to identify the value, or the key type
 * @param <V> the type of the actual value
 */
public abstract class ValueChangeListener<K, V> extends EventAdapter<ValueChangeEvent<K, V>> {

  /**
   * Calls <code>valueChanged()</code> assuming the given event is a ValueChangeEvent
   * @param eventInfo the event info
   */
  @Override
  public final void eventOccurred(final ValueChangeEvent<K, V> eventInfo) {
    valueChanged(eventInfo);
  }

  /**
   * Called when a value changes.
   * @param event the event describing the value change
   */
  protected abstract void valueChanged(final ValueChangeEvent<K, V> event);
}
