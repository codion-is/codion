/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.event;

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
   * @return a {@link EventListener} causing the given {@link EventDataListener} to be fired with null data on each occurrence
   */
  public static EventListener listener(final EventDataListener listener) {
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
