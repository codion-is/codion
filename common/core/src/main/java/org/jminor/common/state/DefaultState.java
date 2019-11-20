/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.state;

import org.jminor.common.event.EventDataListener;
import org.jminor.common.event.EventListener;

class DefaultState implements State {

  private final Object lock = new Object();
  private StateObserver observer;
  private boolean value;

  DefaultState() {
    this(false);
  }

  DefaultState(final boolean value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return Boolean.toString(value);
  }

  @Override
  public void set(final boolean value) {
    synchronized (lock) {
      if (this.value != value) {
        final boolean previousValue = this.value;
        this.value = value;
        ((DefaultStateObserver) getObserver()).notifyObservers(previousValue, value);
      }
    }
  }

  @Override
  public boolean get() {
    synchronized (lock) {
      return value;
    }
  }

  @Override
  public final StateObserver getObserver() {
    synchronized (lock) {
      if (observer == null) {
        observer = new DefaultStateObserver(this, false);
      }

      return observer;
    }
  }

  @Override
  public final void addListener(final EventListener listener) {
    getObserver().addListener(listener);
  }

  @Override
  public final void removeListener(final EventListener listener) {
    getObserver().removeListener(listener);
  }

  @Override
  public void addDataListener(final EventDataListener<Boolean> listener) {
    getObserver().addDataListener(listener);
  }

  @Override
  public void removeDataListener(final EventDataListener listener) {
    getObserver().removeDataListener(listener);
  }

  @Override
  public final StateObserver getReversedObserver() {
    return getObserver().getReversedObserver();
  }
}
