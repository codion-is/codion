/*
 * Copyright (c) 2004 - 2013, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

/**
 * An event listener receiving an information package on each firing
 * @param <T> the type of information provided by the event this listener is registered for
 */
public interface EventInfoListener<T> {

  /**
   * Called when an event this listener is registered for occurs
   * @param eventInfo information relating to the event
   */
  void eventOccurred(final T eventInfo);
}
