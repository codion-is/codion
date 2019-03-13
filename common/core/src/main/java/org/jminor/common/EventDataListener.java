/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

/**
 * An event listener receiving some data on each event occurrence
 * @param <T> the data type provided by the event this listener is registered for
 */
public interface EventDataListener<T> {

  /**
   * Called when an event this listener is registered for occurs
   * @param data information relating to the event
   */
  void eventOccurred(final T data);
}
