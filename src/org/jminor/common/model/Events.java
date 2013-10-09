/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

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
    return new DefaultEvent<>();
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
        for (final EventListener listener : observer.getEventListeners()) {
          listener.eventOccurred();
        }
        for (final EventInfoListener<T> infoListener : observer.getEventInfoListeners()) {
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
          observer = new DefaultObserver<>();
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

    private Collection<EventListener> listeners;
    private Collection<EventInfoListener<T>> infoListeners;

    /** {@inheritDoc} */
    @Override
    public synchronized void addInfoListener(final EventInfoListener<T> listener) {
      Util.rejectNullValue(listener, "listener");
      getInfoListeners().add(listener);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void removeInfoListener(final EventInfoListener listener) {
      getInfoListeners().remove(listener);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void addListener(final EventListener listener) {
      Util.rejectNullValue(listener, "listener");
      getListeners().add(listener);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void removeListener(final EventListener listener) {
      getListeners().remove(listener);
    }

    private synchronized Collection<EventListener> getEventListeners() {
      if (listeners == null) {
        return Collections.emptyList();
      }

      return new ArrayList<>(listeners);
    }

    private synchronized Collection<EventInfoListener<T>> getEventInfoListeners() {
      if (infoListeners == null) {
        return Collections.emptyList();
      }

      return new ArrayList<>(infoListeners);
    }

    private synchronized boolean hasListeners() {
      return (listeners != null && !listeners.isEmpty()) || (infoListeners != null && !infoListeners.isEmpty());
    }

    private Collection<EventListener> getListeners() {
      if (listeners == null) {
        listeners = new LinkedHashSet<>(1);
      }

      return listeners;
    }

    private Collection<EventInfoListener<T>> getInfoListeners() {
      if (infoListeners == null) {
        infoListeners = new LinkedHashSet<>(1);
      }

      return infoListeners;
    }
  }
}
