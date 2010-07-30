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
 * User: Björn Darri
 * Date: 29.7.2010
 * Time: 20:26:46
 */
public final class States {

  public static State state() {
    return new StateImpl();
  }

  public static State state(final boolean initialState) {
    return new StateImpl(initialState);
  }

  public static State.AggregateState aggregateState(final Conjunction type, final StateObserver... states) {
    return new AggregateStateImpl(type, states);
  }

  public static State.StateGroup stateGroup() {
    return new StateGroupImpl();
  }

  static class StateImpl implements State {

    private final Object lock = new Object();

    private volatile StateObserver observer;
    private volatile boolean active = false;

    /**
     * Constructs a new State instance initialized as inactive
     */
    StateImpl() {
      this(false);
    }

    /**
     * Constructs a new State instance
     * @param initialState the initial state
     */
    StateImpl(final boolean initialState) {
      this.active = initialState;
    }

    @Override
    public String toString() {
      return active ? "active" : "inactive";
    }

    public final StateObserver getObserver() {
      if (observer == null) {
        synchronized (lock) {
          observer = new StateObserverImpl(this);
        }
      }
      return observer;
    }

    /**
     * @param value the new active state of this State instance
     */
    public synchronized void setActive(final boolean value) {
      final boolean oldValue = active;
      active = value;
      if (oldValue != value) {
        getObserver().notifyObserver();
      }
    }

    /**
     * @return true if this state is active, false otherwise
     */
    public boolean isActive() {
      return active;
    }

    public void addListeningAction(final Action action) {
      getObserver().addListeningAction(action);
    }

    public void addListener(final ActionListener listener) {
      getObserver().addListener(listener);
    }

    public void notifyObserver() {
      getObserver().notifyObserver();
    }

    public void removeListener(final ActionListener listener) {
      getObserver().removeListener(listener);
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
          getObserver().notifyObserver();
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

  /**
   * A state which behaves according to a set of states, either ANDing or ORing those together
   * when determining its own state.
   */
  private static final class AggregateStateImpl extends StateImpl implements State.AggregateState {

    /**
     * The conjunction types used in AggregateState.
     */
    public enum Type {AND, OR}

    private final List<StateObserver> states = new ArrayList<StateObserver>();
    private final ActionListener linkAction = new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        getObserver().notifyObserver();
      }
    };
    private final Conjunction type;

    private AggregateStateImpl(final Conjunction type) {
      this.type = type;
    }

    private AggregateStateImpl(final Conjunction type, final StateObserver... states) {
      this(type);
      for (final StateObserver state : states) {
        addState(state);
      }
    }

    @Override
    public String toString() {
      final StringBuilder stringBuilder = new StringBuilder("Aggregate ");
      stringBuilder.append(type == Conjunction.AND ? "AND " : "OR ").append(isActive() ? "active" : "inactive");
      for (final StateObserver state : states) {
        stringBuilder.append(", ").append(state);
      }

      return stringBuilder.toString();
    }

    /**
     * @return the type of this aggregate state
     */
    public Conjunction getConjunction() {
      return type;
    }

    public void addState(final StateObserver state) {
      final boolean wasActive = isActive();
      states.add(state);
      state.addListener(linkAction);
      if (wasActive != isActive()) {
        getObserver().notifyObserver();
      }
    }

    public void removeState(final StateObserver state) {
      final boolean wasActive = isActive();
      state.removeListener(linkAction);
      states.remove(state);
      if (wasActive != isActive()) {
        getObserver().notifyObserver();
      }
    }

    @Override
    public boolean isActive() {
      if (type == Conjunction.AND) { //AND, one inactive is enough
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
    private final Event evtStateChanged = Events.event();
    private final State state;

    private ReverseState reversedState = null;

    private StateObserverImpl(final State state) {
      this.state = state;
    }

    public boolean isActive() {
      return state.isActive();
    }

    /**
     * @return A State object that is always the reverse of the parent state
     */
    public StateObserver getReversedState() {
      if (reversedState == null) {
        reversedState = new ReverseState(this);
      }

      return reversedState.getObserver();
    }

    public final void addListeningAction(final Action action) {
      action.setEnabled(state.isActive());
      evtStateChanged.addListener(new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
          action.setEnabled(state.isActive());
        }
      });
    }

    /**
     * @return an EventObserver notified each time the state changes
     */
    public final EventObserver stateObserver() {
      return evtStateChanged.getObserver();
    }

    public final void addListener(final ActionListener listener) {
      evtStateChanged.addListener(listener);
    }

    public final void removeListener(final ActionListener listener) {
      evtStateChanged.removeListener(listener);
    }

    public final void notifyObserver() {
      evtStateChanged.fire();
    }
  }

  /**
   * A StateGroup deactivates all other states when a state in the group is activated.
   * StateGroup works with WeakReference so adding states does not prevent
   * them from being garbage collected.
   */
  static final class StateGroupImpl implements State.StateGroup {

    private final List<WeakReference<State>> members = Collections.synchronizedList(new ArrayList<WeakReference<State>>());

    /**
     * Adds a state to this state group via a WeakReference,
     * so it does not prevent it from being garbage collected.
     * Adding an active state deactivates all other states in the group.
     * @param state the State to add
     */
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
