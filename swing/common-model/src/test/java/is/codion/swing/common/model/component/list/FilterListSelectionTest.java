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
 * Copyright (c) 2013 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.component.list;

import is.codion.swing.common.model.component.table.FilterTableModel;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static is.codion.swing.common.model.component.list.FilterListSelection.filterListSelection;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static javax.swing.ListSelectionModel.*;
import static org.junit.jupiter.api.Assertions.*;

public class FilterListSelectionTest {

	private final FilterListSelection<String> testModel;

	public FilterListSelectionTest() {
		List<String> data = asList("A", "B", "C");
		FilterTableModel<String, Integer> tableModel =
						FilterTableModel.builder(new FilterTableModel.TableColumns<String, Integer>() {
											@Override
											public List<Integer> identifiers() {
												return singletonList(0);
											}

											@Override
											public Class<?> columnClass(Integer identifier) {
												return String.class;
											}

											@Override
											public Object value(String row, Integer identifier) {
												return row;
											}
										})
										.supplier(() -> data)
										.build();
		tableModel.items().refresh();

		testModel = filterListSelection(tableModel.items());
	}

	@Test
	void test() {
		testModel.index().set(0);
		assertTrue(testModel.items().contains("A"));
		assertNotNull(testModel.item().get());
		testModel.clearSelection();
		assertFalse(testModel.items().contains("A"));
		assertNull(testModel.item().get());
	}

	@Test
	void singleSelection() {
		assertFalse(testModel.singleSelection().get());
		testModel.setSelectionMode(SINGLE_SELECTION);
		assertTrue(testModel.singleSelection().get());
		testModel.setSelectionMode(SINGLE_INTERVAL_SELECTION);
		assertFalse(testModel.singleSelection().get());
		testModel.setSelectionMode(MULTIPLE_INTERVAL_SELECTION);
		assertFalse(testModel.singleSelection().get());
		testModel.setSelectionMode(SINGLE_SELECTION);
		assertTrue(testModel.singleSelection().get());
		testModel.singleSelection().set(false);
		assertEquals(MULTIPLE_INTERVAL_SELECTION, testModel.getSelectionMode());
		testModel.setSelectionMode(SINGLE_SELECTION);
		assertTrue(testModel.singleSelection().get());
		assertEquals(SINGLE_SELECTION, testModel.getSelectionMode());
	}

	@Test
	void events() {
		AtomicInteger emptyCounter = new AtomicInteger();
		testModel.empty().addListener(emptyCounter::incrementAndGet);
		testModel.index().set(0);
		assertEquals(1, emptyCounter.get());
		testModel.indexes().add(1);
		assertEquals(1, emptyCounter.get());
		testModel.indexes().set(asList(1, 2));
		assertEquals(1, emptyCounter.get());
		testModel.addSelectionInterval(0, 1);
		assertEquals(1, emptyCounter.get());
		testModel.indexes().increment();
		assertEquals(1, emptyCounter.get());
		testModel.clearSelection();
		assertEquals(2, emptyCounter.get());
	}
}
