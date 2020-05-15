/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.event;

/**
 * An event class. Listeners are notified in the order they were added.
 * <pre>
 * Event&lt;Boolean&gt; event = Events.event();
 *
 * EventObserver&lt;Boolean&gt; observer = event.getObserver();
 *
 * observer.addListener(this::doSomething);
 * observer.addDataListener(this::onBoolean);
 *
 * event.onEvent(true);
 * </pre>
 * @param <T> the type of data propagated with this event
 */
public interface Event<T> extends EventListener, EventDataListener<T>, EventObserver<T> {

  /**
   * @return an observer notified each time this event fires
   */
  EventObserver<T> getObserver();
}