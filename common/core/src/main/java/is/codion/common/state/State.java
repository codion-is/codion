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
import is.codion.common.value.Value.Notify;
import is.codion.common.value.Value.Validator;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * A class encapsulating a boolean state.
 * {@snippet :
 * State state = State.state();
 *
 * ObservableState observable = state.observable();
 *
 * observer.addConsumer(this::onStateChange);
 *
 * state.set(true);
 * state.set(false);
 *
 * boolean value = state.is();
 *}
 * A factory for {@link State} instances.
 * <p>Listener management (add/remove) and state modifications are thread-safe
 * @see #state()
 * @see #state(boolean)
 * @see #builder()
 */
public interface State extends ObservableState {

	/**
	 * Sets the value
	 * @param value the new value
	 */
	void set(boolean value);

	/**
	 * Toggles this state
	 */
	void toggle();

	/**
	 * Returns an {@link ObservableState} notified each time the state changes
	 * @return an {@link ObservableState} notified each time the state changes
	 */
	ObservableState observable();

	/**
	 * @return a {@link Value} instance representing this state
	 */
	Value<Boolean> value();

	/**
	 * Creates a bidirectional link between this and the given original state,
	 * so that changes in one are reflected in the other.
	 * Note that after a call to this method this state is the same as {@code originalState}.
	 * @param originalState the original state to link this state to
	 * @throws IllegalStateException in case the states are already linked or if a cycle is detected
	 * @throws IllegalArgumentException in case the original state is not valid according to this states validators
	 */
	void link(State originalState);

	/**
	 * Unlinks this state from the given original state
	 * @param originalState the original value to unlink from this one
	 * @throws IllegalStateException in case the states are not linked
	 */
	void unlink(State originalState);

	/**
	 * Adds a validator to this {@link State}.
	 * Adding the same validator again has no effect.
	 * @param validator the validator
	 * @return true if this value did not already contain the specified validator
	 * @throws IllegalArgumentException in case the current value is invalid according to the validator
	 */
	boolean addValidator(Validator<? super Boolean> validator);

	/**
	 * Removes the given validator from this value
	 * @param validator the validator
	 * @return true if this value contained the specified validator
	 */
	boolean removeValidator(Validator<? super Boolean> validator);

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
		return builder().value(value).build();
	}

	/**
	 * @return a new {@link Builder} instance
	 */
	static Builder builder() {
		return new DefaultState.DefaultBuilder();
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
	interface Builder {

		/**
		 * @param value the initial value
		 * @return this builder instance
		 */
		Builder value(boolean value);

		/**
		 * @param notify the notify policy for this state, default {@link Notify#CHANGED}
		 * @return this builder instance
		 */
		Builder notify(Notify notify);

		/**
		 * Adds a validator to the resulting value
		 * @param validator the validator to add
		 * @return this builder instance
		 */
		Builder validator(Validator<? super Boolean> validator);

		/**
		 * Links the given state to the resulting state
		 * @param originalState the original state to link
		 * @return this builder instance
		 * @see Value#link(Value)
		 */
		Builder link(State originalState);

		/**
		 * @param listener a listener to add
		 * @return this builder instance
		 */
		Builder listener(Runnable listener);

		/**
		 * @param consumer a consumer to add
		 * @return this builder instance
		 */
		Builder consumer(Consumer<? super Boolean> consumer);

		/**
		 * @param weakListener a weak listener to add
		 * @return this builder instance
		 */
		Builder weakListener(Runnable weakListener);

		/**
		 * @param weakConsumer a weak consumer to add
		 * @return this builder instance
		 */
		Builder weakConsumer(Consumer<? super Boolean> weakConsumer);

		/**
		 * @return a new {@link State} instance based on this builder
		 */
		State build();
	}
}