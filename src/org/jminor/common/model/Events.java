/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * User: Björn Darri
 * Date: 29.7.2010
 * Time: 20:26:11
 */
public final class Events {

  private Events() {}

  public static Event event() {
    return new EventImpl();
  }

  static final class EventImpl implements Event {

    private final ActionEvent defaultActionEvent = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "");
    private volatile EventObserverImpl observer;

    public void fire() {
      fire(defaultActionEvent);
    }

    public void fire(final ActionEvent event) {
      if (observer != null) {
        for (final ActionListener listener : new ArrayList<ActionListener>(observer.getListeners())) {
          listener.actionPerformed(event);
        }
      }
    }

    public void actionPerformed(final ActionEvent e) {
      fire(e);
    }

    public EventObserver getObserver() {
      if (observer == null) {
        synchronized (defaultActionEvent) {
          observer = new EventObserverImpl();
        }
      }

      return observer;
    }

    public void addListener(final ActionListener listener) {
      getObserver().addListener(listener);
    }

    public void removeListener(final ActionListener listener) {
      if (observer != null) {
        observer.removeListener(listener);
      }
    }

    public Collection<? extends ActionListener> getListeners() {
      return ((EventObserverImpl) getObserver()).getListeners();
    }
  }

  private static final class EventObserverImpl implements EventObserver {

    private final Set<ActionListener> listeners = new HashSet<ActionListener>();

    public void addListener(final ActionListener listener) {
      Util.rejectNullValue(listener, "listener");
      synchronized (listeners) {
        listeners.add(listener);
      }
    }

    public synchronized void removeListener(final ActionListener listener) {
      synchronized (listeners) {
        listeners.remove(listener);
      }
    }

    private synchronized Collection<? extends ActionListener> getListeners() {
      synchronized (listeners) {
        return Collections.unmodifiableCollection(listeners);
      }
    }
  }
}
