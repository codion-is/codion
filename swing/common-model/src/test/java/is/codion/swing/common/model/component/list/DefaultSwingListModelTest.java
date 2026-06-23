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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.component.list;

import is.codion.common.utilities.Text;

import org.junit.jupiter.api.Test;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the Swing {@code ListModel} coat of {@link SwingListModel} — the UI-agnostic logic is tested in
 * {@code is.codion.common.model.component.list.DefaultFilterListModelTest}.
 */
final class DefaultSwingListModelTest {

	private static final String ONE = "One";
	private static final String TWO = "Two";
	private static final String THREE = "Three";
	private static final String FOUR = "Four";

	@Test
	void listModelCoat() {
		List<String> items = asList(ONE, TWO, THREE);
		SwingListModel<String> model = SwingListModel.builder()
						.items(items)
						.comparator(Text.collator())
						.build();
		// getSize/getElementAt expose the sorted included items, the ListModel surface a JList needs
		assertEquals(items.size(), model.getSize());
		assertEquals(model.items().included().size(), model.getSize());
		for (int i = 0; i < model.getSize(); i++) {
			assertEquals(model.items().included().get(i), model.getElementAt(i));
		}

		// the selection (a javax.swing.ListSelectionModel) and getElementAt read through the sort
		model.selection().item().set(TWO);
		model.sort().descending();
		assertEquals(THREE, model.getElementAt(1));// getElementAt reflects the sorted order
		assertEquals(TWO, model.selection().item().get());// selection preserved through sort
	}

	@Test
	void async() {
		// the Swing builder's async option is forwarded to the ProgressWorker based refresher
		SwingListModel<String> asyncModel = SwingListModel.builder()
						.<String>items()
						.async(true)
						.build();
		assertTrue(asyncModel.items().refresher().async().is());

		SwingListModel<String> syncModel = SwingListModel.builder()
						.<String>items()
						.async(false)
						.build();
		assertFalse(syncModel.items().refresher().async().is());
	}

	@Test
	void listDataEvents() {
		List<String> items = new ArrayList<>(asList(ONE, TWO, THREE));
		SwingListModel<String> model = SwingListModel.builder()
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
		assertEquals(2, listener.intervalRemovedCount);
		assertEquals(2, listener.intervalAddedCount);

		// Clear all
		model.items().clear();
		assertTrue(listener.intervalRemovedCount > 2); // Should fire remove events

		// Add new items after clear
		model.items().add(asList("New1", "New2"));
		assertEquals(2, model.getSize()); // 2 new items
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
