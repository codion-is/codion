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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Objects.requireNonNull;

final class DefaultStateGroup implements State.Group {

	private final List<WeakReference<State>> members = new ArrayList<>();

	DefaultStateGroup(State... states) {
		for (State state : requireNonNull(states)) {
			add(state);
		}
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
			if (members.stream().anyMatch(reference -> reference.get() == state)) {
				return;//no duplicate states
			}
			members.add(new WeakReference<>(state));
			stateChanged(state);
		}
		state.addDataListener(value -> {
			synchronized (members) {
				if (value) {
					stateChanged(state);
				}
			}
		});
	}

	@Override
	public void add(Collection<State> states) {
		requireNonNull(states).forEach(this::add);
	}

	private void stateChanged(State state) {
		members.removeIf(reference -> reference.get() == null);
		if (state.get()) {
			members.stream()
							.map(WeakReference::get)
							.filter(s -> s != state)
							.forEach(s -> s.set(false));
		}
	}
}
