/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.event;

/**
 * A factory class for Event instances.
 * @see Event
 */
public final class Events {

  private Events() {}

  /**
   * Instantiates a new Event.
   * @param <T> the type of data propagated to listeners on event firing
   * @return a new Event
   */
  public static <T> Event<T> event() {
    return new DefaultEvent<>();
  }

  /**
   * Instantiates a {@link EventListener} causing the given {@link EventDataListener} to be fired with null data on each occurrence.
   * @param listener the data listener
   * @param <T> the value type
   * @return a {@link EventListener} causing the given {@link EventDataListener} to be fired with null data on each occurrence
   */
  public static <T> EventListener listener(final EventDataListener<T> listener) {
    return () -> listener.onEvent(null);
  }

  /**
   * Instantiates a {@link EventDataListener} causing the given {@link EventListener} to be fired on each occurrence.
   * Note that any event data will get discarded along the way.
   * @param <T> the type of data propagated to listeners on event firing
   * @param listener the listener
   * @return a {@link EventDataListener} causing the given {@link EventListener} to be fired on each occurrence
   */
  public static <T> EventDataListener<T> dataListener(final EventListener listener) {
    return data -> listener.onEvent();
  }
}
