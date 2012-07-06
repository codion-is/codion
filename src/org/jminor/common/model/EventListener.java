/*
 * Copyright (c) 2004 - 2012, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

/**
 * An event listener
 * @param <T> the type of information provided by the event this listener is registered for
 */
public interface EventListener<T> {

  /**
   * Called when an event this listener is registered for occurs
   */
  public void eventOccurred();

  /**
   * Called when an event this listener is registered for occurs
   * @param eventInfo information relating to the event
   */
  public void eventOccurred(final T eventInfo);
}
