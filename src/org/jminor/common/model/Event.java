/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * A synchronous event class.
 */
public final class Event implements ActionListener {

  private final Set<ActionListener> listeners = new HashSet<ActionListener>();
  private final ActionEvent defaultActionEvent = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "");

  /**
   * Notifies all listeners
   */
  public void fire() {
    fire(defaultActionEvent);
  }

  /**
   * Notifies all listeners
   * @param event the ActionEvent to use when notifying
   */
  public void fire(final ActionEvent event) {
    for (final ActionListener listener : new ArrayList<ActionListener>(listeners)) {
      listener.actionPerformed(event);
    }
  }

  public void actionPerformed(final ActionEvent e) {
    fire(e);
  }

  /**
   * Adds <code>listener</code> to this Event, adding the same listener
   * a second time has no effect.
   * @param listener the listener to add
   * @throws IllegalArgumentException in case listener is null
   */
  public void addListener(final ActionListener listener) {
    Util.rejectNullValue(listener, "listener");
    listeners.add(listener);
  }

  /**
   * Removes <code>listener</code> from this Event
   * @param listener the listener to remove
   */
  public void removeListener(final ActionListener listener) {
    listeners.remove(listener);
  }
}