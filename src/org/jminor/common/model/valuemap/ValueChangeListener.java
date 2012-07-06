/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import org.jminor.common.model.EventListener;

import java.awt.event.ActionEvent;

/**
 * Used when listening to ValueChangeEvents.
 * @param <K> the type of the object used to identify the value, or the key type
 * @param <V> the type of the actual value
 */
public abstract class ValueChangeListener<K, V> implements EventListener {

  /**
   * Calls <code>valueChanged()</code> assuming the given event is a ValueChangeEvent
   * @param e the event
   * @throws IllegalArgumentException in case the received event is not a ValueChangeEvent instance
   */
  @Override
  @SuppressWarnings({"unchecked"})
  public final void eventOccurred(final ActionEvent e) {
    if (!(e instanceof ValueChangeEvent)) {
      throw new IllegalArgumentException("ValueChangeListener can only be used with ValueChangeEvent, " + e);
    }

    valueChanged((ValueChangeEvent<K, V>) e);
  }

  /**
   * Called when a value changes.
   * @param event the event describing the value change
   */
  protected abstract void valueChanged(final ValueChangeEvent<K, V> event);
}
