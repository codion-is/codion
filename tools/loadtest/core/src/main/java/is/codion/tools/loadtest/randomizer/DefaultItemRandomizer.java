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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

final class DefaultItemRandomizer<T> implements ItemRandomizer<T> {

	private final List<RandomItem<T>> items;
	private final Random random = new Random();

	DefaultItemRandomizer(Collection<RandomItem<T>> items) {
		this.items = unmodifiableList(new ArrayList<>(requireNonNull(items)));
	}

	@Override
	public Value<Integer> weight(T item) {
		return randomItem(item).weight();
	}

	@Override
	public State enabled(T item) {
		return randomItem(item).enabled();
	}

	@Override
	public Collection<RandomItem<T>> items() {
		return items;
	}

	@Override
	public Optional<T> get() {
		int totalWeights = totalWeights();
		if (totalWeights == 0) {
			return Optional.empty();
		}

		int randomNumber = random.nextInt(totalWeights + 1);
		int position = 0;
		for (RandomItem<T> item : items) {
			int weight = item.enabled().is() ? item.weight().getOrThrow() : 0;
			position += weight;
			if (randomNumber <= position && weight > 0) {
				return Optional.of(item.item());
			}
		}

		return Optional.empty();
	}

	RandomItem<T> randomItem(T item) {
		requireNonNull(item);
		for (RandomItem<T> randomItem : items) {
			if (randomItem.item() == item) {
				return randomItem;
			}
		}

		throw new IllegalArgumentException("Item not found: " + item + ": " + item.getClass());
	}

	private int totalWeights() {
		return items.stream()
						.filter(item -> item.enabled().is())
						.map(RandomItem::weight)
						.mapToInt(Value::getOrThrow)
						.sum();
	}
}
