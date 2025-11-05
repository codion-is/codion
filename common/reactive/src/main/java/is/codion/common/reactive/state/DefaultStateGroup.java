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

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static java.util.Objects.requireNonNull;

final class DefaultStateGroup implements State.Group {

	private final List<State> members = new ArrayList<>();

	private @Nullable State previousState;
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
				if (state.is()) {
					stateChanged(state);
				}
			}
		}
		state.addListener(() -> {
			synchronized (members) {
				stateChanged(state);
			}
		});
	}

	@Override
	public void add(Collection<State> states) {
		requireNonNull(states).forEach(this::add);
	}

	private void stateChanged(State state) {
		if (state.is()) {
			disableOthers(state);
		}
		else if (!disablingStates) {
			enablePrevious(state);
		}
	}

	private void disableOthers(State current) {
		previousState = previousState(current);
		disablingStates = true;
		members.stream()
						.filter(state -> state != current)
						.filter(ObservableState::is)
						.forEach(state -> state.set(false));
		disablingStates = false;
	}

	private void enablePrevious(State current) {
		if (previousState != null) {
			previousState.set(true);
		}
		else if (members.size() > 1) {
			//fallback to the next state
			int index = members.indexOf(current);
			members.get(index == members.size() - 1 ? 0 : index + 1).set(true);
		}
		previousState = current;
	}

	private @Nullable State previousState(State current) {
		return members.stream()
						.filter(state -> state != current)
						.filter(ObservableState::is)
						.findFirst()
						.orElse(null);
	}
}
