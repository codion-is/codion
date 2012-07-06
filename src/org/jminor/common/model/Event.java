/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.awt.event.ActionEvent;

/**
 * A synchronous event class.
 * Listeners are notified in the order they were added.
 * <pre>
 * Event event = Events.event();
 *
 * event.addListener(new EventListener...);
 *
 * EventObserver observer = event.getObserver();
 *
 * bindModelToEvent(observer);
 *
 * event.fire();
 * </pre>
 */
public interface Event extends EventListener, EventObserver {

  /**
   * Notifies all listeners
   */
  void fire();

  /**
   * Notifies all listeners with the given ActionEvent
   * @param event the ActionEvent to propagate to listeners when notifying
   */
  void fire(final ActionEvent event);

  /**
   * @return an observer notified each time this event fires
   */
  EventObserver getObserver();
}