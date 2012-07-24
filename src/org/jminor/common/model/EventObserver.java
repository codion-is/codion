/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

/**
 * Specifies an Event observer, responsible for notifying listeners when an event occurs.
 */
public interface EventObserver {

  /**
   * Adds <code>listener</code> to this EventObserver, adding the same listener
   * a second time has no effect.
   * @param listener the listener to add
   * @throws IllegalArgumentException in case listener is null
   */
  void addListener(final EventListener listener);

  /**
   * Removes <code>listener</code> from this EventObserver
   * @param listener the listener to remove
   */
  void removeListener(final EventListener listener);
}
