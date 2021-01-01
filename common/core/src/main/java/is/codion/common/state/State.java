/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.state;

import is.codion.common.Conjunction;

/**
 * A class encapsulating a boolean state.
 * <pre>
 * State state = States.state();
 *
 * StateObserver observer = state.getObserver();
 *
 * observer.getChangeObserver().addDataListener(this::onStateChange);
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
   * A state which combines a number of states, either ANDing or ORing those together
   * when determining its own state.
   */
  interface Combination extends State {

    /**
     * Returns the {@link Conjunction} used when combining the states.
     * @return the type of this aggregate state
     */
    Conjunction getConjunction();

    /**
     * Adds a state to this state combination
     * @param state the state to add to this state combination
     */
    void addState(StateObserver state);

    /**
     * Removes a state from this state combination
     * @param state the state to remove from this state combination
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