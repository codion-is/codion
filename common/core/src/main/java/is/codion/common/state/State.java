/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.state;

import is.codion.common.Conjunction;
import is.codion.common.value.Value;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
 * state.set(false);
 * state.set(null); //translates to false
 * </pre>
 * A factory class for {@link State} objects.
 */
public interface State extends StateObserver, Value<Boolean> {

  /**
   * Returns a {@link StateObserver} notified each time the state changes
   * @return a {@link StateObserver} notified each time the state changes
   */
  StateObserver observer();

  /**
   * A state which combines a number of states, either ANDing or ORing those together
   * when determining its own state.
   */
  interface Combination extends StateObserver {

    /**
     * Returns the {@link Conjunction} used when combining the states.
     * @return the conjunction of this state combination
     */
    Conjunction conjunction();

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
   * A {@link State.Group} deactivates all other states when a state in the group is activated.
   * {@link State.Group} works with WeakReference so adding states does not prevent
   * them from being garbage collected.
   */
  interface Group {

    /**
     * Adds a state to this {@link State.Group} via a WeakReference,
     * so it does not prevent it from being garbage collected.
     * Adding an active state deactivates all other states in the group.
     * @param state the {@link State} instance to add
     */
    void addState(State state);

    /**
     * Adds the given states to this {@link State.Group} via a WeakReference,
     * so it does not prevent it from being garbage collected.
     * Adding an active state deactivates all other states in the group.
     * @param states the {@link State} instances to add
     */
    void addStates(Collection<State> states);
  }

  /**
   * Creates a new 'false' {@link State} instance.
   * @return a new {@link State} instance
   */
  static State state() {
    return state(false);
  }

  /**
   * Creates a new {@link State} instance.
   * @param value the initial state value
   * @return a new {@link State} instance
   */
  static State state(boolean value) {
    return new DefaultState(value);
  }

  /**
   * Creates a new {@link State} instance based on the given getter and setter.
   * @param getter the getter
   * @param setter the setter
   * @return a new {@link State} instance
   * @throws NullPointerException in case either getter or setter is null
   */
  static State state(Supplier<Boolean> getter, Consumer<Boolean> setter) {
    return new DefaultState(getter, setter);
  }

  /**
   * Creates a new {@link State.Combination} instance.
   * @param conjunction the conjunction to use
   * @param stateObservers the state observers to base this state combination on
   * @return a new {@link State.Combination} instance
   */
  static Combination combination(Conjunction conjunction, StateObserver... stateObservers) {
    return new DefaultStateCombination(conjunction, stateObservers);
  }

  /**
   * Creates a new {@link State.Combination} instance.
   * @param conjunction the conjunction to use
   * @param stateObservers the state observers to base this state combination on
   * @return a new {@link State.Combination} instance
   */
  static Combination combination(Conjunction conjunction, Collection<? extends StateObserver> stateObservers) {
    return new DefaultStateCombination(conjunction, stateObservers);
  }

  /**
   * Creates a new {@link State.Combination} instance using {@link Conjunction#AND}.
   * @param stateObservers the state observers to base this state combination on
   * @return a new {@link State.Combination} instance
   */
  static Combination and(StateObserver... stateObservers) {
    return new DefaultStateCombination(Conjunction.AND, stateObservers);
  }

  /**
   * Creates a new {@link State.Combination} instance using {@link Conjunction#AND}.
   * @param stateObservers the state observers to base this state combination on
   * @return a new {@link State.Combination} instance
   */
  static Combination and(Collection<? extends StateObserver> stateObservers) {
    return new DefaultStateCombination(Conjunction.AND, stateObservers);
  }

  /**
   * Creates a new {@link State.Combination} instance using {@link Conjunction#OR}.
   * @param stateObservers the state observers to base this state combination on
   * @return a new {@link State.Combination} instance
   */
  static Combination or(StateObserver... stateObservers) {
    return new DefaultStateCombination(Conjunction.OR, stateObservers);
  }

  /**
   * Creates a new {@link State.Combination} instance using {@link Conjunction#OR}.
   * @param stateObservers the state observers to base this state combination on
   * @return a new {@link State.Combination} instance
   */
  static Combination or(Collection<? extends StateObserver> stateObservers) {
    return new DefaultStateCombination(Conjunction.OR, stateObservers);
  }

  /**
   * Creates a new {@link State.Group} instance, which guarantees that only a single
   * state within the group is active at a time
   * @param states the states to add to the group initially, not required
   * @return a new {@link State.Group} instance
   * @see Group
   */
  static Group group(State... states) {
    return new DefaultStateGroup(states);
  }

  /**
   * Creates a new {@link State.Group} instance, which guarantees that only a single
   * state within the group is active at a time
   * @param states the states to add to the group initially
   * @return a new {@link State.Group} instance
   * @see Group
   */
  static Group group(Collection<State> states) {
    return new DefaultStateGroup(states);
  }
}