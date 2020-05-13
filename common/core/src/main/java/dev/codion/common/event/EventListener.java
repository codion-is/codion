/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.event;

/**
 * An event listener.
 */
public interface EventListener {

  /**
   * Called when the event occurs.
   */
  void onEvent();
}
