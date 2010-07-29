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

  public static Event event() {
    return new EventImpl();
  }

  final static class EventImpl implements Event {
    private final ActionEvent defaultActionEvent = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "");
    private EventObserverImpl observer;

    /**
     * Notifies all listeners
     */
    public void fire() {
      fire(defaultActionEvent);
    }

    /**
     * Notifies all listeners
     * @param event the ActionEvent to use when notifying
     */
    public void fire(final ActionEvent event) {
      for (final ActionListener listener : new ArrayList<ActionListener>(observer.getListeners())) {
        listener.actionPerformed(event);
      }
    }

    public void actionPerformed(final ActionEvent e) {
      fire(e);
    }

    public EventObserverImpl getObserver() {
      if (observer == null) {
        observer = new EventObserverImpl();
      }

      return observer;
    }

    /**
     * Adds <code>listener</code> to this Event, adding the same listener
     * a second time has no effect.
     * @param listener the listener to add
     * @throws IllegalArgumentException in case listener is null
     */
    public void addListener(final ActionListener listener) {
      getObserver().addListener(listener);
    }

    /**
     * Removes <code>listener</code> from this Event
     * @param listener the listener to remove
     */
    public void removeListener(final ActionListener listener) {
      observer.removeListener(listener);
    }

    public Collection<? extends ActionListener> getListeners() {
      return getObserver().getListeners();
    }
  }

  private static final class EventObserverImpl implements EventObserver {

    private final Set<ActionListener> listeners = new HashSet<ActionListener>();

    /**
     * Adds <code>listener</code> to this Event, adding the same listener
     * a second time has no effect.
     * @param listener the listener to add
     * @throws IllegalArgumentException in case listener is null
     */
    public void addListener(final ActionListener listener) {
      Util.rejectNullValue(listener, "listener");
      listeners.add(listener);
    }

    /**
     * Removes <code>listener</code> from this Event
     * @param listener the listener to remove
     */
    public void removeListener(final ActionListener listener) {
      listeners.remove(listener);
    }

    private Collection<? extends ActionListener> getListeners() {
      return Collections.unmodifiableCollection(listeners);
    }
  }
}
