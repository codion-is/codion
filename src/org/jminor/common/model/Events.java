/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
    return new EventImpl();
  }

  static final class EventImpl implements Event {

    private final ActionEvent defaultActionEvent = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "");
    private volatile EventObserverImpl observer;

    /** {@inheritDoc} */
    @Override
    public void fire() {
      fire(defaultActionEvent);
    }

    /** {@inheritDoc} */
    @Override
    public void fire(final ActionEvent event) {
      if (observer != null) {
        for (final ActionListener listener : observer.getListeners()) {
          listener.actionPerformed(event);
        }
      }
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(final ActionEvent e) {
      fire(e);
    }

    /** {@inheritDoc} */
    @Override
    public EventObserver getObserver() {
      synchronized (defaultActionEvent) {
        if (observer == null) {
          observer = new EventObserverImpl();
        }
      }

      return observer;
    }

    /** {@inheritDoc} */
    @Override
    public void addListener(final ActionListener listener) {
      getObserver().addListener(listener);
    }

    /** {@inheritDoc} */
    @Override
    public void removeListener(final ActionListener listener) {
      if (observer != null) {
        observer.removeListener(listener);
      }
    }
  }

  private static final class EventObserverImpl implements EventObserver {

    private final Collection<ActionListener> listeners = new ArrayList<ActionListener>();

    /** {@inheritDoc} */
    @Override
    public void addListener(final ActionListener listener) {
      Util.rejectNullValue(listener, "listener");
      synchronized (listeners) {
        if (!listeners.contains(listener)) {
          listeners.add(listener);
        }
      }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void removeListener(final ActionListener listener) {
      synchronized (listeners) {
        listeners.remove(listener);
      }
    }

    private synchronized Collection<ActionListener> getListeners() {
      synchronized (listeners) {
        return new ArrayList<ActionListener>(listeners);
      }
    }
  }
}
