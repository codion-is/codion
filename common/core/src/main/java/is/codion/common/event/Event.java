/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.event;

/**
 * An event class. Listeners are notified in the order they were added.
 * <pre>
 * Event&lt;Boolean&gt; event = Event.event();
 *
 * EventObserver&lt;Boolean&gt; observer = event.getObserver();
 *
 * observer.addListener(this::doSomething);
 * observer.addDataListener(this::onBoolean);
 *
 * event.onEvent(true);
 * </pre>
 * A factory class for Event instances.
 * @param <T> the type of data propagated with this event
 */
public interface Event<T> extends EventListener, EventDataListener<T>, EventObserver<T> {

  /**
   * @return an observer notified each time this event fires
   */
  EventObserver<T> getObserver();

  /**
   * Instantiates a new Event.
   * @param <T> the type of data propagated to listeners on event firing
   * @return a new Event
   */
  static <T> Event<T> event() {
    return new DefaultEvent<>();
  }

  /**
   * Instantiates a {@link EventListener} causing the {@code listener}s {@link EventDataListener#onEvent(Object)} to be called with a null argument on each occurrence.
   * @param listener the data listener
   * @param <T> the value type
   * @return a {@link EventListener} causing the given {@link EventDataListener} to be fired with null data on each occurrence
   */
  static <T> EventListener listener(EventDataListener<T> listener) {
    return () -> listener.onEvent(null);
  }

  /**
   * Instantiates a {@link EventDataListener} causing the {@code listener}s {@link EventListener#onEvent()} to be called on each occurrence.
   * Note that any event data will get discarded along the way.
   * @param <T> the type of data propagated to listeners on event firing
   * @param listener the listener
   * @return a {@link EventDataListener} causing the given {@link EventListener} to be fired on each occurrence
   */
  static <T> EventDataListener<T> dataListener(EventListener listener) {
    return data -> listener.onEvent();
  }
}