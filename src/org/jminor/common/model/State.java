/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

/**
 * A class encapsulating a simple boolean state, providing a change event.
 */
public interface State extends StateObserver {

  /**
   * @param value the new active state of this State instance
   */
  void setActive(final boolean value);

  /**
   * @return an EventObserver notified each time the state changes
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

    void addState(final StateObserver state);

    void removeState(final StateObserver state);
  }

  /**
   * A StateGroup deactivates all other states when a state in the group is activated.
   * StateGroup works with WeakReference so adding states does not prevent
   * them from being garbage collected.
   */
  interface StateGroup {

    /**
     * Adds a state to this state group via a WeakReference,
     * so it does not prevent it from being garbage collected.
     * Adding an active state deactivates all other states in the group.
     * @param state the State to add
     */
    void addState(final State state);
  }
}