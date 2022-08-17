/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.event;

/**
 * An event class. Listeners are notified in the order they were added.
 * <pre>
 * Event&lt;Boolean&gt; event = Event.event();
 *
 * EventObserver&lt;Boolean&gt; observer = event.observer();
 *
 * observer.addListener(this::doSomething);
 * observer.addDataListener(this::onBoolean);
 *
 * event.onEvent(true);
 * </pre>
 * A factory class for {@link Event} instances.
 * @param <T> the type of data propagated with this event
 */
public interface Event<T> extends EventListener, EventDataListener<T>, EventObserver<T> {

  /**
   * @return an observer notified each time this event occurs
   */
  EventObserver<T> observer();

  /**
   * Creates a new {@link Event}.
   * @param <T> the type of data propagated to listeners on event occurrence
   * @return a new Event
   */
  static <T> Event<T> event() {
    return new DefaultEvent<>();
  }

  /**
   * Creates a {@link EventListener} causing the {@code listener}s {@link EventDataListener#onEvent(Object)} to be called with a null argument on each occurrence.
   * @param listener the data listener
   * @param <T> the value type
   * @return a {@link EventListener} causing the given {@link EventDataListener} to be called with null data on each occurrence
   */
  static <T> EventListener listener(EventDataListener<T> listener) {
    return () -> listener.onEvent(null);
  }

  /**
   * Creates a {@link EventDataListener} causing the {@code listener}s {@link EventListener#onEvent()} to be called on each occurrence.
   * Note that any event data will get discarded along the way.
   * @param <T> the type of data propagated to listeners on event occurrence
   * @param listener the listener
   * @return a {@link EventDataListener} causing the given {@link EventListener} to be called on each occurrence
   */
  static <T> EventDataListener<T> dataListener(EventListener listener) {
    return data -> listener.onEvent();
  }
}