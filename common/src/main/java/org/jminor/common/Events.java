/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;

/**
 * A factory class for Event objects.
 * @see Event
 */
public final class Events {

  private Events() {}

  /**
   * Instantiates a new Event.
   * @param <T> the type of info propagated to listeners on event firing
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
    return () -> listener.eventOccurred(null);
  }

  /**
   * @param <T> the type of info propagated to listeners on event firing
   * @param listener the listener
   * @return a info listener causing the given listener to be fired on each occurrence
   */
  public static <T> EventInfoListener<T> infoListener(final EventListener listener) {
    return info -> listener.eventOccurred();
  }

  private static final class DefaultEvent<T> implements Event<T> {

    private final Object lock = new Object();
    private volatile DefaultObserver<T> observer;

    @Override
    public void fire() {
      fire(null);
    }

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

    @Override
    public void eventOccurred() {
      eventOccurred(null);
    }

    @Override
    public void eventOccurred(final T info) {
      fire(info);
    }

    @Override
    public EventObserver<T> getObserver() {
      synchronized (lock) {
        if (observer == null) {
          observer = new DefaultObserver<>();
        }

        return observer;
      }
    }

    @Override
    public void addListener(final EventListener listener) {
      getObserver().addListener(listener);
    }

    @Override
    public void removeListener(final EventListener listener) {
      getObserver().removeListener(listener);
    }

    @Override
    public void addInfoListener(final EventInfoListener<T> listener) {
      getObserver().addInfoListener(listener);
    }

    @Override
    public void removeInfoListener(final EventInfoListener listener) {
      getObserver().removeInfoListener(listener);
    }
  }

  private static final class DefaultObserver<T> implements EventObserver<T> {

    private final Object lock = new Object();
    private Collection<EventListener> listeners;
    private Collection<EventInfoListener<T>> infoListeners;

    @Override
    public void addInfoListener(final EventInfoListener<T> listener) {
      synchronized (lock) {
        getInfoListeners().add(Objects.requireNonNull(listener, "listener"));
      }
    }

    @Override
    public void removeInfoListener(final EventInfoListener listener) {
      synchronized (lock) {
        getInfoListeners().remove(listener);
      }
    }

    @Override
    public void addListener(final EventListener listener) {
      synchronized (lock) {
        getListeners().add(Objects.requireNonNull(listener, "listener"));
      }
    }

    @Override
    public void removeListener(final EventListener listener) {
      synchronized (lock) {
        getListeners().remove(listener);
      }
    }

    private Collection<EventListener> getEventListeners() {
      synchronized (lock) {
        if (listeners == null) {
          return Collections.emptyList();
        }

        return new ArrayList<>(listeners);
      }
    }

    private Collection<EventInfoListener<T>> getEventInfoListeners() {
      synchronized (lock) {
        if (infoListeners == null) {
          return Collections.emptyList();
        }

        return new ArrayList<>(infoListeners);
      }
    }

    private boolean hasListeners() {
      synchronized (lock) {
        return (listeners != null && !listeners.isEmpty()) || (infoListeners != null && !infoListeners.isEmpty());
      }
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
