/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

/**
 * A state which behaves according to a set of states, either ANDing or ORing those together
 * when determining its own state
 */
public class AggregateState extends State implements Action {

  public final static int AND = 1;
  public final static int OR = 2;

  private final ArrayList<State> states = new ArrayList<State>();
  private final int type;

  public AggregateState(final int type) {
    this.type = type;
  }

  public AggregateState(final int type, final State... states) {
    this(type);
    for (final State state : states)
      addState(state);
  }

  /** {@inheritDoc} */
  public String toString() {
    final StringBuffer ret = new StringBuffer("Aggregate ");
    ret.append(type == AND ? "AND " : "OR ").append(isActive() ? "active" : "inactive");
    for (final State state: states)
      ret.append(", ").append(state);

    return ret.toString();
  }

  /**
   * @return Value for property 'type'.
   */
  public int getType() {
    return type;
  }

  public void addState(final State state) {
    boolean wasActive = isActive();
    states.add(state);
    state.evtStateChanged.addListener(evtStateChanged);
    if (wasActive != isActive())
      evtStateChanged.fire();
  }

  public void removeState(final State state) {
    boolean wasActive = isActive();
    state.evtStateChanged.removeListener(evtStateChanged);
    states.remove(state);
    if (wasActive != isActive())
      evtStateChanged.fire();
  }

  /** {@inheritDoc} */
  public boolean isActive() {
    if (getType() == AND) { //AND, one inactive is enough
      for (final State state : states) {
        if (!state.isActive())
          return false;
      }

      return true;
    }
    else { //OR, one active is enough
      for (final State state : states) {
        if (state.isActive())
          return true;
      }

      return false;
    }
  }

  /** {@inheritDoc} */
  public void setActive(final boolean isActive) {
    throw new RuntimeException("The state of aggregate states can't be set");
  }

  /** {@inheritDoc} */
  public void addActivateEvent(final Event activateEvent) {
    throw new RuntimeException("The state of aggregate states can't be set");
  }

  /** {@inheritDoc} */
  public Object getValue(final String key) {
    return null;
  }

  /** {@inheritDoc} */
  public void putValue(String key, Object value) {}

  /** {@inheritDoc} */
  public void actionPerformed(ActionEvent ev) {}

  /** {@inheritDoc} */
  public void setEnabled(final boolean b) {
    setActive(b);
  }

  /** {@inheritDoc} */
  public boolean isEnabled() {
    return isActive();
  }

  /** {@inheritDoc} */
  public void addPropertyChangeListener(PropertyChangeListener listener) {}

  /** {@inheritDoc} */
  public void removePropertyChangeListener(PropertyChangeListener listener) {}
}