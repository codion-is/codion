/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.state;

import org.jminor.common.Conjunction;

/**
 * A class encapsulating a boolean state.
 * <pre>
 * State state = States.state();
 *
 * StateObserver observer = state.getObserver();
 *
 * observer.getChangeObserver().addDataListener(this::handleStateChange);
 *
 * state.set(true);
 * </pre>
 */
public interface State extends StateObserver {

  /**
   * Sets the value of this {@link State}.
   * @param value the new active state of this State instance
   */
  void set(boolean value);

  /**
   * Returns a StateObserver notified each time the state changes
   * @return a StateObserver notified each time the state changes
   */
  StateObserver getObserver();

  /**
   * A state which behaves according to a set of states, either ANDing or ORing those together
   * when determining its own state.
   */
  interface AggregateState extends State {

    /**
     * Returns the {@link Conjunction} used when aggregating the states.
     * @return the type of this aggregate state
     */
    Conjunction getConjunction();

    /**
     * Adds a state to this aggregate state
     * @param state the state to add to this aggregate state
     */
    void addState(StateObserver state);

    /**
     * Removes a state from this aggregate state
     * @param state the state to remove from this aggregate state
     */
    void removeState(StateObserver state);
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
    void addState(State state);
  }
}