/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.state;

import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.event.EventObserver;

import static java.util.Objects.requireNonNull;

final class DefaultStateObserver implements StateObserver {

  private final Object lock = new Object();
  private final StateObserver stateObserver;
  private final boolean reversed;

  private Event<Boolean> stateChangedEvent;
  private DefaultStateObserver reversedStateObserver;

  DefaultStateObserver(StateObserver stateObserver, boolean reversed) {
    this.stateObserver = requireNonNull(stateObserver);
    this.reversed = reversed;
  }

  @Override
  public String toString() {
    return Boolean.toString(get());
  }

  @Override
  public Boolean get() {
    synchronized (lock) {
      return reversed ? !stateObserver.get() : stateObserver.get();
    }
  }

  @Override
  public boolean isNull() {
    return false;
  }

  @Override
  public boolean isNotNull() {
    return true;
  }

  @Override
  public boolean isNullable() {
    return false;
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
  public void addListener(EventListener listener) {
    getEventObserver().addListener(listener);
  }

  @Override
  public void removeListener(EventListener listener) {
    getEventObserver().removeListener(listener);
  }

  @Override
  public void addDataListener(EventDataListener<Boolean> listener) {
    getEventObserver().addDataListener(listener);
  }

  @Override
  public void removeDataListener(EventDataListener<Boolean> listener) {
    getEventObserver().removeDataListener(listener);
  }

  void notifyObservers(boolean newValue, boolean previousValue) {
    synchronized (lock) {
      if (previousValue != newValue) {
        if (stateChangedEvent != null) {
          stateChangedEvent.onEvent(newValue);
        }
        if (reversedStateObserver != null) {
          reversedStateObserver.notifyObservers(previousValue, newValue);
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
