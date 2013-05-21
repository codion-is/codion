/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
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
    return new StateImpl(initialState);
  }

  /**
   * Instantiates a new State.AggregateState object.
   * @param conjunction the conjunction to use
   * @param stateObservers the state observers to base this aggregate state on
   * @return a new State.AggregateState
   */
  public static State.AggregateState aggregateState(final Conjunction conjunction, final StateObserver... stateObservers) {
    Util.rejectNullValue(conjunction, "conjunction");
    return new AggregateStateImpl(conjunction, stateObservers);
  }

  /**
   * Instantiates a new State.StateGroup object.
   * @return a new State.StateGroup
   */
  public static State.StateGroup stateGroup() {
    return new StateGroupImpl();
  }

  private static class StateImpl implements State {

    private StateObserver observer;
    private boolean active = false;

    StateImpl() {
      this(false);
    }

    StateImpl(final boolean initialState) {
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
      ((StateObserverImpl) getObserver()).notifyObservers(previousValue, active);
    }

    @Override
    public boolean isActive() {
      return active;
    }

    @Override
    public final synchronized StateObserver getObserver() {
      if (observer == null) {
        observer = new StateObserverImpl(this, false);
      }

      return observer;
    }

    @Override
    public final EventObserver getStateChangeObserver() {
      return getObserver().getStateChangeObserver();
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
    public final StateObserver getReversedObserver() {
      return getObserver().getReversedObserver();
    }
  }

  private static final class AggregateStateImpl extends StateImpl implements State.AggregateState {

    private final List<StateObserver> states = new ArrayList<StateObserver>();
    private final EventListener listener = new EventAdapter() {
      /** {@inheritDoc} */
      @Override
      public void eventOccurred() {
        ((StateObserverImpl) getObserver()).notifyObservers();
      }
    };
    private final Conjunction conjunction;

    private AggregateStateImpl(final Conjunction conjunction) {
      this.conjunction = conjunction;
    }

    private AggregateStateImpl(final Conjunction conjunction, final StateObserver... states) {
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
      for (final StateObserver state : states) {
        stringBuilder.append(", ").append(state);
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
      final boolean wasActive = isActive();
      states.add(state);
      state.addListener(listener);
      if (wasActive != isActive()) {
        ((StateObserverImpl) getObserver()).notifyObservers();
      }
    }

    @Override
    public synchronized void removeState(final StateObserver state) {
      Util.rejectNullValue(state, "state");
      final boolean wasActive = isActive();
      state.removeListener(listener);
      states.remove(state);
      if (wasActive != isActive()) {
        ((StateObserverImpl) getObserver()).notifyObservers();
      }
    }

    @Override
    public synchronized boolean isActive() {
      if (conjunction == Conjunction.AND) { //AND, one inactive is enough
        for (final StateObserver state : states) {
          if (!state.isActive()) {
            return false;
          }
        }

        return true;
      }
      else { //OR, one active is enough
        for (final StateObserver state : states) {
          if (state.isActive()) {
            return true;
          }
        }

        return false;
      }
    }

    @Override
    public synchronized void setActive(final boolean active) {
      throw new UnsupportedOperationException("The state of aggregate states can't be set");
    }
  }

  private static final class StateObserverImpl implements StateObserver {

    private final StateObserver stateObserver;
    private final boolean reversed;

    private final Event evtStateChanged = Events.event();
    private final Event evtStateActivated = Events.event();
    private final Event evtStateDeactivated = Events.event();

    private StateObserverImpl reversedStateObserver = null;

    private StateObserverImpl(final StateObserver stateObserver, final boolean reversed) {
      this.stateObserver = stateObserver;
      this.reversed = reversed;
    }

    @Override
    public boolean isActive() {
      return reversed ? !stateObserver.isActive() : stateObserver.isActive();
    }

    @Override
    public EventObserver getStateChangeObserver() {
      return evtStateChanged.getObserver();
    }

    @Override
    public synchronized StateObserver getReversedObserver() {
      if (reversedStateObserver == null) {
        reversedStateObserver = new StateObserverImpl(this, true);
      }

      return reversedStateObserver;
    }

    @Override
    public void addListener(final EventListener listener) {
      evtStateChanged.addListener(listener);
    }

    @Override
    public void removeListener(final EventListener listener) {
      evtStateChanged.removeListener(listener);
    }

    @Override
    public void addActivateListener(final EventListener listener) {
      evtStateActivated.addListener(listener);
    }

    @Override
    public void removeActivateListener(final EventListener listener) {
      evtStateActivated.removeListener(listener);
    }

    @Override
    public void addDeactivateListener(final EventListener listener) {
      evtStateDeactivated.addListener(listener);
    }

    @Override
    public void removeDeactivateListener(final EventListener listener) {
      evtStateDeactivated.removeListener(listener);
    }

    private synchronized void notifyObservers() {
      evtStateChanged.fire();
      if (reversedStateObserver != null) {
        reversedStateObserver.notifyObservers();
      }
    }

    private synchronized void notifyObservers(final boolean previousValue, final boolean newValue) {
      if (previousValue != newValue) {
        notifyObservers();
        if (newValue) {
          evtStateActivated.fire();
        }
        else {
          evtStateDeactivated.fire();
        }
      }
    }
  }

  static final class StateGroupImpl implements State.StateGroup {

    private final List<WeakReference<State>> members = new ArrayList<WeakReference<State>>();

    @Override
    public void addState(final State state) {
      synchronized (members) {
        for (final WeakReference<State> reference : members) {
          if (reference.get() == state) {
            return;
          }
        }//no duplicate states

        members.add(new WeakReference<State>(state));
      }
      updateAccordingToState(state);
      state.addListener(new EventAdapter() {
        /** {@inheritDoc} */
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
