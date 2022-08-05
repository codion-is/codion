/*
 * Copyright (c) 2019 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.state;

import is.codion.common.Conjunction;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
      addState(state);
    }
  }

  @Override
  public String toString() {
    synchronized (observer) {
      StringBuilder stringBuilder = new StringBuilder("Combination");
      stringBuilder.append(toString(conjunction)).append(observer);
      for (StateCombinationListener listener : stateListeners) {
        stringBuilder.append(", ").append(listener.getState());
      }

      return stringBuilder.toString();
    }
  }

  @Override
  public Conjunction conjunction() {
    return conjunction;
  }

  @Override
  public void addState(StateObserver state) {
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
  public void removeState(StateObserver state) {
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
  public StateObserver reversedObserver() {
    return observer.reversedObserver();
  }

  @Override
  public void addListener(EventListener listener) {
    observer.addListener(listener);
  }

  @Override
  public void removeListener(EventListener listener) {
    observer.removeListener(listener);
  }

  @Override
  public void addDataListener(EventDataListener<Boolean> listener) {
    observer.addDataListener(listener);
  }

  @Override
  public void removeDataListener(EventDataListener<Boolean> listener) {
    observer.removeDataListener(listener);
  }

  private boolean get(Conjunction conjunction, StateObserver exclude, boolean excludeReplacement) {
    for (StateCombinationListener listener : stateListeners) {
      StateObserver state = listener.getState();
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
            .filter(listener -> listener.getState().equals(state))
            .findFirst();
  }

  private final class StateCombinationListener implements EventDataListener<Boolean> {
    private final StateObserver state;

    private StateCombinationListener(StateObserver state) {
      this.state = state;
      this.state.addDataListener(this);
    }

    @Override
    public void onEvent(Boolean newValue) {
      observer.notifyObservers(get(), getPreviousState(state, !newValue));
    }

    private boolean getPreviousState(StateObserver excludeState, boolean previousValue) {
      synchronized (observer) {
        return get(conjunction, excludeState, previousValue);
      }
    }

    private StateObserver getState() {
      return state;
    }
  }

  private static String toString(Conjunction conjunction) {
    switch (conjunction) {
      case AND: return " and ";
      case OR: return " or ";
      default: throw new IllegalArgumentException("Unknown conjunction: " + conjunction);
    }
  }
}
