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
package is.codion.common.reactive.value;

import org.jspecify.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.Collections.newSetFromMap;

/**
 * A class for linking two values.
 * <p>The reciprocal update flags prevent a single-threaded update from cycling back on itself; linking, like
 * value modification in general, is single-thread by design (typically an application UI thread) and is not
 * safe under concurrent updates from multiple threads.
 * <p>The guard flags are volatile for cross-thread visibility alone - an application updating from one
 * thread at a time, but not always the same one, sees the guards where it sees the values. It buys no
 * atomicity and does not soften the contract above.
 * @param <T> the type of the value
 */
final class ValueLink<T> {

	private final Value<T> linkedValue;
	private final Value<T> originalValue;

	private final Consumer<T> updateLinkedValue = this::updateLinkedValue;
	private final Consumer<T> updateOriginalValue = this::updateOriginalValue;

	private final LinkedValidator<T> linkedValidator;
	private final LinkedValidator<T> originalValidator;

	private volatile boolean updatingLinked = false;
	private volatile boolean updatingOriginal = false;

	/**
	 * Creates a new ValueLink
	 * @param linkedValue the value to link to the original value
	 * @param originalValue the original value
	 */
	ValueLink(Value<T> linkedValue, Value<T> originalValue) {
		preventLinkCycle(linkedValue, originalValue);
		this.linkedValue = linkedValue;
		this.originalValue = originalValue;
		this.linkedValidator = new LinkedValidator<>(linkedValue);
		this.originalValidator = new LinkedValidator<>(originalValue);
		this.linkedValidator.excluded = originalValidator;
		this.originalValidator.excluded = linkedValidator;
		linkedValue.set(originalValue.get());
		originalValue.addConsumer(updateLinkedValue);
		linkedValue.addConsumer(updateOriginalValue);
		originalValue.addValidator(linkedValidator);
		linkedValue.addValidator(originalValidator);
	}

	void unlink() {
		linkedValue.removeConsumer(updateOriginalValue);
		originalValue.removeConsumer(updateLinkedValue);
		linkedValue.removeValidator(originalValidator);
		originalValue.removeValidator(linkedValidator);
	}

	private static <T> void preventLinkCycle(Value<T> linkedValue, Value<T> originalValue) {
		if (originalValue == linkedValue) {
			throw new IllegalArgumentException("A Value can not be linked to itself");
		}
		preventLinkCycle(linkedValue, originalValue, newSetFromMap(new IdentityHashMap<>()));
	}

	private static <T> void preventLinkCycle(Value<T> linkedValue, Value<T> originalValue, Set<Value<T>> visited) {
		if (!visited.add(originalValue)) {
			//already walked, a diamond in the link graph reaches the same value by more than one path
			return;
		}
		if (!(originalValue instanceof BaseValue)) {
			//a Value implemented outside the framework keeps its own links, if it has any, so the chain
			//ends here - see the note on Value.link(Value)
			return;
		}
		Set<Value<T>> linkedValues = ((BaseValue<T>) originalValue).linkedValues();
		if (linkedValues.contains(linkedValue)) {
			throw new IllegalStateException("Cyclical value link detected");
		}
		//walk up the origin chain, keeping the candidate linkedValue fixed, to detect a transitive cycle
		linkedValues.forEach(value -> preventLinkCycle(linkedValue, value, visited));
	}

	private void updateLinkedValue(T value) {
		if (!updatingOriginal) {
			updatingLinked = true;
			try {
				linkedValue.set(value);
			}
			finally {
				updatingLinked = false;
			}
		}
	}

	private void updateOriginalValue(T value) {
		if (!updatingLinked) {
			updatingOriginal = true;
			try {
				originalValue.set(value);
			}
			finally {
				updatingOriginal = false;
			}
		}
	}

	/**
	 * Runs the far end's validators, so that either end of a link validates against both and the pair can
	 * not come to hold a value one of them rejects.
	 * <p>The far end's validators include the bridges of its own links, so validating walks the whole
	 * connected graph. {@link #excluded} skips the immediate partner, which is enough to unwind a chain,
	 * but not a value linked to two others that share an origin - there the walk arrives back by the other
	 * path. The re-entrancy flag ends it: a bridge already validating further up the walk has run, or is
	 * running, its validators, and returns rather than running them again.
	 * <p>A {@link Value} implemented outside the framework keeps its validators to itself, so that one goes
	 * through the public gate instead. Same guarantee, one more pass over each end's validators - the gate
	 * runs the partner bridge as well, which the flag then stops.
	 */
	private static final class LinkedValidator<T> implements Value.Validator<T> {

		private final Value<T> linkedValue;

		private Value.@Nullable Validator<T> excluded;
		private volatile boolean validating = false;

		private LinkedValidator(Value<T> linkedValue) {
			this.linkedValue = linkedValue;
		}

		@Override
		public void validate(@Nullable T value) {
			if (validating) {
				return;
			}
			validating = true;
			try {
				if (linkedValue instanceof BaseValue) {
					((BaseValue<T>) linkedValue).validators()
									.stream()
									.filter(validator -> validator != excluded)
									.forEach(validator -> validator.validate(value));
				}
				else {
					linkedValue.validate(value);
				}
			}
			finally {
				validating = false;
			}
		}
	}
}
