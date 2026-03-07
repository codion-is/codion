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
 * Copyright (c) 2019 - 2026, Björn Darri Sigurðsson.
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

	private @Nullable State fallback;
	private boolean disabling = false;

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
					disableOthers(state);
				}
				state.addListener(new StateListener(state));
			}
		}
	}

	@Override
	public void add(Collection<State> states) {
		requireNonNull(states).forEach(this::add);
	}

	@Override
	public void fallback(State state) {
		if (!members.contains(requireNonNull(state))) {
			throw new IllegalArgumentException("Fallback state must be a member of the group");
		}
		this.fallback = state;
	}

	private void disableOthers(State current) {
		disabling = true;
		members.stream()
						.filter(state -> state != current)
						.filter(ObservableState::is)
						.forEach(state -> state.set(false));
		disabling = false;
	}

	private final class StateListener implements Runnable {

		private final State state;

		private StateListener(State state) {
			this.state = state;
		}

		@Override
		public void run() {
			synchronized (members) {
				if (disabling) {
					return;
				}
				if (state.is()) {
					disableOthers(state);
				}
				else if (state == fallback) { // activate first non-fallback state
					members.stream()
									.filter(st -> st != fallback)
									.findFirst()
									.ifPresent(st -> st.set(true));
				}
				else if (fallback != null) {
					fallback.set(true);
				}
			}
		}
	}
}
