/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.state;

import org.jminor.common.Conjunction;
import org.jminor.common.event.EventDataListener;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

final class DefaultAggregateState extends DefaultState implements State.AggregateState {

  private final Object lock = new Object();
  private final List<AggregateStateListener> stateListeners = new ArrayList<>();
  private final Conjunction conjunction;

  private DefaultAggregateState(final Conjunction conjunction) {
    this.conjunction = requireNonNull(conjunction, "conjunction");
  }

  DefaultAggregateState(final Conjunction conjunction, final StateObserver... states) {
    this(conjunction);
    if (states != null) {
      for (final StateObserver state : states) {
        addState(state);
      }
    }
  }

  @Override
  public String toString() {
    synchronized (lock) {
      final StringBuilder stringBuilder = new StringBuilder("Aggregate");
      stringBuilder.append(toString(conjunction)).append(super.toString());
      for (final AggregateStateListener listener : stateListeners) {
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
      if (findListener(state) == null) {
        final boolean value = get();
        stateListeners.add(new AggregateStateListener(state));
        ((DefaultStateObserver) getObserver()).notifyObservers(value, get());
      }
    }
  }

  @Override
  public void removeState(final StateObserver state) {
    requireNonNull(state, "state");
    synchronized (lock) {
      final boolean value = get();
      final AggregateStateListener listener = findListener(state);
      if (listener != null) {
        state.removeDataListener(listener);
        stateListeners.remove(listener);
        ((DefaultStateObserver) getObserver()).notifyObservers(value, get());
      }
    }
  }

  @Override
  public boolean get() {
    synchronized (lock) {
      return get(conjunction, null, false);
    }
  }

  @Override
  public void set(final boolean value) {
    throw new UnsupportedOperationException("The state of aggregate states can't be set");
  }

  private boolean get(final Conjunction conjunction, final StateObserver exclude, final boolean excludeReplacement) {
    for (final AggregateStateListener listener : stateListeners) {
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

  private AggregateStateListener findListener(final StateObserver state) {
    return stateListeners.stream().filter(listener -> listener.getState().equals(state)).findFirst().orElse(null);
  }

  private final class AggregateStateListener implements EventDataListener<Boolean> {
    private final StateObserver state;

    private AggregateStateListener(final StateObserver state) {
      this.state = state;
      this.state.addDataListener(this);
    }

    @Override
    public void eventOccurred(final Boolean newValue) {
      ((DefaultStateObserver) getObserver()).notifyObservers(getPreviousState(state, !newValue), get());
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
