/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A synchronous event class
 */
public class Event implements ActionListener, Serializable {

  private final String name;

  private transient final ArrayList<ActionListener> listeners = new ArrayList<ActionListener>();
  private transient final ActionEvent defaultEvent = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "");
  private transient List<ActionListener> scheduledForAddition;
  private transient List<ActionListener> scheduledForRemoval;

  private transient boolean debug = false;//just for debugging purposes
  private transient boolean firing = false;

  public Event(final String name) {
    this.name = name;
  }

  /**
   * @return Value for property 'firing'.
   */
  public boolean isFiring() {
    return firing;
  }

  public void addListener(final ActionListener action) {
    if (!listeners.contains(action)) {
      if (isFiring()) {//delay the addition until firing is done to avoid changing the contents of listeners while iteratating
        if (scheduledForAddition == null)
          scheduledForAddition = new ArrayList<ActionListener>(1);
        scheduledForAddition.add(action);
      }
      else {
        listeners.add(action);
      }
    }
  }

  public void removeListener(final ActionListener action) {
    if (isFiring()) {
      if (scheduledForRemoval == null)
        scheduledForRemoval = new ArrayList<ActionListener>(1);
      scheduledForRemoval.add(action);
    }
    else {
      listeners.remove(action);
    }
  }

  public void fire() {
    fire(defaultEvent);
  }

  public final void fire(final ActionEvent event) {
    try {
      this.firing = true;
      if (debug && listeners.size() > 0) {
        if (event != null && event.getSource() instanceof Event && event.getSource() != this)
          System.out.println(" '-> " + this + " (" + Util.getListContentsAsString(listeners, false) + ") @ " + System.currentTimeMillis());
        else
          System.out.println(this + " (" + Util.getListContentsAsString(listeners, false) + ") @ " + System.currentTimeMillis());
      }
      for (final ActionListener listener : listeners)
        listener.actionPerformed(event);
    }
    finally {
      removeScheduled();
      addScheduled();
      this.firing = false;
    }
  }

  /** {@inheritDoc} */
  public void actionPerformed(final ActionEvent event) {
    fire(event);
  }

  /** {@inheritDoc} */
  public String toString() {
    return name;
  }

  private void addScheduled() {
    if (scheduledForAddition != null && scheduledForAddition.size() > 0) {
      listeners.addAll(scheduledForAddition);
      scheduledForAddition.clear();
    }
  }

  private void removeScheduled() {
    if (scheduledForRemoval != null && scheduledForRemoval.size() > 0) {
      listeners.removeAll(scheduledForRemoval);
      scheduledForRemoval.clear();
    }
  }
}