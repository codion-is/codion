/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A synchronous event class.
 */
public interface Event extends ActionListener, EventObserver {

  /**
   * Notifies all listeners
   */
  void fire();

  /**
   * Notifies all listeners
   * @param event the ActionEvent to use when notifying
   */
  void fire(final ActionEvent event);

  EventObserver getObserver();
}