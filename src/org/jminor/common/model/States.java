/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A factory class for State objects.
 * @see State
 */
public final class States {

  private States() {}

  /**
   * Instantiates a new State object.
   * @return a new State
   */
  public static State state() {
    return new StateImpl();
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

  static class StateImpl implements State {

    static final String ACTIVE = "active";
    static final String INACTIVE = "inactive";

    private final Event evtStateChanged = Events.event();

    private volatile StateObserver observer;
    private volatile boolean active = false;

    StateImpl() {
      this(false);
    }

    StateImpl(final boolean initialState) {
      this.active = initialState;
    }

    @Override
    public String toString() {
      return active ? ACTIVE : INACTIVE;
    }

    public final StateObserver getObserver() {
      if (observer == null) {
        synchronized (evtStateChanged) {
          observer = new StateObserverImpl(this);
        }
      }
      return observer;
    }

    public synchronized void setActive(final boolean value) {
      final boolean oldValue = active;
      active = value;
      if (oldValue != value) {
        evtStateChanged.fire();
      }
    }

    public boolean isActive() {
      return active;
    }

    public final void addListeningAction(final Action action) {
      Util.rejectNullValue(action, "action");
      action.setEnabled(isActive());
      addListener(new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
          action.setEnabled(isActive());
        }
      });
    }

    public final void addListener(final ActionListener listener) {
      evtStateChanged.addListener(listener);
    }

    public final void notifyObservers() {
      evtStateChanged.fire();
    }

    public final void removeListener(final ActionListener listener) {
      evtStateChanged.removeListener(listener);
    }

    public StateObserver getReversedState() {
      return getObserver().getReversedState();
    }
  }

  private static final class ReverseState extends StateImpl {

    private final StateObserver referenceState;

    ReverseState(final StateObserver referenceState) {
      this.referenceState = referenceState;
      this.referenceState.addListener(new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
          notifyObservers();
        }
      });
    }

    @Override
    public boolean isActive() {
      return !referenceState.isActive();
    }

    @Override
    public StateObserver getReversedState() {
      return referenceState;
    }

    @Override
    public synchronized void setActive(final boolean value) {
      throw new RuntimeException("Cannot set the state of a reversed state");
    }

    @Override
    public String toString() {
      return isActive() ? "active reversed" : "inactive reversed";
    }
  }

  private static final class AggregateStateImpl extends StateImpl implements State.AggregateState {

    private final List<StateObserver> states = new ArrayList<StateObserver>();
    private final ActionListener linkAction = new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        notifyObservers();
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
      final StringBuilder stringBuilder = new StringBuilder("Aggregate ");
      stringBuilder.append(conjunction.toString()).append(isActive() ? ACTIVE : INACTIVE);
      for (final StateObserver state : states) {
        stringBuilder.append(", ").append(state);
      }

      return stringBuilder.toString();
    }

    public Conjunction getConjunction() {
      return conjunction;
    }

    public synchronized void addState(final StateObserver state) {
      Util.rejectNullValue(state, "state");
      final boolean wasActive = isActive();
      states.add(state);
      state.addListener(linkAction);
      if (wasActive != isActive()) {
        notifyObservers();
      }
    }

    public synchronized void removeState(final StateObserver state) {
      Util.rejectNullValue(state, "state");
      final boolean wasActive = isActive();
      state.removeListener(linkAction);
      states.remove(state);
      if (wasActive != isActive()) {
        notifyObservers();
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
    public synchronized void setActive(final boolean value) {
      throw new RuntimeException("The state of aggregate states can't be set");
    }
  }

  static final class StateObserverImpl implements StateObserver {

    private final State state;

    private volatile ReverseState reversedState = null;

    private StateObserverImpl(final State state) {
      this.state = state;
    }

    public boolean isActive() {
      return state.isActive();
    }

    public StateObserver getReversedState() {
      if (reversedState == null) {
        synchronized (state) {
          reversedState = new ReverseState(this);
        }
      }
      return reversedState.getObserver();
    }

    public void addListeningAction(final Action action) {
      state.addListeningAction(action);
    }

    public void addListener(final ActionListener listener) {
      state.addListener(listener);
    }

    public void removeListener(final ActionListener listener) {
      state.removeListener(listener);
    }
  }

  static final class StateGroupImpl implements State.StateGroup {

    private final List<WeakReference<State>> members = Collections.synchronizedList(new ArrayList<WeakReference<State>>());

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
      state.addListener(new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
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
