/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

/**
 * A class encapsulating a simple boolean state.
 */
public interface State extends StateObserver {

  /**
   * @param value the new active state of this State instance
   */
  void setActive(final boolean value);

  /**
   * @return an StateObserver notified each time the state changes
   */
  StateObserver getObserver();

  /**
   * A state which behaves according to a set of states, either ANDing or ORing those together
   * when determining its own state.
   */
  interface AggregateState extends State {

    /**
     * @return the type of this aggregate state
     */
    Conjunction getConjunction();

    /**
     * @param state the state to add to this aggregate state
     */
    void addState(final StateObserver state);

    /**
     * @param state the state to remove from this aggregate state
     */
    void removeState(final StateObserver state);
  }

  /**
   * A State.Group deactivates all other states when a state in the group is activated.
   * State.Group works with WeakReference so adding states does not prevent
   * them from being garbage collected.
   */
  interface Group {

    /**
     * Adds a state to this state group via a WeakReference,
     * so it does not prevent it from being garbage collected.
     * Adding an active state deactivates all other states in the group.
     * @param state the State to add
     */
    void addState(final State state);
  }
}