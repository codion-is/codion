/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

/**
 * A event class.
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
 * @param <T> the type of info propagated with this event
 */
public interface Event<T> extends EventListener, EventDataListener<T>, EventObserver<T> {

  /**
   * Notifies all listeners that this event has occurred
   */
  void fire();

  /**
   * Notifies all listeners that this event has occurred
   * @param info information to propagate to listeners when notifying
   */
  void fire(final T info);

  /**
   * @return an observer notified each time this event fires
   */
  EventObserver<T> getObserver();
}