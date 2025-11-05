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
 * Copyright (c) 2010 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.tools.loadtest.randomizer;

import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.Value;

import java.util.Collection;
import java.util.Optional;

/**
 * ItemRandomizer provides a way to randomly choose an item based on a weight value.
 * {@snippet :
 * Item one = new Item();
 * Item two = new Item();
 * Item three = new Item();
 *
 * ItemRandomizer<Object> randomizer = ItemRandomizer.randomizer(Arrays.asList(one, two, three));
 *
 * randomizer.setWeight(one, 10);
 * randomizer.setWeight(two, 60);
 * randomizer.setWeight(three, 30);
 *
 * //10% chance of getting 'one', 60% chance of getting 'two' and 30% chance of getting 'three'.
 * Item random = randomizer.get().orElse(null);
 *}
 * For instances use the following factory functions: {@link #randomizer(Collection)}
 * @param <T> the type of item this random item model returns
 */
public interface ItemRandomizer<T> {

	/**
	 * @return the items in this model.
	 */
	Collection<RandomItem<T>> items();

	/**
	 * @return the {@link Value} controlling the weight
	 */
	Value<Integer> weight(T item);

	/**
	 * @param item the item
	 * @return the item enabled state
	 */
	State enabled(T item);

	/**
	 * Fetches a random item from this model based on the item weights.
	 * @return a randomly chosen item or an empty {@link Optional} in case no item is enabled or the total weights are zero
	 */
	Optional<T> get();

	/**
	 * Instantiates a new {@link ItemRandomizer}.
	 * @param <T> the item type
	 * @param items the items to randomize
	 * @return a new {@link ItemRandomizer}
	 */
	static <T> ItemRandomizer<T> randomizer(Collection<RandomItem<T>> items) {
		return new DefaultItemRandomizer<>(items);
	}

	/**
	 * Wraps an item for usage in the ItemRandomizer.
	 * For instances use the {@link RandomItem#randomItem(Object, int)} factory method.
	 * @param <T> the type being wrapped
	 */
	interface RandomItem<T> {

		/**
		 * @return the weight assigned to this item
		 */
		Value<Integer> weight();

		/**
		 * @return the enabled state
		 */
		State enabled();

		/**
		 * @return the item this random item represents
		 */
		T item();

		/**
		 * Instantiates a new {@link RandomItem} instance.
		 * @param item the item
		 * @param weight the random selection weight to assign to this item
		 * @param <T> the item type
		 * @return a new {@link RandomItem} instance.
		 */
		static <T> RandomItem<T> randomItem(T item, int weight) {
			return new DefaultRandomItem<>(item, weight);
		}
	}
}
