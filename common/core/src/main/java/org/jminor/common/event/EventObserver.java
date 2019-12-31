/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.event;

/**
 * Manages listeners for an Event.
 * @param <T> the type of data propagated with the event.
 */
public interface EventObserver<T> {

  /**
   * Adds {@code listener} to this EventObserver, adding the same listener
   * a second time has no effect.
   * @param listener the listener to add
   * @throws NullPointerException in case listener is null
   */
  void addListener(EventListener listener);

  /**
   * Removes {@code listener} from this EventObserver
   * @param listener the listener to remove
   */
  void removeListener(EventListener listener);

  /**
   * Adds {@code listener} to this EventObserver, adding the same listener
   * a second time has no effect.
   * @param listener the listener to add
   * @throws NullPointerException in case listener is null
   */
  void addDataListener(EventDataListener<T> listener);

  /**
   * Removes {@code listener} from this EventObserver
   * @param listener the listener to remove
   */
  void removeDataListener(EventDataListener listener);
}
