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
 * Copyright (c) 2019 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.state;

import is.codion.common.Conjunction;
import is.codion.common.observer.Observer;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

final class DefaultStateCombination implements State.Combination {

	private final DefaultObservableState observableState;
	private final List<StateCombinationConsumer> stateListeners = new ArrayList<>();
	private final Conjunction conjunction;

	DefaultStateCombination(Conjunction conjunction, ObservableState... states) {
		this(conjunction, states == null ? Collections.emptyList() : Arrays.asList(states));
	}

	DefaultStateCombination(Conjunction conjunction, Collection<? extends ObservableState> states) {
		this.observableState = new DefaultObservableState(this, false);
		this.conjunction = requireNonNull(conjunction);
		for (ObservableState state : requireNonNull(states)) {
			add(state);
		}
	}

	@Override
	public String toString() {
		synchronized (observableState) {
			StringBuilder stringBuilder = new StringBuilder("Combination");
			stringBuilder.append(toString(conjunction)).append(observableState);
			for (StateCombinationConsumer listener : stateListeners) {
				stringBuilder.append(", ").append(listener.state);
			}

			return stringBuilder.toString();
		}
	}

	@Override
	public Conjunction conjunction() {
		return conjunction;
	}

	@Override
	public void add(ObservableState state) {
		requireNonNull(state);
		synchronized (observableState) {
			if (!findConsumer(state).isPresent()) {
				boolean previousValue = get();
				stateListeners.add(new StateCombinationConsumer(state));
				observableState.notifyObservers(get(), previousValue);
			}
		}
	}

	@Override
	public void remove(ObservableState state) {
		requireNonNull(state);
		synchronized (observableState) {
			boolean previousValue = get();
			findConsumer(state).ifPresent(consumer -> {
				state.removeConsumer(consumer);
				stateListeners.remove(consumer);
				observableState.notifyObservers(get(), previousValue);
			});
		}
	}

	@Override
	public Boolean get() {
		synchronized (observableState) {
			return get(conjunction, null, false);
		}
	}

	@Override
	public ObservableState not() {
		return observableState.not();
	}

	@Override
	public Observer<Boolean> observer() {
		return observableState.observer();
	}

	private boolean get(Conjunction conjunction, @Nullable ObservableState exclude, boolean excludeReplacement) {
		for (StateCombinationConsumer listener : stateListeners) {
			ObservableState state = listener.state;
			boolean value = state.equals(exclude) ? excludeReplacement : state.get();
			if (conjunction == Conjunction.AND) {
				if (!value) {
					return false;
				}
			}
			else if (value) {
				return true;
			}
		}

		return conjunction == Conjunction.AND;
	}

	private Optional<StateCombinationConsumer> findConsumer(ObservableState state) {
		return stateListeners.stream()
						.filter(listener -> listener.state.equals(state))
						.findFirst();
	}

	private final class StateCombinationConsumer implements Consumer<Boolean> {
		private final ObservableState state;

		private StateCombinationConsumer(ObservableState state) {
			this.state = state;
			this.state.addConsumer(this);
		}

		@Override
		public void accept(Boolean newValue) {
			observableState.notifyObservers(get(), previousState(state, !newValue));
		}

		private boolean previousState(ObservableState excludeState, boolean previousValue) {
			synchronized (observableState) {
				return get(conjunction, excludeState, previousValue);
			}
		}
	}

	private static String toString(Conjunction conjunction) {
		switch (conjunction) {
			case AND:
				return " and ";
			case OR:
				return " or ";
			default:
				throw new IllegalArgumentException("Unknown conjunction: " + conjunction);
		}
	}
}
