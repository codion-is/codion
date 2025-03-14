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
package is.codion.tools.loadtest.ui;

import is.codion.tools.loadtest.randomizer.ItemRandomizer;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ItemRandomizerPanelTest {

	@Test
	void test() {
		ItemRandomizer<String> model = ItemRandomizer.itemRandomizer(Arrays.asList(
						ItemRandomizer.RandomItem.randomItem("one", 5),
						ItemRandomizer.RandomItem.randomItem("two", 5),
						ItemRandomizer.RandomItem.randomItem("three", 5)
		));
		ItemRandomizerPanel<String> panel = new ItemRandomizerPanel<>(model);
		assertEquals(model, panel.itemRandomizer());
	}

	@Test
	void constructorNullModel() {
		assertThrows(NullPointerException.class, () -> new ItemRandomizerPanel<>(null));
	}
}
