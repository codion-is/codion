/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
  public StateObserver reversedObserver() {
    if (reversed) {
      return stateObserver;
    }
    synchronized (lock) {
      if (reversedStateObserver == null) {
        reversedStateObserver = new DefaultStateObserver(this, true);
      }

      return reversedStateObserver;
    }
  }

  @Override
  public void addListener(EventListener listener) {
    eventObserver().addListener(listener);
  }

  @Override
  public void removeListener(EventListener listener) {
    eventObserver().removeListener(listener);
  }

  @Override
  public void addDataListener(EventDataListener<Boolean> listener) {
    eventObserver().addDataListener(listener);
  }

  @Override
  public void removeDataListener(EventDataListener<Boolean> listener) {
    eventObserver().removeDataListener(listener);
  }

  @Override
  public void addWeakListener(EventListener listener) {
    eventObserver().addWeakListener(listener);
  }

  @Override
  public void removeWeakListener(EventListener listener) {
    eventObserver().removeWeakListener(listener);
  }

  @Override
  public void addWeakDataListener(EventDataListener<Boolean> listener) {
    eventObserver().addWeakDataListener(listener);
  }

  @Override
  public void removeWeakDataListener(EventDataListener<Boolean> listener) {
    eventObserver().removeWeakDataListener(listener);
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

  private EventObserver<Boolean> eventObserver() {
    synchronized (lock) {
      if (stateChangedEvent == null) {
        stateChangedEvent = Event.event();
      }

      return stateChangedEvent.observer();
    }
  }
}
