/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A synchronous event class.
 * <pre>
 * Event event = Events.event();
 *
 * event.addListener(new ActionListener...);
 *
 * EventObserver observer = event.getObserver();
 *
 * bindModelToEvent(observer);
 *
 * event.fire();
 * </pre>
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

  /**
   * @return an observer notified each time this event fires
   */
  EventObserver getObserver();
}