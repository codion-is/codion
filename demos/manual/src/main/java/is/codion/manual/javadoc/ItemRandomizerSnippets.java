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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.manual.javadoc;

import is.codion.tools.loadtest.randomizer.ItemRandomizer;

import java.util.List;

import static is.codion.tools.loadtest.randomizer.ItemRandomizer.RandomItem.randomItem;

/**
 * The {@link ItemRandomizer} javadoc snippets, each the region of the same name.
 */
final class ItemRandomizerSnippets {

	void usage() {
		String one = "one"; // @start region=usage
		String two = "two";
		String three = "three";

		ItemRandomizer<String> randomizer = ItemRandomizer.randomizer(List.of(
						randomItem(one, 10),
						randomItem(two, 60),
						randomItem(three, 30)));

		//10% chance of getting 'one', 60% chance of getting 'two' and 30% chance of getting 'three'.
		String random = randomizer.get().orElse(null);

		// The weights can be changed
		randomizer.weight(one).set(20); // @end
	}
}
