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
 * A class encapsulating a simple boolean state, providing a change event.
 */
public class State {

  private final Event evtStateChanged = new Event();

  private LinkedState linkedState = null;
  private ReverseState reversedState = null;
  private boolean active = false;

  /**
   * Constructs a new State instance initialized as inactive
   */
  public State() {
    this(false);
  }

  /**
   * Constructs a new State instance
   * @param initialState the initial state
   */
  public State(final boolean initialState) {
    this.active = initialState;
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return active ? "active" : "inactive";
  }

  public void addListeningAction(final Action action) {
    action.setEnabled(isActive());
    evtStateChanged.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
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
    if (oldValue != value)
      evtStateChanged.fire();
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
  public State getLinkedState() {
    if (linkedState == null)
      linkedState = new LinkedState(this);

    return linkedState;
  }

  /**
   * @return A State object that is always the reverse of the parent state
   */
  public State getReversedState() {
    if (reversedState == null)
      reversedState = new ReverseState(this);

    return reversedState;
  }

  /**
   * @return an Event fired each time the state changes
   */
  public Event eventStateChanged() {
    return evtStateChanged;
  }

  private static class LinkedState extends State {

    private final State referenceState;

    private LinkedState(final State referenceState) {
      this.referenceState = referenceState;
      this.referenceState.eventStateChanged().addListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          eventStateChanged().actionPerformed(e);
        }
      });
    }

    @Override
    public boolean isActive() {
      return referenceState.isActive();
    }

    @Override
    public void setActive(final boolean isActive) {
      throw new RuntimeException("Cannot set the state of a linked state");
    }

    @Override
    public String toString() {
      return referenceState + " linked";
    }

    public State getReferenceState() {
      return referenceState;
    }
  }

  private static final class ReverseState extends LinkedState {

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
    public void setActive(final boolean isActive) {
      throw new RuntimeException("Cannot set the state of a reversed state");
    }

    @Override
    public String toString() {
      return isActive() ? "active reversed" : "inactive reversed";
    }
  }

  /**
   * A StateGroup deactivates all other states when a state in the group is activated.
   * StateGroup works with WeakReference so adding states does not prevent
   * them from being garbage collected.
   */
  public static class StateGroup {

    private final List<WeakReference<State>> members = Collections.synchronizedList(new ArrayList<WeakReference<State>>());

    /**
     * Adds a state to this state group via a WeakReference,
     * so it does not prevent it from being garbage collected.
     * Adding an active state deactivates all other states in the group.
     * @param state the State to add
     */
    public void addState(final State state) {
      synchronized (members) {
        for (final WeakReference<State> reference : members)
          if (reference.get() == state)
            return;//no duplicate states

        members.add(new WeakReference<State>(state));
      }
      updateAccordingToState(state);
      state.evtStateChanged.addListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          updateAccordingToState(state);
        }
      });
    }

    private void updateAccordingToState(final State state) {
      synchronized (members) {
        for (final WeakReference reference : members.toArray(new WeakReference[members.size()])) {
          final State referredState = (State) reference.get();
          if (referredState == null) //remove this dead weak reference
            members.remove(reference);
          else if (state.isActive() && referredState != state)
            referredState.setActive(false);
        }
      }
    }
  }
}