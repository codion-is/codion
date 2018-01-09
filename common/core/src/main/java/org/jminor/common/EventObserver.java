/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

/**
 * Specifies an Event observer, managing listeners for an Event.
 * @param <T> the type of info propagated with this observers event
 */
public interface EventObserver<T> {

  /**
   * Adds {@code listener} to this EventObserver, adding the same listener
   * a second time has no effect.
   * Adding a listener does not prevent it from being garbage collected.
   * @param listener the listener to add
   * @throws NullPointerException in case listener is null
   */
  void addListener(final EventListener listener);

  /**
   * Removes {@code listener} from this EventObserver
   * @param listener the listener to remove
   */
  void removeListener(final EventListener listener);

  /**
   * Adds {@code listener} to this EventObserver, adding the same listener
   * a second time has no effect.
   * Adding a listener does not prevent it from being garbage collected.
   * @param listener the listener to add
   * @throws NullPointerException in case listener is null
   */
  void addInfoListener(final EventInfoListener<T> listener);

  /**
   * Removes {@code listener} from this EventObserver
   * @param listener the listener to remove
   */
  void removeInfoListener(final EventInfoListener listener);
}
