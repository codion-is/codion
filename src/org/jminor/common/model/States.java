/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * A factory class for State objects.
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
    Util.rejectNullValue(conjunction, "conjunction");
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
    public synchronized void setActive(final boolean active) {
      final boolean previousValue = this.active;
      this.active = active;
      ((DefaultStateObserver) getObserver()).notifyObservers(previousValue, active);
    }

    @Override
    public synchronized boolean isActive() {
      return active;
    }

    @Override
    public final synchronized StateObserver getObserver() {
      if (observer == null) {
        observer = new DefaultStateObserver(this, false);
      }

      return observer;
    }

    @Override
    public final EventObserver<Boolean> getChangeObserver() {
      return getObserver().getChangeObserver();
    }

    @Override
    public final void addActivateListener(final EventListener listener) {
      getObserver().addActivateListener(listener);
    }

    @Override
    public final void removeActivateListener(final EventListener listener) {
      getObserver().removeActivateListener(listener);
    }

    @Override
    public final void addDeactivateListener(final EventListener listener) {
      getObserver().addDeactivateListener(listener);
    }

    @Override
    public final void removeDeactivateListener(final EventListener listener) {
      getObserver().removeDeactivateListener(listener);
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
    public void addInfoListener(final EventInfoListener<Boolean> listener) {
      getObserver().addInfoListener(listener);
    }

    @Override
    public void removeInfoListener(final EventInfoListener listener) {
      getObserver().removeInfoListener(listener);
    }

    @Override
    public final StateObserver getReversedObserver() {
      return getObserver().getReversedObserver();
    }
  }

  private static final class DefaultAggregateState extends DefaultState implements State.AggregateState {

    private final List<AggregateStateListener> stateListeners = new ArrayList<>();
    private final Conjunction conjunction;

    private DefaultAggregateState(final Conjunction conjunction) {
      this.conjunction = conjunction;
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
    public synchronized String toString() {
      final StringBuilder stringBuilder = new StringBuilder("Aggregate");
      stringBuilder.append(conjunction.toString()).append(super.toString());
      for (final AggregateStateListener listener : stateListeners) {
        stringBuilder.append(", ").append(listener.state);
      }

      return stringBuilder.toString();
    }

    @Override
    public Conjunction getConjunction() {
      return conjunction;
    }

    @Override
    public synchronized void addState(final StateObserver state) {
      Util.rejectNullValue(state, "state");
      if (findListener(state) == null) {
        final boolean wasActive = isActive();
        stateListeners.add(new AggregateStateListener(state));
        ((DefaultStateObserver) getObserver()).notifyObservers(wasActive, isActive());
      }
    }

    @Override
    public synchronized void removeState(final StateObserver state) {
      Util.rejectNullValue(state, "state");
      final boolean wasActive = isActive();
      final AggregateStateListener listener = findListener(state);
      if (listener != null) {
        state.removeInfoListener(listener);
        stateListeners.remove(listener);
        ((DefaultStateObserver) getObserver()).notifyObservers(wasActive, isActive());
      }
    }

    @Override
    public synchronized boolean isActive() {
      return isActive(conjunction, null, false);
    }

    @Override
    public synchronized void setActive(final boolean active) {
      throw new UnsupportedOperationException("The state of aggregate states can't be set");
    }

    private synchronized boolean getPreviousState(final StateObserver excludeState, final boolean previousValue) {
      return isActive(conjunction, excludeState, previousValue);
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

    private final class AggregateStateListener implements EventInfoListener<Boolean> {
      private final StateObserver state;

      private AggregateStateListener(final StateObserver state) {
        this.state = state;
        this.state.addInfoListener(this);
      }

      @Override
      public void eventOccurred(final Boolean isActive) {
        ((DefaultStateObserver) getObserver()).notifyObservers(getPreviousState(state, !isActive), isActive);
      }

      private StateObserver getState() {
        return state;
      }
    }
  }

  private static final class DefaultStateObserver implements StateObserver {

    private final StateObserver stateObserver;
    private final boolean reversed;

    private Event<Boolean> stateChangedEvent;
    private Event stateActivatedEvent;
    private Event stateDeactivatedEvent;

    private DefaultStateObserver reversedStateObserver = null;

    private DefaultStateObserver(final StateObserver stateObserver, final boolean reversed) {
      this.stateObserver = stateObserver;
      this.reversed = reversed;
    }

    @Override
    public boolean isActive() {
      return reversed ? !stateObserver.isActive() : stateObserver.isActive();
    }

    @Override
    public synchronized EventObserver<Boolean> getChangeObserver() {
      if (stateChangedEvent == null) {
        stateChangedEvent = Events.event();
      }

      return stateChangedEvent.getObserver();
    }

    @Override
    public synchronized StateObserver getReversedObserver() {
      if (reversedStateObserver == null) {
        reversedStateObserver = new DefaultStateObserver(this, true);
      }

      return reversedStateObserver;
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
    public void addInfoListener(final EventInfoListener<Boolean> listener) {
      getChangeObserver().addInfoListener(listener);
    }

    @Override
    public void removeInfoListener(final EventInfoListener listener) {
      getChangeObserver().removeInfoListener(listener);
    }

    @Override
    public void addActivateListener(final EventListener listener) {
      getStateActivatedEvent().addListener(listener);
    }

    @Override
    public void removeActivateListener(final EventListener listener) {
      getStateActivatedEvent().removeListener(listener);
    }

    @Override
    public void addDeactivateListener(final EventListener listener) {
      getStateDeactivatedEvent().addListener(listener);
    }

    @Override
    public void removeDeactivateListener(final EventListener listener) {
      getStateDeactivatedEvent().removeListener(listener);
    }

    private synchronized Event getStateActivatedEvent() {
      if (stateActivatedEvent == null) {
        stateActivatedEvent = Events.event();
      }

      return stateActivatedEvent;
    }

    private synchronized Event getStateDeactivatedEvent() {
      if (stateDeactivatedEvent == null) {
        stateDeactivatedEvent = Events.event();
      }

      return stateDeactivatedEvent;
    }

    private synchronized void notifyObservers(final boolean previousValue, final boolean newValue) {
      if (previousValue != newValue) {
        if (stateChangedEvent != null) {
          stateChangedEvent.fire(newValue);
        }
        if (reversedStateObserver != null) {
          reversedStateObserver.notifyObservers(newValue, previousValue);
        }
        if (newValue) {
          if (stateActivatedEvent != null) {
            stateActivatedEvent.fire();
          }
        }
        else {
          if (stateDeactivatedEvent != null) {
            stateDeactivatedEvent.fire();
          }
        }
      }
    }
  }

  static final class DefaultGroup implements State.Group {

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
      state.addListener(new EventListener() {
        @Override
        public void eventOccurred() {
          updateAccordingToState(state);
        }
      });
    }

    private void updateAccordingToState(final State state) {
      synchronized (members) {
        for (final WeakReference reference : members.toArray(new WeakReference[members.size()])) {
          final State referredState = (State) reference.get();
          if (referredState == null) {//remove this dead weak reference
            members.remove(reference);
          }
          else if (state.isActive() && !state.equals(referredState)) {
            referredState.setActive(false);
          }
        }
      }
    }
  }
}
