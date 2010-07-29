/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A synchronous event class.
 */
public interface Event extends ActionListener, EventObserver {

  /**
   * Notifies all listeners
   */
  void fire();

  /**
   * Notifies all listeners
   * @param event the ActionEvent to use when notifying
   */
  void fire(final ActionEvent event);

  void actionPerformed(final ActionEvent e);

  /**
   * Adds <code>listener</code> to this Event, adding the same listener
   * a second time has no effect.
   * @param listener the listener to add
   * @throws IllegalArgumentException in case listener is null
   */
  void addListener(final ActionListener listener);

  /**
   * Removes <code>listener</code> from this Event
   * @param listener the listener to remove
   */
  void removeListener(final ActionListener listener);
}