/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

/**
 * A event class.
 * Listeners are notified in the order they were added.
 * <pre>
 * Event&lt;Boolean&gt; event = Events.event();
 *
 * EventObserver&lt;Boolean&gt; observer = event.getObserver();
 *
 * observer.addListener(this::doSomething);
 * observer.addDataListener(this::handleBoolean);
 *
 * event.fire(true);
 * </pre>
 * @param <T> the type of data propagated with this event
 */
public interface Event<T> extends EventListener, EventDataListener<T>, EventObserver<T> {

  /**
   * Notifies all listeners that this event has occurred
   */
  void fire();

  /**
   * Notifies all listeners that this event has occurred
   * @param data information to propagate to listeners when notifying
   */
  void fire(final T data);

  /**
   * @return an observer notified each time this event fires
   */
  EventObserver<T> getObserver();
}