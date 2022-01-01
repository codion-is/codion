/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.state;

import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.event.EventObserver;

import java.util.Objects;
import java.util.Optional;

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
  public Boolean get() {
    synchronized (lock) {
      return reversed ? !stateObserver.get() : stateObserver.get();
    }
  }

  @Override
  public Optional<Boolean> toOptional() {
    return Optional.of(stateObserver.get());
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
  public boolean equalTo(final Boolean value) {
    return Objects.equals(stateObserver.get(), value);
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

  void notifyObservers(final boolean newValue, final boolean previousValue) {
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
