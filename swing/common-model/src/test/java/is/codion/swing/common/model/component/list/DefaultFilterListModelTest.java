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
package is.codion.swing.common.model.component.list;

import is.codion.common.Text;

import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

final class DefaultFilterListModelTest {

	private static final String ONE = "One";
	private static final String TWO = "Two";
	private static final String THREE = "Three";
	private static final String FOUR = "Four";

	@Test
	void test() {
		List<String> items = asList(ONE, TWO, THREE);
		FilterListModel<String> unsorted = FilterListModel.<String>builder()
						.supplier(() -> items)
						.async(false)
						.comparator(null)
						.build();
		assertFalse(unsorted.sort().sorted());
		unsorted.sort().descending();
		assertFalse(unsorted.sort().sorted());
		unsorted.sort().clear();
		assertFalse(unsorted.sort().sorted());

		FilterListModel<String> model = FilterListModel.builder(items)
						.comparator(Text.collator())
						.build();
		model.selection().item().set(TWO);
		assertEquals(items.size(), model.items().count());
		assertEquals(items.size(), model.getSize());
		assertTrue(model.sort().sorted());
		model.sort().descending();
		assertTrue(model.sort().sorted());
		assertEquals(0, model.items().visible().indexOf(TWO));
		assertEquals(THREE, model.getElementAt(1));
		assertEquals(TWO, model.selection().item().get());
		model.sort().ascending();
		assertEquals(2, model.items().visible().indexOf(TWO));
		assertEquals(TWO, model.selection().item().get());
		model.sort().clear();
		assertFalse(model.sort().sorted());
		model.items().refresh();
		assertEquals(0, model.items().visible().indexOf(ONE));

		model.items().replace(TWO, FOUR);
		assertEquals(FOUR, model.getElementAt(2));
		assertEquals(FOUR, model.selection().item().get());

		model.items().visible().predicate().set(string -> !string.startsWith("T"));
		assertEquals(2, model.items().visible().count());
		assertEquals(1, model.items().filtered().count());
		assertTrue(model.items().visible().contains(ONE));
		assertTrue(model.items().visible().contains(FOUR));
		assertTrue(model.items().filtered().contains(THREE));
	}
}
