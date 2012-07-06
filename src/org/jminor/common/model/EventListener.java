/*
 * Copyright (c) 2004 - 2012, Björn Darri Sigurðsson. All Rights Reserved.
 */

package org.jminor.common.model;

import java.awt.event.ActionEvent;

/**
 * An event listener
 */
public interface EventListener {

  /**
   * Called when an event this listener is registered for occurs
   * @param e the ActionEvent
   */
  public void eventOccurred(ActionEvent e);
}
