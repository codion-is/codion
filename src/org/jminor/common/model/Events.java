/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A factory class for Event objects.
 * @see Event
 */
public final class Events {

  private Events() {}

  /**
   * Instantiates a new Event object.
   * @return a new Event
   */
  public static Event event() {
    return new DefaultEvent();
  }

  private static final class DefaultEvent implements Event {

    private volatile DefaultEventObserver observer;

    /** {@inheritDoc} */
    @Override
    public void fire() {
      fire(null);
    }

    /** {@inheritDoc} */
    @Override
    public void fire(final Object eventInfo) {
      if (observer != null && observer.hasListeners()) {
        for (final EventListener listener : observer.getListeners()) {
          listener.eventOccurred(eventInfo);
        }
      }
    }

    /** {@inheritDoc} */
    @Override
    public void eventOccurred() {
      eventOccurred(null);
    }

    /** {@inheritDoc} */
    @Override
    public void eventOccurred(final Object eventInfo) {
      fire(eventInfo);
    }

    /** {@inheritDoc} */
    @Override
    public EventObserver getObserver() {
      synchronized (this) {
        if (observer == null) {
          observer = new DefaultEventObserver();
        }
      }

      return observer;
    }

    /** {@inheritDoc} */
    @Override
    public void addListener(final EventListener listener) {
      getObserver().addListener(listener);
    }

    /** {@inheritDoc} */
    @Override
    public void removeListener(final EventListener listener) {
      if (observer != null) {
        observer.removeListener(listener);
      }
    }
  }

  private static final class DefaultEventObserver implements EventObserver {

    private final Collection<EventListener> listeners = new ArrayList<EventListener>();

    /** {@inheritDoc} */
    @Override
    public synchronized void addListener(final EventListener listener) {
      Util.rejectNullValue(listener, "listener");
      if (!listeners.contains(listener)) {
        listeners.add(listener);
      }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void removeListener(final EventListener listener) {
      listeners.remove(listener);
    }

    private synchronized Collection<EventListener> getListeners() {
      return new ArrayList<EventListener>(listeners);
    }

    private boolean hasListeners() {
      return !listeners.isEmpty();
    }
  }
}
