/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.state;

import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.event.EventObserver;

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
  public void removeDataListener(final EventDataListener<Boolean> listener) {
    getEventObserver().removeDataListener(listener);
  }

  void notifyObservers(final boolean previousValue, final boolean newValue) {
    synchronized (lock) {
      if (previousValue != newValue) {
        if (stateChangedEvent != null) {
          stateChangedEvent.onEvent(newValue);
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
        stateChangedEvent = Event.event();
      }

      return stateChangedEvent.getObserver();
    }
  }
}
