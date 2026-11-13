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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.reactive.state;

import is.codion.common.reactive.value.Value.Validator;
import is.codion.common.reactive.value.ValueSet;

import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

final class ValueSetContains<T> implements Runnable, Consumer<Boolean>, Validator<Boolean> {

	private final ValueSet<T> valueSet;
	private final T value;
	private final State state;

	ValueSetContains(ValueSet<T> valueSet, T value) {
		this.valueSet = valueSet;
		this.state = State.state(valueSet.contains(value));
		this.value = value;
		this.valueSet.addWeakListener(this);
		this.state.addValidator(this);
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

	/**
	 * Asks the set before the state moves, so that a change the set refuses, locked or invalid, moves neither.
	 */
	@Override
	public void validate(@Nullable Boolean contains) {
		boolean add = Boolean.TRUE.equals(contains);
		if (add != valueSet.contains(value)) {
			if (valueSet.locked().is()) {
				throw new IllegalStateException("Value is locked and can not be changed");
			}
			Set<T> values = new LinkedHashSet<>(valueSet.get());
			if (add) {
				values.add(value);
			}
			else {
				values.remove(value);
			}
			valueSet.validate(values);
		}
	}

	State state() {
		return state;
	}
}
