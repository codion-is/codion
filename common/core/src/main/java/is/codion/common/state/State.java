/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.state;

import is.codion.common.Conjunction;
import is.codion.common.value.Value;

import java.util.Collection;

/**
 * A class encapsulating a boolean state, non-nullable with null values translated to false.
 * <pre>
 * {@code
 * State state = State.state();
 *
 * StateObserver observer = state.observer();
 *
 * observer.addConsumer(this::onStateChange);
 *
 * state.set(true);
 * state.set(false);
 * state.clear(); //translates to false
 * }
 * </pre>
 * A factory for {@link State} instances.
 * @see #state()
 * @see #builder()
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
		void add(StateObserver state);

		/**
		 * Removes a state from this state combination
		 * @param state the state to remove from this state combination
		 */
		void remove(StateObserver state);
	}

	/**
	 * A {@link State.Group} deactivates all other states when a state in the group is activated.
	 */
	interface Group {

		/**
		 * Adds a state to this {@link State.Group}.
		 * Adding an active state deactivates all other states in the group.
		 * @param state the {@link State} instance to add
		 */
		void add(State state);

		/**
		 * Adds the given states to this {@link State.Group}.
		 * @param states the {@link State} instances to add
		 */
		void add(Collection<State> states);
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
	 * @param initialValue the initial state value
	 * @return a new {@link State} instance
	 */
	static State state(boolean initialValue) {
		return builder(initialValue).build();
	}

	/**
	 * @return a new {@link Builder} instance
	 */
	static Builder builder() {
		return builder(false);
	}

	/**
	 * @param initialValue the initial value
	 * @return a new {@link Builder} instance
	 */
	static Builder builder(boolean initialValue) {
		return new DefaultState.DefaultBuilder(initialValue);
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

	/**
	 * Builds a {@link State} instance.
	 */
	interface Builder extends Value.Builder<Boolean, Builder> {

		/**
		 * @return a new {@link State} instance based on this builder
		 */
		State build();
	}
}