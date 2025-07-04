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
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static javax.swing.ListSelectionModel.*;
import static org.junit.jupiter.api.Assertions.*;

public class FilterListSelectionTest {

	private final FilterListSelection<String> testModel;

	public FilterListSelectionTest() {
		List<String> data = asList("A", "B", "C");
		FilterTableModel<String, Integer> tableModel =
						FilterTableModel.builder()
										.columns(new FilterTableModel.TableColumns<String, Integer>() {
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

		testModel = filterListSelection(tableModel.items().visible());
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

	@Test
	void multipleSelectionOperations() {
		// Test multiple selection mode operations
		testModel.setSelectionMode(MULTIPLE_INTERVAL_SELECTION);

		// Test selectAll
		testModel.selectAll();
		assertEquals(3, testModel.count());
		assertEquals(asList(0, 1, 2), testModel.indexes().get());

		// Test contains operations
		assertTrue(testModel.indexes().contains(1));
		assertFalse(testModel.indexes().contains(3));
		assertTrue(testModel.items().contains("B"));
		assertFalse(testModel.items().contains("D"));

		// Test remove operations
		testModel.indexes().remove(1);
		assertEquals(2, testModel.count());
		assertEquals(asList(0, 2), testModel.indexes().get());

		testModel.indexes().remove(asList(0, 2));
		assertEquals(0, testModel.count());
		assertTrue(testModel.empty().get());
	}

	@Test
	void itemSelectionOperations() {
		// Test selecting by item
		testModel.item().set("B");
		assertEquals(1, testModel.index().get());
		assertEquals("B", testModel.item().get());

		// Test selecting multiple items
		testModel.items().set(asList("A", "C"));
		assertEquals(asList(0, 2), testModel.indexes().get());
		assertEquals(asList("A", "C"), testModel.items().get());

		// Test adding items
		testModel.items().add("B");
		assertEquals(asList(0, 1, 2), testModel.indexes().get());

		// Test removing items
		testModel.items().remove("B");
		assertEquals(asList(0, 2), testModel.indexes().get());

		testModel.items().remove(asList("A", "C"));
		assertTrue(testModel.empty().get());
	}

	@Test
	void predicateSelection() {
		// Test selecting by predicate
		testModel.items().set(item -> item.compareTo("B") >= 0);
		assertEquals(asList(1, 2), testModel.indexes().get());
		assertEquals(asList("B", "C"), testModel.items().get());

		// Test adding by predicate
		testModel.clearSelection();
		testModel.items().add(item -> item.equals("A") || item.equals("C"));
		assertEquals(asList(0, 2), testModel.indexes().get());
	}

	@Test
	void incrementDecrement() {
		// Test increment on empty selection
		testModel.clearSelection();
		testModel.indexes().increment();
		assertEquals(0, testModel.index().get());

		// Test increment wrap around
		testModel.index().set(2);
		testModel.indexes().increment();
		assertEquals(0, testModel.index().get());

		// Test decrement on empty selection
		testModel.clearSelection();
		testModel.indexes().decrement();
		assertEquals(2, testModel.index().get());

		// Test decrement wrap around
		testModel.index().set(0);
		testModel.indexes().decrement();
		assertEquals(2, testModel.index().get());

		// Test with multiple selections
		testModel.indexes().set(asList(0, 1));
		testModel.indexes().increment();
		assertEquals(asList(1, 2), testModel.indexes().get());

		testModel.indexes().set(asList(1, 2));
		testModel.indexes().decrement();
		assertEquals(asList(0, 1), testModel.indexes().get());
	}

	@Test
	void selectionBounds() {
		// Test out of bounds index
		assertThrows(IndexOutOfBoundsException.class, () -> testModel.index().set(3));
		assertThrows(IndexOutOfBoundsException.class, () -> testModel.index().set(-1));
		assertThrows(IndexOutOfBoundsException.class, () -> testModel.indexes().add(3));
		assertThrows(IndexOutOfBoundsException.class, () -> testModel.indexes().set(asList(0, 4)));
	}

	@Test
	void selectionModeChanges() {
		// Set multiple selections
		testModel.setSelectionMode(MULTIPLE_INTERVAL_SELECTION);
		testModel.indexes().set(asList(0, 1, 2));
		assertEquals(3, testModel.count());

		// Change to single selection - should clear selection
		testModel.setSelectionMode(SINGLE_SELECTION);
		assertTrue(testModel.empty().get());

		// In single selection mode, adding multiple should only keep last
		testModel.index().set(0);
		testModel.indexes().add(1);
		assertEquals(1, testModel.index().get());
		assertEquals(1, testModel.count());
	}

	@Test
	void valueIsAdjusting() {
		// Test adjusting flag
		assertFalse(testModel.getValueIsAdjusting());

		testModel.adjusting(true);
		assertTrue(testModel.getValueIsAdjusting());

		// Test that selection interval changes with adjusting true still update values
		// but should not fire final value changed events
		AtomicInteger selectionEventCounter = new AtomicInteger();
		testModel.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				selectionEventCounter.incrementAndGet();
			}
		});

		// Change selection while adjusting
		testModel.adjusting(true);
		testModel.setSelectionInterval(0, 0);
		assertEquals(0, selectionEventCounter.get()); // No final event while adjusting

		// Setting adjusting to false fires a value changed event with the current selection
		testModel.adjusting(false);
		assertEquals(1, selectionEventCounter.get()); // Final event fired when adjusting set to false

		// Another selection change while not adjusting
		testModel.setSelectionInterval(1, 1);
		assertEquals(2, selectionEventCounter.get()); // Another final event
	}

	@Test
	void nullHandling() {
		// Test null item selection
		testModel.item().set(null);
		assertTrue(testModel.empty().get());

		// Test null in collections
		assertThrows(NullPointerException.class, () -> testModel.items().add((String) null));
		assertThrows(NullPointerException.class, () -> testModel.items().set(asList("A", null)));
		assertThrows(NullPointerException.class, () -> testModel.items().remove((String) null));
	}

	@Test
	void emptyCollectionOperations() {
		// Test operations with empty collections
		testModel.indexes().set(asList(0, 1));
		testModel.indexes().add(emptyList());
		assertEquals(asList(0, 1), testModel.indexes().get());

		testModel.indexes().remove(emptyList());
		assertEquals(asList(0, 1), testModel.indexes().get());

		testModel.items().add(emptyList());
		assertEquals(asList("A", "B"), testModel.items().get());
	}

	@Test
	void stateObservables() {
		// Test empty state
		assertTrue(testModel.empty().get());
		testModel.index().set(0);
		assertFalse(testModel.empty().get());

		// Test single state
		assertTrue(testModel.single().get());
		testModel.indexes().add(1);
		assertFalse(testModel.single().get());

		// Test multiple state
		assertTrue(testModel.multiple().get());
		testModel.clearSelection();
		assertFalse(testModel.multiple().get());
		testModel.index().set(0);
		assertFalse(testModel.multiple().get());
	}

	@Test
	void changingEvent() {
		// Test changing event notifications
		AtomicInteger changingCounter = new AtomicInteger();
		testModel.changing().addListener(changingCounter::incrementAndGet);

		testModel.addSelectionInterval(0, 1);
		assertEquals(1, changingCounter.get());

		testModel.setSelectionInterval(1, 2);
		assertEquals(2, changingCounter.get());

		testModel.removeSelectionInterval(0, 2);
		assertEquals(3, changingCounter.get());

		testModel.insertIndexInterval(0, 1, true);
		assertEquals(4, changingCounter.get());

		testModel.removeIndexInterval(0, 1);
		assertEquals(5, changingCounter.get());
	}
}
