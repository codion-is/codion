/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.state;

import is.codion.common.event.Event;
import is.codion.common.event.EventObserver;

import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

final class DefaultStateObserver implements StateObserver {

  private final Object lock = new Object();
  private final StateObserver observedState;
  private final boolean reversed;

  private Event<Boolean> stateChangedEvent;
  private DefaultStateObserver reversedObserver;

  DefaultStateObserver(StateObserver observedState, boolean reversed) {
    this.observedState = requireNonNull(observedState);
    this.reversed = reversed;
  }

  @Override
  public String toString() {
    return Boolean.toString(get());
  }

  @Override
  public Boolean get() {
    synchronized (lock) {
      return reversed ? !observedState.get() : observedState.get();
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
  public StateObserver reversed() {
    if (reversed) {
      return observedState;
    }
    synchronized (lock) {
      if (reversedObserver == null) {
        reversedObserver = new DefaultStateObserver(this, true);
      }

      return reversedObserver;
    }
  }

  @Override
  public void addListener(Runnable listener) {
    eventObserver().addListener(listener);
  }

  @Override
  public void removeListener(Runnable listener) {
    eventObserver().removeListener(listener);
  }

  @Override
  public void addDataListener(Consumer<Boolean> listener) {
    eventObserver().addDataListener(listener);
  }

  @Override
  public void removeDataListener(Consumer<Boolean> listener) {
    eventObserver().removeDataListener(listener);
  }

  @Override
  public void addWeakListener(Runnable listener) {
    eventObserver().addWeakListener(listener);
  }

  @Override
  public void removeWeakListener(Runnable listener) {
    eventObserver().removeWeakListener(listener);
  }

  @Override
  public void addWeakDataListener(Consumer<Boolean> listener) {
    eventObserver().addWeakDataListener(listener);
  }

  @Override
  public void removeWeakDataListener(Consumer<Boolean> listener) {
    eventObserver().removeWeakDataListener(listener);
  }

  void notifyObservers(boolean newValue, boolean previousValue) {
    synchronized (lock) {
      if (previousValue != newValue) {
        if (stateChangedEvent != null) {
          stateChangedEvent.accept(newValue);
        }
        if (reversedObserver != null) {
          reversedObserver.notifyObservers(previousValue, newValue);
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
