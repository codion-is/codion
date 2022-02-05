/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.state;

import is.codion.common.Conjunction;
import is.codion.common.event.EventDataListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

final class DefaultStateCombination extends DefaultState implements State.Combination {

  private final Object lock = new Object();
  private final List<StateCombinationListener> stateListeners = new ArrayList<>();
  private final Conjunction conjunction;

  DefaultStateCombination(final Conjunction conjunction, final StateObserver... states) {
    this(conjunction, states == null ? Collections.emptyList() : Arrays.asList(states));
  }

  DefaultStateCombination(final Conjunction conjunction, final Collection<? extends StateObserver> states) {
    this.conjunction = requireNonNull(conjunction);
    for (final StateObserver state : requireNonNull(states)) {
      addState(state);
    }
  }

  @Override
  public String toString() {
    synchronized (lock) {
      final StringBuilder stringBuilder = new StringBuilder("Combination");
      stringBuilder.append(toString(conjunction)).append(super.toString());
      for (final StateCombinationListener listener : stateListeners) {
        stringBuilder.append(", ").append(listener.getState());
      }

      return stringBuilder.toString();
    }
  }

  @Override
  public Conjunction getConjunction() {
    return conjunction;
  }

  @Override
  public void addState(final StateObserver state) {
    requireNonNull(state, "state");
    synchronized (lock) {
      if (!findListener(state).isPresent()) {
        final boolean previousValue = get();
        stateListeners.add(new StateCombinationListener(state));
        ((DefaultStateObserver) getObserver()).notifyObservers(get(), previousValue);
      }
    }
  }

  @Override
  public void removeState(final StateObserver state) {
    requireNonNull(state, "state");
    synchronized (lock) {
      final boolean previousValue = get();
      findListener(state).ifPresent(listener -> {
        state.removeDataListener(listener);
        stateListeners.remove(listener);
        ((DefaultStateObserver) getObserver()).notifyObservers(get(), previousValue);
      });
    }
  }

  @Override
  public Boolean get() {
    synchronized (lock) {
      return get(conjunction, null, false);
    }
  }

  @Override
  public void set(final Boolean value) {
    throw new UnsupportedOperationException("The state of state combination can't be set");
  }

  private boolean get(final Conjunction conjunction, final StateObserver exclude, final boolean excludeReplacement) {
    for (final StateCombinationListener listener : stateListeners) {
      final StateObserver state = listener.getState();
      final boolean value = state.equals(exclude) ? excludeReplacement : state.get();
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

  private Optional<StateCombinationListener> findListener(final StateObserver state) {
    return stateListeners.stream()
            .filter(listener -> listener.getState().equals(state))
            .findFirst();
  }

  private final class StateCombinationListener implements EventDataListener<Boolean> {
    private final StateObserver state;

    private StateCombinationListener(final StateObserver state) {
      this.state = state;
      this.state.addDataListener(this);
    }

    @Override
    public void onEvent(final Boolean newValue) {
      ((DefaultStateObserver) getObserver()).notifyObservers(get(), getPreviousState(state, !newValue));
    }

    private boolean getPreviousState(final StateObserver excludeState, final boolean previousValue) {
      synchronized (lock) {
        return get(conjunction, excludeState, previousValue);
      }
    }

    private StateObserver getState() {
      return state;
    }
  }

  private static String toString(final Conjunction conjunction) {
    switch (conjunction) {
      case AND: return " and ";
      case OR: return " or ";
      default: throw new IllegalArgumentException("Unknown conjunction: " + conjunction);
    }
  }
}
