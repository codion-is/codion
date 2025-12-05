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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.reactive.state;

import is.codion.common.reactive.value.ValueSet;

import java.util.function.Consumer;

final class ValueSetContains<T> implements Runnable, Consumer<Boolean> {

	private final ValueSet<T> valueSet;
	private final T value;
	private final State state;

	ValueSetContains(ValueSet<T> valueSet, T value) {
		this.valueSet = valueSet;
		this.state = State.state(valueSet.contains(value));
		this.value = value;
		this.valueSet.addWeakListener(this);
		this.state.addConsumer(this);
	}

	@Override
	public void run() {
		state.set(valueSet.contains(value));
	}

	@Override
	public void accept(Boolean contains) {
		if (contains && !valueSet.contains(value)) {
			valueSet.add(value);
		}
		else if (!contains && valueSet.contains(value)) {
			valueSet.remove(value);
		}
	}

	State state() {
		return state;
	}
}
