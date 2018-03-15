/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

/**
 * A factory class for {@link State} objects.
 * @see State
 */
public final class States {

  private States() {}

  /**
   * Instantiates a new inactive State object.
   * @return a new State
   */
  public static State state() {
    return state(false);
  }

  /**
   * Instantiates a new State object.
   * @param initialState the initial state
   * @return a new State
   */
  public static State state(final boolean initialState) {
    return new DefaultState(initialState);
  }

  /**
   * Instantiates a new State.AggregateState object.
   * @param conjunction the conjunction to use
   * @param stateObservers the state observers to base this aggregate state on
   * @return a new State.AggregateState
   */
  public static State.AggregateState aggregateState(final Conjunction conjunction, final StateObserver... stateObservers) {
    return new DefaultAggregateState(conjunction, stateObservers);
  }

  /**
   * Instantiates a new State.Group object, which guarantees that only a single
   * state within the group is active at a time
   * @param states the states to add to the group initially, not required
   * @return a new State.Group
   * @see State.Group
   */
  public static State.Group group(final State... states) {
    return new DefaultGroup(states);
  }

  private static class DefaultState implements State {

    private final Object lock = new Object();
    private StateObserver observer;
    private boolean active = false;

    DefaultState() {
      this(false);
    }

    DefaultState(final boolean initialState) {
      this.active = initialState;
    }

    @Override
    public String toString() {
      return active ? "active" : "inactive";
    }

    @Override
    public void setActive(final boolean active) {
      synchronized (lock) {
        if (this.active != active) {
          final boolean previousValue = this.active;
          this.active = active;
          ((DefaultStateObserver) getObserver()).notifyObservers(previousValue, active);
        }
      }
    }

    @Override
    public boolean isActive() {
      synchronized (lock) {
        return active;
      }
    }

    @Override
    public final StateObserver getObserver() {
      synchronized (lock) {
        if (observer == null) {
          observer = new DefaultStateObserver(this, false);
        }

        return observer;
      }
    }

    @Override
    public final EventObserver<Boolean> getChangeObserver() {
      return getObserver().getChangeObserver();
    }

    @Override
    public final void addListener(final EventListener listener) {
      getObserver().addListener(listener);
    }

    @Override
    public final void removeListener(final EventListener listener) {
      getObserver().removeListener(listener);
    }

    @Override
    public void addDataListener(final EventDataListener<Boolean> listener) {
      getObserver().addDataListener(listener);
    }

    @Override
    public void removeDataListener(final EventDataListener listener) {
      getObserver().removeDataListener(listener);
    }

    @Override
    public final StateObserver getReversedObserver() {
      return getObserver().getReversedObserver();
    }
  }

  private static final class DefaultAggregateState extends DefaultState implements State.AggregateState {

    private final Object lock = new Object();
    private final List<AggregateStateListener> stateListeners = new ArrayList<>();
    private final Conjunction conjunction;

    private DefaultAggregateState(final Conjunction conjunction) {
      this.conjunction = Objects.requireNonNull(conjunction, "conjunction");
    }

    private DefaultAggregateState(final Conjunction conjunction, final StateObserver... states) {
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
        stringBuilder.append(conjunction.toString()).append(super.toString());
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
      Objects.requireNonNull(state, "state");
      synchronized (lock) {
        if (findListener(state) == null) {
          final boolean wasActive = isActive();
          stateListeners.add(new AggregateStateListener(state));
          ((DefaultStateObserver) getObserver()).notifyObservers(wasActive, isActive());
        }
      }
    }

    @Override
    public void removeState(final StateObserver state) {
      Objects.requireNonNull(state, "state");
      synchronized (lock) {
        final boolean wasActive = isActive();
        final AggregateStateListener listener = findListener(state);
        if (listener != null) {
          state.removeDataListener(listener);
          stateListeners.remove(listener);
          ((DefaultStateObserver) getObserver()).notifyObservers(wasActive, isActive());
        }
      }
    }

