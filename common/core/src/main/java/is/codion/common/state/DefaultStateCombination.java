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
 * Copyright (c) 2019 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.state;

import is.codion.common.Conjunction;
import is.codion.common.observer.Observer;

import java.util.Collection;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

final class DefaultStateCombination implements State.Combination {

	private final DefaultObservableState observableState;
	private final List<StateCombinationConsumer> stateListeners;
	private final Conjunction conjunction;

	private final Lock updateLock = new Lock() {};
	private volatile boolean value;

	DefaultStateCombination(Conjunction conjunction, ObservableState... states) {
		this(conjunction, states == null ? emptyList() : asList(states));
	}

	DefaultStateCombination(Conjunction conjunction, Collection<? extends ObservableState> states) {
		this.conjunction = requireNonNull(conjunction);
		this.observableState = new DefaultObservableState(this, false);
		this.stateListeners = states.stream() // Create consumers but don't register them yet
						.map(state -> new StateCombinationConsumer(requireNonNull(state)))
						.collect(toList());
		this.value = calculateValue(); // Calculate initial value before registering listeners
		this.stateListeners.forEach(StateCombinationConsumer::registerListener);
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder("Combination");
		stringBuilder.append(toString(conjunction)).append(value);
		for (StateCombinationConsumer listener : stateListeners) {
			stringBuilder.append(", ").append(listener.state);
		}

		return stringBuilder.toString();
	}

	@Override
	public Conjunction conjunction() {
		return conjunction;
	}

	@Override
	public boolean is() {
		return value;
	}

	@Override
	public ObservableState not() {
		return observableState.not();
	}

	@Override
	public Observer<Boolean> observer() {
		return observableState.observer();
	}

	private boolean calculateValue() {
		if (stateListeners.isEmpty()) {
			return false;
		}
		for (StateCombinationConsumer listener : stateListeners) {
			boolean is = listener.state.is();
			if (conjunction == Conjunction.AND) {
				if (!is) {
					return false;
				}
			}
			else if (is) {
				return true;
			}
		}

		return conjunction == Conjunction.AND;
	}

	private final class StateCombinationConsumer implements Runnable {

		private final ObservableState state;

		private StateCombinationConsumer(ObservableState state) {
			this.state = state;
		}

		@Override
		public void run() {
			boolean oldValue = value;
			boolean newValue = calculateValue();
			if (oldValue != newValue) {
				synchronized (updateLock) {
					value = newValue;
				}
				observableState.notifyObservers(newValue, oldValue);
			}
		}

		private void registerListener() {
			state.addListener(this);
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

	private interface Lock {}
}
