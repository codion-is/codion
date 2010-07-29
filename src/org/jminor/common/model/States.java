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

  public static State.AggregateState aggregateState(final State.AggregateState.Type type, final State... states) {
    return new AggregateStateImpl(type, states);
  }

  public static State.StateGroup stateGroup() {
    return new StateGroupImpl();
  }

  static class StateImpl implements State {
    private final Event evtStateChanged = Events.event();

    private LinkedState linkedState = null;
    private ReverseState reversedState = null;
    private boolean active = false;

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

    public final void addListeningAction(final Action action) {
      action.setEnabled(isActive());
      evtStateChanged.addListener(new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
          action.setEnabled(isActive());
        }
      });
    }

    /**
     * @param value the new active state of this State instance
     */
    public void setActive(final boolean value) {
      final boolean oldValue = active;
      active = value;
      if (oldValue != value) {
        evtStateChanged.fire();
      }
    }

    /**
     * @return true if this state is active, false otherwise
     */
    public boolean isActive() {
      return active;
    }

    /**
     * @return a State that is always the same as the parent state but can not be directly modified
     */
    public final State getLinkedState() {
      if (linkedState == null) {
        linkedState = new LinkedState(this);
      }

      return linkedState;
    }

    /**
     * @return A State object that is always the reverse of the parent state
     */
    public State getReversedState() {
      if (reversedState == null) {
        reversedState = new ReverseState(this);
      }

      return reversedState;
    }

    /**
     * @return an EventObserver notified each time the state changes
     */
    public final EventObserver stateObserver() {
      return evtStateChanged.getObserver();
    }

    public final void addStateListener(final ActionListener listener) {
      evtStateChanged.addListener(listener);
    }

    public final void removeStateListener(final ActionListener listener) {
      evtStateChanged.removeListener(listener);
    }

    public final void notifyStateObserver() {
      evtStateChanged.fire();
    }

    protected Event evtStateChanged() {
      return evtStateChanged;
    }

    private class LinkedState extends StateImpl {

      private final State referenceState;

      private LinkedState(final State referenceState) {
        this.referenceState = referenceState;
        this.referenceState.addStateListener(new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            evtStateChanged().actionPerformed(e);
          }
        });
      }

      @Override
      public boolean isActive() {
        return referenceState.isActive();
      }

      @Override
      public void setActive(final boolean value) {
        throw new RuntimeException("Cannot set the state of a linked state");
      }

      @Override
      public String toString() {
        return referenceState + " linked";
      }

      public final State getReferenceState() {
        return referenceState;
      }
    }

    private final class ReverseState extends LinkedState {

      private ReverseState(final State referenceState) {
        super(referenceState);
      }

      @Override
      public boolean isActive() {
        return !getReferenceState().isActive();
      }

      @Override
      public State getReversedState() {
        return getReferenceState();
      }

      @Override
      public void setActive(final boolean value) {
        throw new RuntimeException("Cannot set the state of a reversed state");
      }

      @Override
      public String toString() {
        return isActive() ? "active reversed" : "inactive reversed";
      }
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

    private final List<State> states = new ArrayList<State>();
    private final AggregateState.Type type;

    private AggregateStateImpl(final AggregateState.Type type) {
      this.type = type;
    }

    private AggregateStateImpl(final AggregateState.Type type, final State... states) {
      this(type);
      for (final State state : states) {
        addState(state);
      }
    }

    @Override
    public String toString() {
      final StringBuilder stringBuilder = new StringBuilder("Aggregate ");
      stringBuilder.append(type == AggregateState.Type.AND ? "AND " : "OR ").append(isActive() ? "active" : "inactive");
      for (final State state : states) {
        stringBuilder.append(", ").append(state);
      }

      return stringBuilder.toString();
    }

    /**
     * @return the type of this aggregate state
     */
    public State.AggregateState.Type getType() {
      return type;
    }

    public void addState(final State state) {
      final boolean wasActive = isActive();
      states.add(state);
      state.addStateListener(evtStateChanged());
      if (wasActive != isActive()) {
        evtStateChanged().fire();
      }
    }

    public void removeState(final State state) {
      final boolean wasActive = isActive();
      state.removeStateListener(evtStateChanged());
      states.remove(state);
      if (wasActive != isActive()) {
        evtStateChanged().fire();
      }
    }

    @Override
    public boolean isActive() {
      if (type == AggregateState.Type.AND) { //AND, one inactive is enough
        for (final State state : states) {
          if (!state.isActive()) {
            return false;
          }
        }

        return true;
      }
      else { //OR, one active is enough
        for (final State state : states) {
          if (state.isActive()) {
            return true;
          }
        }

        return false;
      }
    }

    @Override
    public void setActive(final boolean value) {
      throw new RuntimeException("The state of aggregate states can't be set");
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
      state.addStateListener(new ActionListener() {
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
