/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.common.state;

import is.codion.common.event.Event;
import is.codion.common.event.EventObserver;

import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

final class DefaultStateObserver implements StateObserver {

  private final Object lock = new Object();
  private final StateObserver observedState;
  private final boolean not;

  private Event<Boolean> stateChangedEvent;
  private DefaultStateObserver notObserver;

  DefaultStateObserver(StateObserver observedState, boolean not) {
    this.observedState = requireNonNull(observedState);
    this.not = not;
  }

  @Override
  public String toString() {
    return Boolean.toString(get());
  }

  @Override
  public Boolean get() {
    synchronized (lock) {
      return not ? !observedState.get() : observedState.get();
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
  public StateObserver not() {
    if (not) {
      return observedState;
    }
    synchronized (lock) {
      if (notObserver == null) {
        notObserver = new DefaultStateObserver(this, true);
      }

      return notObserver;
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
        if (notObserver != null) {
          notObserver.notifyObservers(previousValue, newValue);
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
