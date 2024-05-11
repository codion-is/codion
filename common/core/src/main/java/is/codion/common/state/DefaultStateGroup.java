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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

final class DefaultStateGroup implements State.Group {

	private final List<State> members = new ArrayList<>();

	private State previousState;
	private boolean disablingStates = false;

	DefaultStateGroup(State... states) {
		this(Arrays.asList(states));
	}

	DefaultStateGroup(Collection<State> states) {
		for (State state : requireNonNull(states)) {
			add(state);
		}
	}

	@Override
	public void add(State state) {
		requireNonNull(state);
		synchronized (members) {
			if (!members.contains(state)) {
				members.add(state);
				stateChanged(state);
			}
		}
		state.addConsumer(value -> {
			synchronized (members) {
				stateChanged(state);
			}
		});
	}

	@Override
	public void add(Collection<State> states) {
		requireNonNull(states).forEach(this::add);
	}

	private void stateChanged(State current) {
		if (current.get()) {
			previousState = previousState(current);
			disablingStates = true;
			members.stream()
							.filter(state -> state != current)
							.forEach(state -> state.set(false));
			disablingStates = false;
		}
		else if (!disablingStates && previousState != null) {
			previousState.set(true);
			previousState = current;
		}
	}

	private State previousState(State current) {
		return members.stream()
						.filter(state -> state != current)
						.filter(Supplier::get)
						.findFirst()
						.orElse(null);
	}
}
