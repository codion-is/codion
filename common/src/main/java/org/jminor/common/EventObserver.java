/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

/**
 * Specifies an Event observer, managing listeners for an Event.
 * @param <T> the type of info propagated with events
 */
public interface EventObserver<T> {

  /**
   * Adds <code>listener</code> to this EventObserver, adding the same listener
   * a second time has no effect.
   * Adding a listener does not prevent it from being garbage collected.
   * @param listener the listener to add
   * @throws NullPointerException in case listener is null
   */
  void addListener(final EventListener listener);

  /**
   * Removes <code>listener</code> from this EventObserver
   * @param listener the listener to remove
   */
  void removeListener(final EventListener listener);

  /**
   * Adds <code>listener</code> to this EventObserver, adding the same listener
   * a second time has no effect.
   * Adding a listener does not prevent it from being garbage collected.
   * @param listener the listener to add
   * @throws IllegalArgumentException in case listener is null
   */
  void addInfoListener(final EventInfoListener<T> listener);

  /**
   * Removes <code>listener</code> from this EventObserver
   * @param listener the listener to remove
   */
  void removeInfoListener(final EventInfoListener listener);
}
