/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.awt.event.ActionListener;
import java.util.Collection;

/**
 * User: darri
 * Date: 29.7.2010
 * Time: 11:54:38
 */
public interface EventObserver {

  void addListener(final ActionListener listener);

  /**
   * Removes <code>listener</code> from this Event
   * @param listener the listener to remove
   */
  void removeListener(final ActionListener listener);

  Collection<? extends ActionListener> getListeners();
}
