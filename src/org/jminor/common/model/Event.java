/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A synchronous event class
 */
public class Event implements ActionListener, Serializable {

  private final String name;

  private transient final List<ActionListener> listeners = new ArrayList<ActionListener>();
  private transient final ActionEvent defaultEvent = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "");

  /**
   * Instantiates a new Event
   * @param name the name of this event, useful for debuggin purposes
   */
  public Event(final String name) {
    this.name = name;
  }

  /**
   * Notifies all listeners
   */
  public void fire() {
    fire(defaultEvent);
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

  /** {@inheritDoc} */
  public String toString() {
    return name;
  }
}