/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.state;

import org.jminor.common.event.Event;
import org.jminor.common.event.EventDataListener;
import org.jminor.common.event.EventListener;
import org.jminor.common.event.EventObserver;
import org.jminor.common.event.Events;

final class DefaultStateObserver implements StateObserver {

  private final Object lock = new Object();
  private final StateObserver stateObserver;
  private final boolean reversed;

  private Event<Boolean> stateChangedEvent;
  private DefaultStateObserver reversedStateObserver;

  DefaultStateObserver(final StateObserver stateObserver, final boolean reversed) {
    this.stateObserver = stateObserver;
    this.reversed = reversed;
  }

  @Override
  public boolean get() {
    synchronized (lock) {
      return reversed ? !stateObserver.get() : stateObserver.get();
    }
  }

  @Override
  public StateObserver getReversedObserver() {
    synchronized (lock) {
      if (reversedStateObserver == null) {
        reversedStateObserver = new DefaultStateObserver(this, true);
      }

      return reversedStateObserver;
    }
  }

  @Override
  public void addListener(final EventListener listener) {
    getEventObserver().addListener(listener);
  }

  @Override
  public void removeListener(final EventListener listener) {
    getEventObserver().removeListener(listener);
  }

  @Override
  public void addDataListener(final EventDataListener<Boolean> listener) {
    getEventObserver().addDataListener(listener);
  }

  @Override
  public void removeDataListener(final EventDataListener listener) {
    getEventObserver().removeDataListener(listener);
  }

  void notifyObservers(final boolean previousValue, final boolean newValue) {
    synchronized (lock) {
      if (previousValue != newValue) {
        if (stateChangedEvent != null) {
          stateChangedEvent.fire(newValue);
        }
        if (reversedStateObserver != null) {
          reversedStateObserver.notifyObservers(newValue, previousValue);
        }
      }
    }
  }

  private EventObserver<Boolean> getEventObserver() {
    synchronized (lock) {
      if (stateChangedEvent == null) {
        stateChangedEvent = Events.event();
      }

      return stateChangedEvent.getObserver();
    }
  }
}
