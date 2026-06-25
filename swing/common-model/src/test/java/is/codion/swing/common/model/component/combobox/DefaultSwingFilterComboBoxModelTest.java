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
 * Copyright (c) 2008 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.component.combobox;

import is.codion.common.utilities.item.Item;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static is.codion.common.utilities.item.Item.item;
import static is.codion.swing.common.model.component.combobox.SwingFilterComboBoxModel.booleanItems;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the Swing {@code ComboBoxModel}/{@code ListModel} coat over the common {@code FilterComboBoxModel};
 * the rich model logic is tested in
 * {@code is.codion.common.model.component.combobox.DefaultFilterComboBoxModelTest}.
 */
public class DefaultSwingFilterComboBoxModelTest {

	private static final String NULL = "nullitem";
	private static final String ANNA = "anna";
	private static final String KALLI = "kalli";
	private static final String SIGGI = "siggi";
	private static final String TOMAS = "tomas";
	private static final String BJORN = "björn";

	private static final List<String> ITEMS = asList(ANNA, KALLI, SIGGI, TOMAS, BJORN);

	private SwingFilterComboBoxModel<String> testModel;

	@Test
	void getSize() {
		assertEquals(ITEMS.size() + 1, testModel.getSize());//+ null item
		testModel.items().clear();
		assertEquals(1, testModel.getSize());//just the null item
	}

	@Test
	void getElementAt() {
		//null item first, then the items (sorted by default)
		assertEquals(NULL, testModel.getElementAt(0));
		assertEquals(ANNA, testModel.getElementAt(1));
	}

	@Test
	void listDataEvents() {
		AtomicInteger contentsChanged = new AtomicInteger();
		ListDataListener listener = new ListDataListener() {
			@Override
			public void intervalAdded(ListDataEvent e) {}

			@Override
			public void intervalRemoved(ListDataEvent e) {}

			@Override
			public void contentsChanged(ListDataEvent e) {
				contentsChanged.incrementAndGet();
			}
		};
		testModel.addListDataListener(listener);
		int beforeSelection = contentsChanged.get();
		testModel.selection().item().set(BJORN);//selection change fires
		assertTrue(contentsChanged.get() > beforeSelection);
		int beforeItems = contentsChanged.get();
		testModel.items().clear();//items change fires
		assertTrue(contentsChanged.get() > beforeItems);
		testModel.removeListDataListener(listener);
		int afterRemove = contentsChanged.get();
		testModel.items().set(ITEMS);//no longer listening
		assertEquals(afterRemove, contentsChanged.get());
	}

	@Test
	void selectedItem() {
		//the Object-based ComboBoxModel setter, incl. null-item handling
		testModel.setSelectedItem(BJORN);
		assertEquals(BJORN, testModel.selectedItem());
		assertEquals(BJORN, testModel.selection().item().get());
		testModel.setSelectedItem(NULL);//the null item clears the selection
		assertNull(testModel.selection().item().get());
		assertEquals(NULL, testModel.selectedItem());
		testModel.setSelectedItem(null);
		assertNull(testModel.selection().item().get());
	}

	@Test
	void itemComboBoxSelectByValue() {
		//setSelectedItem(Object) translates a raw value to its Item
		Item<Integer> nullItem = item(null, "");
		Item<Integer> aOne = item(1, "AOne");
		Item<Integer> bTwo = item(2, "BTwo");
		List<Item<Integer>> items = asList(nullItem, bTwo, aOne);
		SwingFilterComboBoxModel<Item<Integer>> model = SwingFilterComboBoxModel.builder()
						.items(items)
						.build();
		model.setSelectedItem(1);//raw value -> aOne
		assertSame(aOne, model.selectedItem());
		assertEquals(1, model.selection().item().getOrThrow().get());
		model.setSelectedItem(2);
		assertSame(bTwo, model.selectedItem());
		model.setSelectedItem(null);
		assertNull(model.selection().item().get());
		assertSame(nullItem, model.selectedItem());
	}

	@Test
	void booleanItemComboBoxModel() {
		List<Item<Boolean>> items = booleanItems();
		SwingFilterComboBoxModel<Item<Boolean>> model = SwingFilterComboBoxModel.builder()
						.items(items)
						.build();
		assertSame(items.get(0), model.selectedItem());
		assertNull(model.selection().item().get());
		model.setSelectedItem(false);
		assertEquals(false, model.selection().item().getOrThrow().get());
		model.setSelectedItem(true);
		assertEquals(true, model.selection().item().getOrThrow().get());
		model.setSelectedItem(null);
		assertNull(model.selection().item().get());
	}

	@BeforeEach
	void setUp() {
		testModel = SwingFilterComboBoxModel.builder()
						.items(ITEMS)
						.nullItem(NULL)
						.build();
	}
}
