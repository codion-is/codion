/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.state;

import is.codion.common.Conjunction;

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
   * Instantiates a new State.Combination object.
   * @param conjunction the conjunction to use
   * @param stateObservers the state observers to base this state combination on
   * @return a new State.Combination
   */
  public static State.Combination combination(final Conjunction conjunction, final StateObserver... stateObservers) {
    return new DefaultStateCombination(conjunction, stateObservers);
  }

  /**
   * Instantiates a new State.Group object, which guarantees that only a single
   * state within the group is active at a time
   * @param states the states to add to the group initially, not required
   * @return a new State.Group
   * @see State.Group
   */
  public static State.Group group(final State... states) {
    return new DefaultStateGroup(states);
  }
}
