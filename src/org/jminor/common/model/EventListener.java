/*
 * Copyright (c) 2004 - 2012, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

/**
 * An event listener
 */
public interface EventListener {

  /**
   * Called when an event this listener is registered for occurs
   */
  void eventOccurred();
}
