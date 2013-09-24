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
  public static <T> Event<T> event() {
    return new DefaultEvent<T>();
  }

  public static EventListener listener(final EventInfoListener<?> listener) {
    return new EventListener() {
      @Override
      public void eventOccurred() {
        listener.eventOccurred(null);
      }
    };
  }

  public static <T> EventInfoListener<T> infoListener(final EventListener listener) {
    return new EventInfoListener<T>() {
      @Override
      public void eventOccurred(final T eventInfo) {
        listener.eventOccurred();
      }
    };
  }

  private static final class DefaultEvent<T> implements Event<T> {

    private volatile DefaultObserver observer;

    /** {@inheritDoc} */
    @Override
    public void fire() {
      fire(null);
    }

    /** {@inheritDoc} */
    @Override
    public void fire(final T eventInfo) {
      if (observer != null && observer.hasListeners()) {
        for (final Object listener : observer.getListeners()) {
          if (listener instanceof EventListener) {
            ((EventListener) listener).eventOccurred();
          }
          else if (listener instanceof EventInfoListener) {
            ((EventInfoListener<T>) listener).eventOccurred(eventInfo);
          }
        }
      }
    }

    /** {@inheritDoc} */
    @Override
    public void eventOccurred() {
      eventOccurred(null);
    }

    @Override
    public void eventOccurred(final T eventInfo) {
      fire(eventInfo);
    }

    /** {@inheritDoc} */
    @Override
    public EventObserver getObserver() {
      synchronized (this) {
        if (observer == null) {
          observer = new DefaultObserver();
        }
      }

      return observer;
    }

    @Override
    public EventInfoObserver<T> getInfoObserver() {
      return (EventInfoObserver<T>) getObserver();
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

    /** {@inheritDoc} */
    @Override
    public void addInfoListener(final EventInfoListener<T> listener) {
      getInfoObserver().addInfoListener(listener);
    }

    /** {@inheritDoc} */
    @Override
    public void removeInfoListener(final EventInfoListener listener) {
      if (observer != null) {
        observer.removeInfoListener(listener);
      }
    }
  }

  private static final class DefaultObserver implements EventObserver, EventInfoObserver {

    private final Collection listeners = new ArrayList();

    /** {@inheritDoc} */
    @Override
    public void addInfoListener(final EventInfoListener listener) {
      doAddListener(listener);
    }

    /** {@inheritDoc} */
    @Override
    public void removeInfoListener(final EventInfoListener listener) {
      doRemoveListener(listener);
    }

    /** {@inheritDoc} */
    @Override
    public void addListener(final EventListener listener) {
      doAddListener(listener);
    }

    /** {@inheritDoc} */
    @Override
    public void removeListener(final EventListener listener) {
      doRemoveListener(listener);
    }

    private synchronized Collection<EventInfoListener> getListeners() {
      return new ArrayList<EventInfoListener>(listeners);
    }

    private boolean hasListeners() {
      return !listeners.isEmpty();
    }

    private void doAddListener(final Object listener) {
      Util.rejectNullValue(listener, "listener");
      if (!listeners.contains(listener)) {
        listeners.add(listener);
      }
    }

    private void doRemoveListener(final Object listener) {
      listeners.remove(listener);
    }
  }
}
