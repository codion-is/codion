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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.tools.loadtest.randomizer;

import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.AbstractValue;
import is.codion.common.reactive.value.Value;

/**
 * A class encapsulating an Object item and an integer weight value.
 */
final class DefaultRandomItem<T> implements ItemRandomizer.RandomItem<T> {

	private static final String WEIGHT_CAN_NOT_BE_NEGATIVE = "Weight can not be negative";

	private final T item;
	private final Value<Integer> weight;
	private final State enabled = State.state(true);

	/**
	 * Instantiates a new RandomItem
	 * @param item the item
	 * @param weight the random selection weight to assign to this item
	 */
	DefaultRandomItem(T item, int weight) {
		if (weight < 0) {
			throw new IllegalArgumentException(WEIGHT_CAN_NOT_BE_NEGATIVE);
		}
		this.item = item;
		this.weight = new DefaultWeight(weight);
	}

	@Override
	public Value<Integer> weight() {
		return weight;
	}

	@Override
	public State enabled() {
		return enabled;
	}

	@Override
	public T item() {
		return item;
	}

	@Override
	public String toString() {
		return item.toString();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ItemRandomizer.RandomItem && (((ItemRandomizer.RandomItem<T>) obj).item().equals(item));
	}

	@Override
	public int hashCode() {
		return item.hashCode();
	}

	private static final class DefaultWeight extends AbstractValue<Integer> {

		private static final Validator<? super Integer> VALIDATOR = value -> {
			if (value < 0) {
				throw new IllegalStateException(WEIGHT_CAN_NOT_BE_NEGATIVE);
			}
		};

		private int weight;

		private DefaultWeight(int weight) {
			super(0);
			addValidator(VALIDATOR);
			this.weight = weight;
		}

		@Override
		protected Integer getValue() {
			return weight;
		}

		@Override
		protected void setValue(Integer value) {
			weight = value;
		}
	}
}
