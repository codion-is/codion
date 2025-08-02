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

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
		FilterListModel<String> unsorted = FilterListModel.builder()
						.items(() -> items)
						.async(false)
						.comparator(null)
						.build();
		assertFalse(unsorted.sort().sorted());
		unsorted.sort().descending();
		assertFalse(unsorted.sort().sorted());
		unsorted.sort().clear();
		assertFalse(unsorted.sort().sorted());

		FilterListModel<String> model = FilterListModel.builder()
						.items(items)
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

	@Test
	void filterPredicate() {
		List<String> items = asList("apple", "banana", "cherry", "date", "elderberry");
		FilterListModel<String> model = FilterListModel.builder()
						.items(items).build();

		// Initially all visible
		assertEquals(5, model.items().visible().count());
		assertEquals(0, model.items().filtered().count());

		// Filter items containing 'e' - apple, cherry, date, elderberry
		model.items().visible().predicate().set(s -> s.contains("e"));
		assertEquals(4, model.items().visible().count());
		assertEquals(1, model.items().filtered().count()); // only banana
		assertTrue(model.items().visible().contains("apple"));
		assertTrue(model.items().visible().contains("cherry"));
		assertTrue(model.items().visible().contains("date"));
		assertTrue(model.items().visible().contains("elderberry"));

		// Chain filters - only items with 'e' and length > 5
		model.items().visible().predicate().set(s -> s.contains("e") && s.length() > 5);
		assertEquals(2, model.items().visible().count());
		assertTrue(model.items().visible().contains("cherry"));
		assertTrue(model.items().visible().contains("elderberry"));

		// Clear filter
		model.items().visible().predicate().clear();
		assertEquals(5, model.items().visible().count());
		assertEquals(0, model.items().filtered().count());
	}

	@Test
	void listDataEvents() {
		List<String> items = new ArrayList<>(asList(ONE, TWO, THREE));
		FilterListModel<String> model = FilterListModel.builder()
						.items(items).build();

		TestListDataListener listener = new TestListDataListener();
		model.addListDataListener(listener);

		// Add item
		model.items().add(FOUR);
		assertEquals(1, listener.intervalAddedCount);
		assertEquals(3, listener.lastAddedIndex);

		// Remove item
		model.items().remove(TWO);
		assertEquals(1, listener.intervalRemovedCount);

		// Update/replace item
		model.items().replace(ONE, "ONE_UPDATED");
		assertEquals(1, listener.contentsChangedCount);

		// Clear all
		model.items().clear();
		assertTrue(listener.intervalRemovedCount > 1); // Should fire remove events

		// Add new items after clear
		model.items().add(asList("New1", "New2"));
		assertEquals(2, model.getSize()); // 2 new items
	}

	@Test
	void concurrentModification() throws Exception {
		List<String> items = new ArrayList<>(asList(ONE, TWO, THREE, FOUR));
		FilterListModel<String> model = FilterListModel.<String>builder()
						.items(() -> new ArrayList<>(items))
						.async(false)
						.build();

		CountDownLatch latch = new CountDownLatch(2);
		AtomicBoolean error = new AtomicBoolean(false);

		// Thread 1: Continuously filter
		Thread filterThread = new Thread(() -> {
			try {
				for (int i = 0; i < 100; i++) {
					if (i % 2 == 0) {
						model.items().visible().predicate().set(s -> s.length() > 3);
					}
					else {
						model.items().visible().predicate().clear();
					}
					Thread.sleep(1);
				}
			}
			catch (Exception e) {
				error.set(true);
			}
			finally {
				latch.countDown();
			}
		});

		// Thread 2: Continuously modify selection
		Thread selectionThread = new Thread(() -> {
			try {
				for (int i = 0; i < 100; i++) {
					int size = model.getSize();
					if (size > 0) {
						int index = i % size;
						model.selection().index().set(index);
					}
					Thread.sleep(1);
				}
			}
			catch (Exception e) {
				error.set(true);
			}
			finally {
				latch.countDown();
			}
		});

		filterThread.start();
		selectionThread.start();

		assertTrue(latch.await(5, TimeUnit.SECONDS));
		assertFalse(error.get());
	}

	@Test
	void emptyModel() {
		FilterListModel<String> model = FilterListModel.builder()
						.<String>items()
						.build();

		assertEquals(0, model.getSize());
		assertEquals(0, model.items().count());
		assertEquals(0, model.items().visible().count());
		assertEquals(0, model.items().filtered().count());

		// Operations on empty model should not throw
		model.items().visible().predicate().set(s -> true);
		model.items().refresh();
		model.selection().clear();

		// Add items to empty model
		model.items().add(asList(ONE, TWO));
		assertEquals(2, model.getSize());
	}

	@Test
	void selectionPreservation() {
		List<String> items = asList(ONE, TWO, THREE, FOUR);
		FilterListModel<String> model = FilterListModel.builder()
						.items(items).build();

		// Select multiple items
		model.selection().indexes().set(asList(0, 2));
		assertTrue(model.selection().items().contains(ONE));
		assertTrue(model.selection().items().contains(THREE));

		// Filter - selection should be preserved for visible items
		model.items().visible().predicate().set(s -> !s.equals(TWO));
		assertTrue(model.selection().items().contains(ONE));
		assertTrue(model.selection().items().contains(THREE));

		// Clear filter - selection still preserved
		model.items().visible().predicate().clear();
		assertTrue(model.selection().items().contains(ONE));
		assertTrue(model.selection().items().contains(THREE));
	}

	@Test
	void builderConfiguration() {
		// Test various builder configurations
		FilterListModel<String> asyncModel = FilterListModel.builder()
						.<String>items()
						.async(true)
						.build();
		assertTrue(asyncModel.items().refresher().async().is());

		FilterListModel<String> syncModel = FilterListModel.builder()
						.<String>items()
						.async(false)
						.build();
		assertFalse(syncModel.items().refresher().async().is());

		// With predicate
		FilterListModel<String> filteredModel = FilterListModel.builder()
						.items(asList(ONE, TWO, THREE, FOUR))
						.visible(s -> s.length() > 3)
						.build();
		assertEquals(2, filteredModel.items().visible().count()); // THREE and FOUR
		assertEquals(2, filteredModel.items().filtered().count()); // ONE and TWO
	}

	@Test
	void sortingWithSelection() {
		List<String> items = asList("Charlie", "Alice", "Bob", "David");
		FilterListModel<String> model = FilterListModel.builder()
						.items(items)
						.comparator(String::compareTo)
						.build();

		// Select Bob
		model.selection().item().set("Bob");
		assertEquals("Bob", model.selection().item().get());

		// Sort ascending - Bob should still be selected
		model.sort().ascending();
		assertEquals("Bob", model.selection().item().get());
		assertEquals(1, model.items().visible().indexOf("Bob"));

		// Sort descending - Bob should still be selected
		model.sort().descending();
		assertEquals("Bob", model.selection().item().get());
		assertEquals(2, model.items().visible().indexOf("Bob"));

		// Clear sort - selection preserved
		model.sort().clear();
		assertEquals("Bob", model.selection().item().get());
	}

	@Test
	void removeSelectedItem() {
		List<String> items = new ArrayList<>(asList(ONE, TWO, THREE));
		FilterListModel<String> model = FilterListModel.builder()
						.items(items).build();

		// Select and remove
		model.selection().item().set(TWO);
		assertEquals(TWO, model.selection().item().get());

		model.items().remove(TWO);
		// Selection might move to another item or be cleared
		assertEquals(2, model.getSize());
		// If selection moved, it should be to a remaining item
		if (!model.selection().empty().is()) {
			assertTrue(model.items().visible().contains(model.selection().item().get()));
		}
	}


	private static class TestListDataListener implements ListDataListener {
		int intervalAddedCount = 0;
		int intervalRemovedCount = 0;
		int contentsChangedCount = 0;
		int lastAddedIndex = -1;

		@Override
		public void intervalAdded(ListDataEvent e) {
			intervalAddedCount++;
			lastAddedIndex = e.getIndex0();
		}

		@Override
		public void intervalRemoved(ListDataEvent e) {
			intervalRemovedCount++;
		}

		@Override
		public void contentsChanged(ListDataEvent e) {
			contentsChangedCount++;
		}
	}
}
