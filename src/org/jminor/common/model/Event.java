/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;

/**
 * A synchronous event class.
 */
public class Event implements ActionListener {

  private final Collection<ActionListener> listeners = new ArrayList<ActionListener>();
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
  public final void fire(final ActionEvent event) {
    for (final ActionListener listener : new ArrayList<ActionListener>(listeners))
      listener.actionPerformed(event);
  }

  /** {@inheritDoc} */
  public void actionPerformed(final ActionEvent event) {
    fire(event);
  }

  /**
   * Adds <code>listener</code> to this Event
   * @param listener the listener to add
   */
  public void addListener(final ActionListener listener) {
    if (!listeners.contains(listener))
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