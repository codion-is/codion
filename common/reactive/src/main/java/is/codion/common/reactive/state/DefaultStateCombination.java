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
package is.codion.common.reactive.state;

import is.codion.common.reactive.observer.Observer;

import java.util.Collection;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

final class DefaultStateCombination implements ObservableState {

	private final DefaultObservableState observableState;
	private final List<StateCombinationConsumer> stateListeners;
	private final boolean and;

	private final Lock updateLock = new Lock() {};
	private volatile boolean value;

	DefaultStateCombination(boolean and, ObservableState... states) {
		this(and, states == null ? emptyList() : asList(states));
	}

	DefaultStateCombination(boolean and, Collection<? extends ObservableState> states) {
		this.and = and;
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
		stringBuilder.append(toString(and)).append(value);
		for (StateCombinationConsumer listener : stateListeners) {
			stringBuilder.append(", ").append(listener.state);
		}

		return stringBuilder.toString();
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
			if (and) {
				if (!is) {
					return false;
				}
			}
			else if (is) {
				return true;
			}
		}

		return and;
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

	private static String toString(boolean and) {
		return and ? " and " : " or ";
	}

	private interface Lock {}
}
