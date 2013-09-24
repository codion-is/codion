/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

/**
 * Specifies an Event observer, managing listeners for an Event receiving information on each firing.
 * @param <T> the type of information supplied by the event
 */
public interface EventInfoObserver<T> {

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
