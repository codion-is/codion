/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A state which behaves according to a set of states, either ANDing or ORing those together
 * when determining its own state.
 */
public final class AggregateState extends State {

  /**
   * The conjunction types used in AggregateState.
   */
  public enum Type {AND, OR}

  private final List<State> states = new ArrayList<State>();
  private final Type type;

  public AggregateState(final Type type) {
    this.type = type;
  }

  public AggregateState(final Type type, final State... states) {
    this(type);
    for (final State state : states) {
      addState(state);
    }
  }

  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder("Aggregate ");
    stringBuilder.append(type == Type.AND ? "AND " : "OR ").append(isActive() ? "active" : "inactive");
    for (final State state : states) {
      stringBuilder.append(", ").append(state);
    }

    return stringBuilder.toString();
  }

  /**
   * @return the type of this aggregate state
   */
  public Type getType() {
    return type;
  }

  public void addState(final State state) {
    final boolean wasActive = isActive();
    states.add(state);
    state.eventStateChanged().addListener(eventStateChanged());
    if (wasActive != isActive()) {
      eventStateChanged().fire();
    }
  }

  public void removeState(final State state) {
    final boolean wasActive = isActive();
    state.eventStateChanged().removeListener(eventStateChanged());
    states.remove(state);
    if (wasActive != isActive()) {
      eventStateChanged().fire();
    }
  }

  @Override
  public boolean isActive() {
    if (type == Type.AND) { //AND, one inactive is enough
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