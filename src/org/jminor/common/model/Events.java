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
   * Instantiates a new Event.
   * @return a new Event
   */
  public static <T> Event<T> event() {
    return new DefaultEvent<T>();
  }

  /**
   * @param listener the info listener
   * @return a listener causing the given info listener to be fired with null info on each occurrence
   */
  public static EventListener listener(final EventInfoListener<?> listener) {
    return new EventListener() {
      @Override
      public void eventOccurred() {
        listener.eventOccurred(null);
      }
    };
  }

  /**
   * @param listener the listener
   * @return a info listener causing the given listener to be fired on each occurrence
   */
  public static <T> EventInfoListener<T> infoListener(final EventListener listener) {
    return new EventInfoListener<T>() {
      @Override
      public void eventOccurred(final T info) {
        listener.eventOccurred();
      }
    };
  }

  private static final class DefaultEvent<T> implements Event<T> {

    private volatile DefaultObserver<T> observer;

    /** {@inheritDoc} */
    @Override
    public void fire() {
      fire(null);
    }

    /** {@inheritDoc} */
    @Override
    public void fire(final T info) {
      if (observer != null && observer.hasListeners()) {
        for (final EventListener listener : observer.getListeners()) {
          listener.eventOccurred();
        }
        for (final EventInfoListener<T> infoListener : observer.getInfoListeners()) {
          infoListener.eventOccurred(info);
        }
      }
    }

    /** {@inheritDoc} */
    @Override
    public void eventOccurred() {
      eventOccurred(null);
    }

    @Override
    public void eventOccurred(final T info) {
      fire(info);
    }

    /** {@inheritDoc} */
    @Override
    public EventObserver<T> getObserver() {
      synchronized (this) {
        if (observer == null) {
          observer = new DefaultObserver<T>();
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
      getObserver().removeListener(listener);
    }

    /** {@inheritDoc} */
    @Override
    public void addInfoListener(final EventInfoListener<T> listener) {
      getObserver().addInfoListener(listener);
    }

    /** {@inheritDoc} */
    @Override
    public void removeInfoListener(final EventInfoListener listener) {
      getObserver().removeInfoListener(listener);
    }
  }

  private static final class DefaultObserver<T> implements EventObserver<T> {

    private final Collection<EventListener> listeners = new ArrayList<EventListener>(0);
    private final Collection<EventInfoListener<T>> infoListeners = new ArrayList<EventInfoListener<T>>(0);

    /** {@inheritDoc} */
    @Override
    public synchronized void addInfoListener(final EventInfoListener<T> listener) {
      Util.rejectNullValue(listener, "listener");
      if (!infoListeners.contains(listener)) {
        infoListeners.add(listener);
      }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void removeInfoListener(final EventInfoListener listener) {
      infoListeners.remove(listener);
    }

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

    private synchronized Collection<EventInfoListener<T>> getInfoListeners() {
      return new ArrayList<EventInfoListener<T>>(infoListeners);
    }

    private boolean hasListeners() {
      return !listeners.isEmpty() || !infoListeners.isEmpty();
    }
  }
}
