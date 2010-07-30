/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.awt.event.ActionListener;

/**
 * User: darri
 * Date: 29.7.2010
 * Time: 11:54:38
 */
public interface EventObserver {

  /**
   * Adds <code>listener</code> to this EventObserver, adding the same listener
   * a second time has no effect.
   * @param listener the listener to add
   * @throws IllegalArgumentException in case listener is null
   */
  void addListener(final ActionListener listener);

  /**
   * Removes <code>listener</code> from this Event
   * @param listener the listener to remove
   */
  void removeListener(final ActionListener listener);
}
