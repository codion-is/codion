/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A class encapsulting a simple boolean state, providing change events
 */
public class State implements Serializable {

  public final Event evtStateChanged = new Event("State.evtStateChanged") {
    public String toString() {
      return super.toString() + " [" + State.this + "]";
    }
  };
  public final Event evtSetActive = new Event("State.evtSetActive") {
    public String toString() {
      return super.toString() + " [" + State.this + "]";
    }
  };
  public final Event evtSetInactive = new Event("State.evtSetInactive") {
    public String toString() {
      return super.toString() + " [" + State.this + "]";
    }
  };

  private ReverseState reversedState = null;
  private boolean active = false;
  private final String name;

  /**
   * Constructs a new anonymous State instance initialized as inactive
   */
  public State() {
    this(null);
  }

  /**
   * Constructs a new State instance
   * @param name the name of the state
   */
  public State(final String name) {
    this.name = name;
  }

  /**
   * Constructs a new State instance
   * @param name the name of the state
   * @param initialState the initial state
   */
  public State(final String name, final boolean initialState) {
    this(name);
    setActive(initialState);
  }

  /**
   * Constructs a new State instance
   * @param initialState the initial state
   */
  public State(final boolean initialState) {
    this();
    setActive(initialState);
  }

  /** {@inheritDoc} */
  public String toString() {
    return (name != null ? (name + " ") : "") + (active ? "active" : "inactive");
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
    if (active)
      evtSetActive.fire();
    else
      evtSetInactive.fire();

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
   * @return A State object that is always the reverse of the parent state
   */
  public State getReversedState() {
    if (reversedState == null)
      reversedState = new ReverseState(this);

    return reversedState;
  }

  private static class ReverseState extends State {

    private final State referenceState;

    private ReverseState(final State referenceState) {
      this.referenceState = referenceState;
      this.referenceState.evtStateChanged.addListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          ReverseState.this.evtStateChanged.fire();
        }
      });
    }

    public boolean isActive() {
      return !referenceState.isActive();
    }

    public State getReversedState() {
      return referenceState;
    }

    public void setActive(final boolean isActive) {
      throw new RuntimeException("Cannot set the state of a reversed state");
    }

    public String toString() {
      return referenceState + " reversed";
    }
  }

  /**
   * A StateGroup deactivates all other states when a state in the group is activated
   */
  public static class StateGroup {
    final List<State> members;

    /** Constructs a new StateGroup. */
    public StateGroup() {
      this.members = new ArrayList<State>();
    }

    public void addState(final State state) {
      if (!members.contains(state)) {
        members.add(state);
        state.evtSetActive.addListener(new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            for (final State currstate : members)
              if (currstate != state)
                currstate.setActive(false);
          }
        });
      }
    }
  }
}
