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
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.model.randomizer;

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
		assertEquals(3, model.itemCount());
		assertEquals(3, model.items().size());

		model.incrementWeight(three);
		assertEquals(three, model.randomItem());

		model.decrementWeight(three);

		model.incrementWeight(one);
		assertEquals(1, model.weight(one));
		model.incrementWeight(two);
		assertEquals(1, model.weight(two));
		model.incrementWeight(three);
		assertEquals(1, model.weight(three));

		assertEquals(Double.valueOf(1 / 3d), Double.valueOf(model.weightRatio(one)));

		model.incrementWeight(three);
		assertEquals(2, model.weight(three));
		model.incrementWeight(three);
		assertEquals(3, model.weight(three));
		model.incrementWeight(three);
		assertEquals(4, model.weight(three));

		assertEquals(Double.valueOf(4 / 6d), Double.valueOf(model.weightRatio(three)));

		model.incrementWeight(one);
		assertEquals(2, model.weight(one));

		assertEquals(Double.valueOf(2 / 7d), Double.valueOf(model.weightRatio(one)));

		model.incrementWeight(two);
		assertEquals(2, model.weight(two));

		assertEquals(Double.valueOf(2 / 8d), Double.valueOf(model.weightRatio(one)));
		assertEquals(Double.valueOf(2 / 8d), Double.valueOf(model.weightRatio(two)));
		assertEquals(Double.valueOf(4 / 8d), Double.valueOf(model.weightRatio(three)));

		model.decrementWeight(one);
		assertEquals(1, model.weight(one));
		model.decrementWeight(two);
		assertEquals(1, model.weight(two));

		model.decrementWeight(one);
		assertEquals(0, model.weight(one));
		model.decrementWeight(two);
		assertEquals(0, model.weight(two));

		model.setItemEnabled(one, false);
		assertFalse(model.isItemEnabled(one));

		model.setItemEnabled(one, true);
		assertTrue(model.isItemEnabled(one));

		try {
			model.decrementWeight(one);
			fail();
		}
		catch (IllegalStateException ignored) {/*ignored*/}
	}
}
