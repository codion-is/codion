/*
 * Copyright (c) 2004 - 2012, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

/**
 * An adapter class implementation of EventListener.
 * N.B. you must override one or both of the available methods, otherwise it will get
 * stuck in an endless loop.
 * @param <T> the type of information provided by the event this listener is registered for
 */
public class EventAdapter<T> implements EventListener<T> {

  /** {@inheritDoc} */
  @Override
  public void eventOccurred() {
    eventOccurred(null);
  }

  /** {@inheritDoc} */
  @Override
  public void eventOccurred(final T eventInfo) {
    eventOccurred();
  }
}
