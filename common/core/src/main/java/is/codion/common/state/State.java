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
 * Copyright (c) 2008 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.state;

import is.codion.common.Conjunction;
import is.codion.common.value.Value;

import org.jspecify.annotations.NonNull;

import java.util.Collection;

/**
 * A class encapsulating a boolean state, non-nullable with null values translated to false.
 * {@snippet :
 * State state = State.state();
 *
 * ObservableState observable = state.observable();
 *
 * observer.addConsumer(this::onStateChange);
 *
 * state.set(true);
 * state.set(false);
 * state.set(null); //translates to false
 *}
 * A factory for {@link State} instances.
 * <p><b>Thread Safety:</b> Listener management (add/remove) is thread-safe and supports concurrent access.
 * However, state modifications via {@link #set(Object)} are NOT thread-safe and should be
 * performed from a single thread (such as an application UI thread).</p>
 * @see #state()
 * @see #builder()
 */
public interface State extends ObservableState, Value<Boolean> {

	@Override
	@NonNull Boolean get();

	/**
	 * Returns an {@link ObservableState} notified each time the state changes
	 * @return an {@link ObservableState} notified each time the state changes
	 */
	ObservableState observable();

	/**
	 * A state which combines a number of states, either ANDing or ORing those together
	 * when determining its own state.
	 */
	interface Combination extends ObservableState {

		/**
		 * Returns the {@link Conjunction} used when combining the states.
		 * @return the conjunction of this state combination
		 */
		Conjunction conjunction();

		/**
		 * Adds a state to this state combination
		 * @param state the state to add to this state combination
		 */
		void add(ObservableState state);

		/**
		 * Removes a state from this state combination
		 * @param state the state to remove from this state combination
		 */
		void remove(ObservableState state);
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
	 * @param value the initial state value
	 * @return a new {@link State} instance
	 */
	static State state(boolean value) {
		return builder(value).build();
	}

	/**
	 * @return a new {@link Builder} instance
	 */
	static Builder builder() {
		return builder(false);
	}

	/**
	 * @param value the initial value
	 * @return a new {@link Builder} instance
	 */
	static Builder builder(boolean value) {
		return new DefaultState.DefaultBuilder(value);
	}

	/**
	 * Creates a new {@link State.Combination} instance.
	 * @param conjunction the conjunction to use
	 * @param observableStates the state observers to base this state combination on
	 * @return a new {@link State.Combination} instance
	 */
	static Combination combination(Conjunction conjunction, ObservableState... observableStates) {
		return new DefaultStateCombination(conjunction, observableStates);
	}

	/**
	 * Creates a new {@link State.Combination} instance.
	 * @param conjunction the conjunction to use
	 * @param observableStates the state observers to base this state combination on
	 * @return a new {@link State.Combination} instance
	 */
	static Combination combination(Conjunction conjunction, Collection<? extends ObservableState> observableStates) {
		return new DefaultStateCombination(conjunction, observableStates);
	}

	/**
	 * Creates a new {@link State.Combination} instance using {@link Conjunction#AND}.
	 * @param observableStates the state observers to base this state combination on
	 * @return a new {@link State.Combination} instance
	 */
	static Combination and(ObservableState... observableStates) {
		return new DefaultStateCombination(Conjunction.AND, observableStates);
	}

	/**
	 * Creates a new {@link State.Combination} instance using {@link Conjunction#AND}.
	 * @param observableStates the state observers to base this state combination on
	 * @return a new {@link State.Combination} instance
	 */
	static Combination and(Collection<? extends ObservableState> observableStates) {
		return new DefaultStateCombination(Conjunction.AND, observableStates);
	}

	/**
	 * Creates a new {@link State.Combination} instance using {@link Conjunction#OR}.
	 * @param observableStates the state observers to base this state combination on
	 * @return a new {@link State.Combination} instance
	 */
	static Combination or(ObservableState... observableStates) {
		return new DefaultStateCombination(Conjunction.OR, observableStates);
	}

	/**
	 * Creates a new {@link State.Combination} instance using {@link Conjunction#OR}.
	 * @param observableStates the state observers to base this state combination on
	 * @return a new {@link State.Combination} instance
	 */
	static Combination or(Collection<? extends ObservableState> observableStates) {
		return new DefaultStateCombination(Conjunction.OR, observableStates);
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