    @Override
    public boolean isActive() {
      synchronized (lock) {
        return isActive(conjunction, null, false);
      }
    }

    @Override
    public void setActive(final boolean active) {
      throw new UnsupportedOperationException("The state of aggregate states can't be set");
    }

    private boolean isActive(final Conjunction conjunction, final StateObserver exclude, final boolean excludeReplacement) {
      for (final AggregateStateListener listener : stateListeners) {
        final StateObserver state = listener.getState();
        final boolean value = state.equals(exclude) ? excludeReplacement : state.isActive();
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
      for (final AggregateStateListener listener : stateListeners) {
        if (listener.getState().equals(state)) {
          return listener;
        }
      }

      return null;
    }

    private final class AggregateStateListener implements EventDataListener<Boolean> {
      private final StateObserver state;

      private AggregateStateListener(final StateObserver state) {
        this.state = state;
        this.state.addDataListener(this);
      }

      @Override
      public void eventOccurred(final Boolean newValue) {
        ((DefaultStateObserver) getObserver()).notifyObservers(getPreviousState(state, !newValue), isActive());
      }

      private boolean getPreviousState(final StateObserver excludeState, final boolean previousValue) {
        synchronized (lock) {
          return isActive(conjunction, excludeState, previousValue);
        }
      }

      private StateObserver getState() {
        return state;
      }
    }
  }

  private static final class DefaultStateObserver implements StateObserver {

    private final Object lock = new Object();
    private final StateObserver stateObserver;
    private final boolean reversed;

    private Event<Boolean> stateChangedEvent;
    private DefaultStateObserver reversedStateObserver;

    private DefaultStateObserver(final StateObserver stateObserver, final boolean reversed) {
      this.stateObserver = stateObserver;
      this.reversed = reversed;
    }

    @Override
    public boolean isActive() {
      synchronized (lock) {
        return reversed ? !stateObserver.isActive() : stateObserver.isActive();
      }
    }

    @Override
    public EventObserver<Boolean> getChangeObserver() {
      synchronized (lock) {
        if (stateChangedEvent == null) {
          stateChangedEvent = Events.event();
        }

        return stateChangedEvent.getObserver();
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
      getChangeObserver().addListener(listener);
    }

    @Override
    public void removeListener(final EventListener listener) {
      getChangeObserver().removeListener(listener);
    }

    @Override
    public void addDataListener(final EventDataListener<Boolean> listener) {
      getChangeObserver().addDataListener(listener);
    }

    @Override
    public void removeDataListener(final EventDataListener listener) {
      getChangeObserver().removeDataListener(listener);
    }

    private void notifyObservers(final boolean previousValue, final boolean newValue) {
      synchronized (lock) {
        if (previousValue != newValue) {
          if (stateChangedEvent != null) {
            stateChangedEvent.fire(newValue);
          }
          if (reversedStateObserver != null) {
            reversedStateObserver.notifyObservers(newValue, previousValue);
          }
        }
      }
    }
  }

  private static final class DefaultGroup implements State.Group {

    private final List<WeakReference<State>> members = new ArrayList<>();

    public DefaultGroup(final State... states) {
      if (states != null) {
        for (final State state : states) {
          addState(state);
        }
      }
    }

    @Override
    public void addState(final State state) {
      synchronized (members) {
        for (final WeakReference<State> reference : members) {
          if (reference.get() == state) {
            return;
          }
        }//no duplicate states

        members.add(new WeakReference<>(state));
      }
      updateAccordingToState(state);
      state.addListener(() -> updateAccordingToState(state));
    }

    private void updateAccordingToState(final State state) {
      synchronized (members) {
        final ListIterator<WeakReference<State>> iterator = members.listIterator();
        while (iterator.hasNext()) {
          final State referredState = iterator.next().get();
          if (referredState == null) {//remove this dead weak reference
            iterator.remove();
          }
          else if (state.isActive() && !state.equals(referredState)) {
            referredState.setActive(false);
          }
        }
      }
    }
  }
}
