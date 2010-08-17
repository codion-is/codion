/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Used when listening to ValueChangeEvents
 */
public abstract class ValueChangeListener<K, V> implements ActionListener {

  /**
   * Calls <code>valueChanged()</code> assuming the given event is a ValueChangeEvent
   * @param e the event
   * @throws IllegalArgumentException in case the received event is not a ValueChangeEvent instance
   */
  @SuppressWarnings({"unchecked"})
  public final void actionPerformed(final ActionEvent e) {
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
