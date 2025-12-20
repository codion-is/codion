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
package is.codion.common.reactive.state;

import is.codion.common.reactive.observer.Observable;
import is.codion.common.reactive.value.Value;
import is.codion.common.reactive.value.Value.Notify;
import is.codion.common.reactive.value.Value.Validator;
import is.codion.common.reactive.value.ValueSet;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

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
	 * Returns an {@link ObservableState} notified when the state changes (default) or is set, depending on the {@link Notify} policy
	 * @return an {@link ObservableState} notified each time the state changes or is set
	 * @see Builder#notify(Notify)
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
	 * @return an {@link ObservableState} active when the given observable
	 * has a value present, determined by {@link Observable#optional()}.
	 */
	static <T> ObservableState present(Observable<T> observable) {
		return new ObservableIsPresent<>(requireNonNull(observable)).observable();
	}

	/**
	 * Creates a {@link State} synchronized with whether the given value is contained in the given {@link ValueSet}.
	 * The state is bidirectionally synchronized:
	 * <ul>
	 *   <li>When the value is added to or removed from the set, the state is updated accordingly</li>
	 *   <li>When the state is set to {@code true}, the value is added to the set (if not already present)</li>
	 *   <li>When the state is set to {@code false}, the value is removed from the set (if present)</li>
	 * </ul>
	 * The initial state reflects whether the value is currently contained in the set.
	 * <p>
	 * The synchronization is maintained via a weak listener on the {@link ValueSet}, meaning:
	 * <ul>
	 *   <li>As long as the returned {@link State} is reachable, the synchronization remains active</li>
	 *   <li>Once the returned {@link State} is no longer reachable and is garbage collected,
	 *       the synchronization is automatically cleaned up</li>
	 * </ul>
	 * <pre>
	 * ValueSet&lt;String&gt; tags = ValueSet.valueSet();
	 * State containsImportant = State.contains(tags, "important");
	 *
	 * // State → Set
	 * containsImportant.set(true);
	 * assertTrue(tags.contains("important"));
	 *
	 * // Set → State
	 * tags.remove("important");
	 * assertFalse(containsImportant.is());
	 * </pre>
	 * @param <T> the value type
	 * @param valueSet the value set
	 * @param value the value
	 * @return a state synchronized with the set's containment of the value
	 */
	static <T> State contains(ValueSet<T> valueSet, T value) {
		return new ValueSetContains<>(requireNonNull(valueSet), value).state();
	}

	/**
	 * Creates a new {@link ObservableState} instance using AND.
	 * @param observableStates the state observers to base this state combination on
	 * @return a new {@link ObservableState} instance
	 */
	static ObservableState and(ObservableState... observableStates) {
		return new DefaultStateCombination(true, observableStates);
	}

	/**
	 * Creates a new {@link ObservableState} instance using AND.
	 * @param observableStates the state observers to base this state combination on
	 * @return a new {@link ObservableState} instance
	 */
	static ObservableState and(Collection<? extends ObservableState> observableStates) {
		return new DefaultStateCombination(true, observableStates);
	}

	/**
	 * Creates a new {@link ObservableState} instance using OR.
	 * @param observableStates the state observers to base this state combination on
	 * @return a new {@link ObservableState} instance
	 */
	static ObservableState or(ObservableState... observableStates) {
		return new DefaultStateCombination(false, observableStates);
	}

	/**
	 * Creates a new {@link ObservableState} instance using OR.
	 * @param observableStates the state observers to base this state combination on
	 * @return a new {@link ObservableState} instance
	 */
	static ObservableState or(Collection<? extends ObservableState> observableStates) {
		return new DefaultStateCombination(false, observableStates);
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
		 * @param group the {@link Group} to add the state to
		 * @return this builder instance
		 */
		Builder group(Group group);

		/**
		 * @param locked true if the state should be locked
		 * @return this builder instance
		 */
		Builder locked(boolean locked);

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
		 * Adds a conditional listener
		 * @param value the value on which to run
		 * @param runnable the runnable to run
		 * @return this builder instance
		 */
		Builder when(boolean value, Runnable runnable);

		/**
		 * Adds a conditional consumer
		 * @param value the value to consume
		 * @param consumer the consumer to use
		 * @return this builder instance
		 */
		Builder when(boolean value, Consumer<? super Boolean> consumer);

		/**
		 * Adds a conditional listener
		 * @param predicate the predicate on which to run
		 * @param runnable the runnable to run
		 * @return this builder instance
		 */
		Builder when(Predicate<Boolean> predicate, Runnable runnable);

		/**
		 * Adds a conditional consumer
		 * @param predicate the predicate on which to consume the state
		 * @param consumer the consumer to use
		 * @return this builder instance
		 */
		Builder when(Predicate<Boolean> predicate, Consumer<? super Boolean> consumer);

		/**
		 * @return a new {@link State} instance based on this builder
		 */
		State build();
	}
}