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

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultItemRandomizerTest {

	@Test
	void test() {
		Object one = "one";
		Object two = "two";
		Object three = "three";

		DefaultItemRandomizer<Object> model = new DefaultItemRandomizer<>(Arrays.asList(
						ItemRandomizer.RandomItem.randomItem(one, 0),
						ItemRandomizer.RandomItem.randomItem(two, 0),
						ItemRandomizer.RandomItem.randomItem(three, 0)
		));
		assertEquals(3, model.items().size());

		model.weight(three).map(value -> value + 1);
		assertEquals(three, model.get().orElse(null));

		model.weight(three).map(value -> value - 1);

		model.weight(one).map(value -> value + 1);
		assertTrue(model.weight(one).is(1));
		model.weight(two).map(value -> value + 1);
		assertTrue(model.weight(two).is(1));
		model.weight(three).map(value -> value + 1);
		assertTrue(model.weight(three).is(1));

		model.weight(three).map(value -> value + 1);
		assertTrue(model.weight(three).is(2));
		model.weight(three).map(value -> value + 1);
		assertTrue(model.weight(three).is(3));
		model.weight(three).map(value -> value + 1);
		assertTrue(model.weight(three).is(4));

		model.weight(one).map(value -> value + 1);
		assertTrue(model.weight(one).is(2));

		model.weight(two).map(value -> value + 1);
		assertTrue(model.weight(two).is(2));

		model.weight(one).map(value -> value - 1);
		assertTrue(model.weight(one).is(1));
		model.weight(two).map(value -> value - 1);
		assertTrue(model.weight(two).is(1));

		model.weight(one).map(value -> value - 1);
		assertTrue(model.weight(one).is(0));
		model.weight(two).map(value -> value - 1);
		assertTrue(model.weight(two).is(0));

		model.enabled(one).set(false);

		model.enabled(one).set(true);

		try {
			model.weight(one).map(value -> value - 1);
			fail();
		}
		catch (IllegalStateException ignored) {/*ignored*/}
	}
}
