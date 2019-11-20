/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.state;

import org.jminor.common.Conjunction;

/**
 * A factory class for {@link State} objects.
 * @see State
 */
public final class States {

  private States() {}

  /**
   * Instantiates a new 'false' State object.
   * @return a new State
   */
  public static State state() {
    return state(false);
  }

  /**
   * Instantiates a new State object.
   * @param value the initial state value
   * @return a new State
   */
  public static State state(final boolean value) {
    return new DefaultState(value);
  }

  /**
   * Instantiates a new State.AggregateState object.
   * @param conjunction the conjunction to use
   * @param stateObservers the state observers to base this aggregate state on
   * @return a new State.AggregateState
   */
  public static State.AggregateState aggregateState(final Conjunction conjunction, final StateObserver... stateObservers) {
    return new DefaultAggregateState(conjunction, stateObservers);
  }

  /**
   * Instantiates a new State.Group object, which guarantees that only a single
   * state within the group is active at a time
   * @param states the states to add to the group initially, not required
   * @return a new State.Group
   * @see State.Group
   */
  public static State.Group group(final State... states) {
    return new DefaultGroup(states);
  }
}
