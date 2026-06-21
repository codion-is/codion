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
package is.codion.common.model.selection;

import is.codion.common.model.filter.FilterModel.IncludedItems;
import is.codion.common.model.selection.MultiSelection.IndexedItems;
import is.codion.common.reactive.value.Value;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.junit.jupiter.api.Assertions.*;

public final class DefaultMultiSelectionTest {

	@Test
	void multiSelection() {
		TestItems items = new TestItems(asList("a", "b", "c", "d"));
		MultiSelection<String> selection = new DefaultMultiSelection<>(items);

		assertTrue(selection.empty().is());
		assertEquals(0, selection.count());
		assertNull(selection.item().get());
		assertNull(selection.index().get());

		selection.item().set("b");
		assertFalse(selection.empty().is());
		assertTrue(selection.single().is());
		assertFalse(selection.multiple().is());
		assertEquals(1, selection.count());
		assertEquals(1, selection.index().get());
		assertEquals("b", selection.item().get());

		selection.indexes().set(asList(1, 3));
		assertEquals(2, selection.count());
		assertTrue(selection.multiple().is());
		assertEquals(asList(1, 3), selection.indexes().get());
		assertEquals(asList("b", "d"), selection.items().get());
		// index() tracks the minimum selected index
		assertEquals(1, selection.index().get());

		selection.indexes().add(0);
		assertEquals(asList(0, 1, 3), selection.indexes().get());
		assertEquals(0, selection.index().get());

		selection.items().remove("b");
		assertEquals(asList(0, 3), selection.indexes().get());
		assertFalse(selection.indexes().contains(1));

		selection.selectAll();
		assertEquals(4, selection.count());
		assertEquals(asList("a", "b", "c", "d"), selection.items().get());

		selection.clear();
		assertTrue(selection.empty().is());
		assertEquals(0, selection.count());
	}

	@Test
	void singleSelectionMode() {
		TestItems items = new TestItems(asList("a", "b", "c"));
		MultiSelection<String> selection = new DefaultMultiSelection<>(items);

		selection.singleSelection().set(true);
		selection.indexes().set(asList(0, 1, 2)); // request many...
		assertEquals(1, selection.count());        // ...but single mode keeps one
		assertTrue(selection.single().is());
	}

	@Test
	void incrementDecrement() {
		TestItems items = new TestItems(asList("a", "b", "c"));
		MultiSelection<String> selection = new DefaultMultiSelection<>(items);

		selection.indexes().increment(); // empty -> first
		assertEquals(0, selection.index().get());
		selection.indexes().increment();
		assertEquals(1, selection.index().get());
		selection.index().set(2);
		selection.indexes().increment(); // wraps around
		assertEquals(0, selection.index().get());
		selection.indexes().decrement(); // wraps back
		assertEquals(2, selection.index().get());
	}

	/**
	 * Minimal {@link IncludedItems} over a fixed list; observable surface delegated to a {@link Value}.
	 * Only the index/item lookups {@link DefaultMultiSelection} actually uses are implemented.
	 */
	private static final class TestItems implements IndexedItems<String> {

		private final List<String> list = new ArrayList<>();
		private final Value<List<String>> value = Value.nonNull(unmodifiableList(new ArrayList<>()));

		private TestItems(List<String> items) {
			this.list.addAll(items);
			this.value.set(unmodifiableList(new ArrayList<>(items)));
		}

		@Override
		public List<String> get() {
			return unmodifiableList(list);
		}

		@Override
		public String get(int index) {
			return list.get(index);
		}

		@Override
		public int indexOf(String item) {
			return list.indexOf(item);
		}

		@Override
		public int size() {
			return list.size();
		}
	}
}
