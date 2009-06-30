/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * A class encapsulting a simple boolean state, providing change events
 */
public class State implements Serializable {

  /** Fired each time the state changes */
  public final Event evtStateChanged = new Event();
  /** Fired each time <code>setActive(true)</code> is called */
  public final Event evtSetActive = new Event();
  /** Fired each time <code>setActive(false)</code> is called */
  public final Event evtSetInactive = new Event();

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
  @Override
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

    @Override
    public boolean isActive() {
      return !referenceState.isActive();
    }

    @Override
    public State getReversedState() {
      return referenceState;
    }

    @Override
    public void setActive(final boolean isActive) {
      throw new RuntimeException("Cannot set the state of a reversed state");
    }

    @Override
    public String toString() {
      return referenceState + " reversed";
    }
  }

  /**
   * A StateGroup deactivates all other states when a state in the group is activated.
   * StateGroup works with WeakReference so adding states does not prevent
   * them from being garbage collected.
   */
  public static class StateGroup {
    final List<WeakReference<State>> members;

    /** Constructs a new StateGroup. */
    public StateGroup() {
      this.members = new ArrayList<WeakReference<State>>();
    }

    /**
     * Adds a state to this state group via a WeakReference,
     * so it does not prevent it from being garbage collected.
     * @param state the State to add
     */
    public void addState(final State state) {
      for (final WeakReference<State> reference : members)
        if (reference.get() == state)
          return;//no duplicate states

      state.evtSetActive.addListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          for (final ListIterator<WeakReference<State>> iterator = members.listIterator(); iterator.hasNext();) {
            final State referredState = iterator.next().get();
            if (referredState == null) //remove this dead weak reference
              iterator.remove();
            else if (referredState != state)
              referredState.setActive(false);
          }
        }
      });
      members.add(new WeakReference<State>(state));
    }
  }
}