/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

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
    return new EventImpl();
  }

  private static final class EventImpl implements Event {

    private volatile EventObserverImpl observer;

    /** {@inheritDoc} */
    @Override
    public void fire() {
      fire(null);
    }

    /** {@inheritDoc} */
    @Override
    public void fire(final Object eventInfo) {
      if (observer != null) {
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
          observer = new EventObserverImpl();
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

  private static final class EventObserverImpl implements EventObserver {

    private final List<WeakReference<EventListener>> listeners = new ArrayList<WeakReference<EventListener>>();

    /** {@inheritDoc} */
    @Override
    public synchronized void addListener(final EventListener listener) {
      Util.rejectNullValue(listener, "listener");
      final ListIterator<WeakReference<EventListener>> iterator = listeners.listIterator();
      while (iterator.hasNext()) {
        final EventListener referencedListener = iterator.next().get();
        if (referencedListener == null) {
          iterator.remove();
        }
        else if (referencedListener == listener) {
          return;
        }
      }
      listeners.add(new WeakReference<EventListener>(listener));
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void removeListener(final EventListener listener) {
      final ListIterator<WeakReference<EventListener>> iterator = listeners.listIterator();
      while (iterator.hasNext()) {
        final EventListener referencedListener = iterator.next().get();
        if (referencedListener == null || referencedListener == listener) {
          iterator.remove();
        }
      }
    }

    private synchronized Collection<EventListener> getListeners() {
      final ListIterator<WeakReference<EventListener>> iterator = listeners.listIterator();
      final List<EventListener> eventListeners = new ArrayList<EventListener>(listeners.size());
      while (iterator.hasNext()) {
        final EventListener listener = iterator.next().get();
        if (listener == null) {
          iterator.remove();
        }
        else {
          eventListeners.add(listener);
        }
      }

      return eventListeners;
    }
  }
}
