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
 * Copyright (c) 2019 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.state;

import is.codion.common.Conjunction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

final class DefaultStateCombination implements State.Combination {

  private final DefaultStateObserver observer;
  private final List<StateCombinationListener> stateListeners = new ArrayList<>();
  private final Conjunction conjunction;

  DefaultStateCombination(Conjunction conjunction, StateObserver... states) {
    this(conjunction, states == null ? Collections.emptyList() : Arrays.asList(states));
  }

  DefaultStateCombination(Conjunction conjunction, Collection<? extends StateObserver> states) {
    this.observer = new DefaultStateObserver(this, false);
    this.conjunction = requireNonNull(conjunction);
    for (StateObserver state : requireNonNull(states)) {
      add(state);
    }
  }

  @Override
  public String toString() {
    synchronized (observer) {
      StringBuilder stringBuilder = new StringBuilder("Combination");
      stringBuilder.append(toString(conjunction)).append(observer);
      for (StateCombinationListener listener : stateListeners) {
        stringBuilder.append(", ").append(listener.state);
      }

      return stringBuilder.toString();
    }
  }

  @Override
  public Conjunction conjunction() {
    return conjunction;
  }

  @Override
  public void add(StateObserver state) {
    requireNonNull(state, "state");
    synchronized (observer) {
      if (!findListener(state).isPresent()) {
        boolean previousValue = get();
        stateListeners.add(new StateCombinationListener(state));
        observer.notifyObservers(get(), previousValue);
      }
    }
  }

  @Override
  public void remove(StateObserver state) {
    requireNonNull(state, "state");
    synchronized (observer) {
      boolean previousValue = get();
      findListener(state).ifPresent(listener -> {
        state.removeDataListener(listener);
        stateListeners.remove(listener);
        observer.notifyObservers(get(), previousValue);
      });
    }
  }

  @Override
  public Boolean get() {
    synchronized (observer) {
      return get(conjunction, null, false);
    }
  }

  @Override
  public boolean isNull() {
    return observer.isNull();
  }

  @Override
  public boolean isNotNull() {
    return observer.isNotNull();
  }

  @Override
  public boolean nullable() {
    return observer.nullable();
  }

  @Override
  public StateObserver not() {
    return observer.not();
  }

  @Override
  public boolean addListener(Runnable listener) {
    return observer.addListener(listener);
  }

  @Override
  public boolean removeListener(Runnable listener) {
    return observer.removeListener(listener);
  }

  @Override
  public boolean addDataListener(Consumer<Boolean> listener) {
    return observer.addDataListener(listener);
  }

  @Override
  public boolean removeDataListener(Consumer<Boolean> listener) {
    return observer.removeDataListener(listener);
  }

  @Override
  public boolean addWeakListener(Runnable listener) {
    return observer.addWeakListener(listener);
  }

  @Override
  public boolean removeWeakListener(Runnable listener) {
    return observer.removeWeakListener(listener);
  }

  @Override
  public boolean addWeakDataListener(Consumer<Boolean> listener) {
    return observer.addWeakDataListener(listener);
  }

  @Override
  public boolean removeWeakDataListener(Consumer<Boolean> listener) {
    return observer.removeWeakDataListener(listener);
  }

  private boolean get(Conjunction conjunction, StateObserver exclude, boolean excludeReplacement) {
    for (StateCombinationListener listener : stateListeners) {
      StateObserver state = listener.state;
      boolean value = state.equals(exclude) ? excludeReplacement : state.get();
      if (conjunction == Conjunction.AND) {
        if (!value) {
          return false;
        }
      }
      else if (value) {
        return true;
      }
    }

    return conjunction == Conjunction.AND;
  }

  private Optional<StateCombinationListener> findListener(StateObserver state) {
    return stateListeners.stream()
            .filter(listener -> listener.state.equals(state))
            .findFirst();
  }

  private final class StateCombinationListener implements Consumer<Boolean> {
    private final StateObserver state;

    private StateCombinationListener(StateObserver state) {
      this.state = state;
      this.state.addDataListener(this);
    }

    @Override
    public void accept(Boolean newValue) {
      observer.notifyObservers(get(), previousState(state, !newValue));
    }

    private boolean previousState(StateObserver excludeState, boolean previousValue) {
      synchronized (observer) {
        return get(conjunction, excludeState, previousValue);
      }
    }
  }

  private static String toString(Conjunction conjunction) {
    switch (conjunction) {
      case AND:
        return " and ";
      case OR:
        return " or ";
      default:
        throw new IllegalArgumentException("Unknown conjunction: " + conjunction);
    }
  }
}
