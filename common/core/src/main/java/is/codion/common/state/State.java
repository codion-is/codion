/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.state;

import is.codion.common.Conjunction;
import is.codion.common.value.Value;

import java.util.Collection;

/**
 * A class encapsulating a boolean state, non-nullable with null values translated to false.
 * <pre>
 * State state = State.state();
 *
 * StateObserver observer = state.getObserver();
 *
 * observer.addDataListener(this::onStateChange);
 *
 * state.set(true);
 * </pre>
 * A factory class for {@link State} objects.
 */
public interface State extends StateObserver, Value<Boolean> {

  /**
   * Returns a StateObserver notified each time the state changes
   * @return a StateObserver notified each time the state changes
   */
  StateObserver getObserver();

  /**
   * A state which combines a number of states, either ANDing or ORing those together
   * when determining its own state.
   */
  interface Combination extends StateObserver {

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

  /**
   * Instantiates a new 'false' State object.
   * @return a new State
   */
  static State state() {
    return state(false);
  }

  /**
   * Instantiates a new State object.
   * @param value the initial state value
   * @return a new State
   */
  static State state(final boolean value) {
    return new DefaultState(value);
  }

  /**
   * Instantiates a new {@link State.Combination} instance.
   * @param conjunction the conjunction to use
   * @param stateObservers the state observers to base this state combination on
   * @return a new State.Combination
   */
  static Combination combination(final Conjunction conjunction, final StateObserver... stateObservers) {
    return new DefaultStateCombination(conjunction, stateObservers);
  }

  /**
   * Instantiates a new {@link State.Combination} instance.
   * @param conjunction the conjunction to use
   * @param stateObservers the state observers to base this state combination on
   * @return a new State.Combination
   */
  static Combination combination(final Conjunction conjunction, final Collection<? extends StateObserver> stateObservers) {
    return new DefaultStateCombination(conjunction, stateObservers);
  }

  /**
   * Instantiates a new {@link State.Combination} instance using {@link Conjunction#AND}.
   * @param stateObservers the state observers to base this state combination on
   * @return a new State.Combination
   */
  static Combination and(final StateObserver... stateObservers) {
    return new DefaultStateCombination(Conjunction.AND, stateObservers);
  }

  /**
   * Instantiates a new {@link State.Combination} instance using {@link Conjunction#AND}.
   * @param stateObservers the state observers to base this state combination on
   * @return a new State.Combination
   */
  static Combination and(final Collection<? extends StateObserver> stateObservers) {
    return new DefaultStateCombination(Conjunction.AND, stateObservers);
  }

  /**
   * Instantiates a new {@link State.Combination} instance using {@link Conjunction#OR}.
   * @param stateObservers the state observers to base this state combination on
   * @return a new State.Combination
   */
  static Combination or(final StateObserver... stateObservers) {
    return new DefaultStateCombination(Conjunction.OR, stateObservers);
  }

  /**
   * Instantiates a new {@link State.Combination} instance using {@link Conjunction#OR}.
   * @param stateObservers the state observers to base this state combination on
   * @return a new State.Combination
   */
  static Combination or(final Collection<? extends StateObserver> stateObservers) {
    return new DefaultStateCombination(Conjunction.OR, stateObservers);
  }

  /**
   * Instantiates a new State.Group object, which guarantees that only a single
   * state within the group is active at a time
   * @param states the states to add to the group initially, not required
   * @return a new State.Group
   * @see Group
   */
  static Group group(final State... states) {
    return new DefaultStateGroup(states);
  }
}