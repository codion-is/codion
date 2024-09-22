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
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.component.combobox;

import is.codion.common.value.Value;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel.ItemFinder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultFilterComboBoxModelTest {

	private FilterComboBoxModel<String> testModel;

	private static final String NULL = "nullitem";
	private static final String ANNA = "anna";
	private static final String KALLI = "kalli";
	private static final String SIGGI = "siggi";
	private static final String TOMAS = "tomas";
	private static final String BJORN = "björn";

	private final ListDataListener listDataListener = new ListDataListener() {
		@Override
		public void intervalAdded(ListDataEvent e) {}

		@Override
		public void intervalRemoved(ListDataEvent e) {}

		@Override
		public void contentsChanged(ListDataEvent e) {}
	};

	@Test
	void testRefreshClear() {
		testModel.refresh();
		assertEquals(5, testModel.items().visible().count());
		testModel.clear();
		assertEquals(1, testModel.getSize());//null item
		assertTrue(testModel.cleared());
	}

	@Test
	void testDataListeners() {
		testModel.addListDataListener(listDataListener);
		testModel.removeListDataListener(listDataListener);
	}

	@Test
	void testSorting() {
		assertEquals(ANNA, testModel.getElementAt(1));
		assertEquals(BJORN, testModel.getElementAt(2));
		assertEquals(KALLI, testModel.getElementAt(3));
		assertEquals(SIGGI, testModel.getElementAt(4));
		assertEquals(TOMAS, testModel.getElementAt(5));

		Comparator<String> comparator = testModel.comparator().get();
		testModel.comparator().clear();
		assertNull(testModel.comparator().get());
		List<String> names = new ArrayList<>();
		names.add(ANNA);
		names.add(KALLI);
		names.add(SIGGI);
		names.add(TOMAS);
		names.add(BJORN);
		testModel.items().set(names);

		assertEquals(ANNA, testModel.getElementAt(1));
		assertEquals(KALLI, testModel.getElementAt(2));
		assertEquals(SIGGI, testModel.getElementAt(3));
		assertEquals(TOMAS, testModel.getElementAt(4));
		assertEquals(BJORN, testModel.getElementAt(5));

		testModel.comparator().set(comparator);
		names.remove(SIGGI);
		testModel.items().set(names);
		testModel.addItem(SIGGI);

		assertEquals(ANNA, testModel.getElementAt(1));
		assertEquals(BJORN, testModel.getElementAt(2));
		assertEquals(KALLI, testModel.getElementAt(3));
		assertEquals(SIGGI, testModel.getElementAt(4));
		assertEquals(TOMAS, testModel.getElementAt(5));

		testModel.comparator().set((o1, o2) -> {
			if (o1 == null) {
				return -1;
			}
			if (o2 == null) {
				return 1;
			}
			return o2.compareTo(o1);
		});
		assertNotNull(testModel.comparator().get());

		assertEquals(TOMAS, testModel.getElementAt(1));
		assertEquals(SIGGI, testModel.getElementAt(2));
		assertEquals(KALLI, testModel.getElementAt(3));
		assertEquals(BJORN, testModel.getElementAt(4));
		assertEquals(ANNA, testModel.getElementAt(5));
	}

	@Test
	void testSelection() {
		AtomicInteger selectionChangedCounter = new AtomicInteger();
		Consumer<String> selectionConsumer = selectedItem -> selectionChangedCounter.incrementAndGet();
		testModel.selection().item().addConsumer(selectionConsumer);
		testModel.setSelectedItem(BJORN);
		assertEquals(1, selectionChangedCounter.get());
		testModel.setSelectedItem(null);
		assertEquals(2, selectionChangedCounter.get());
		testModel.setSelectedItem(NULL);
		assertEquals(2, selectionChangedCounter.get());
		testModel.setSelectedItem(BJORN);
		assertEquals(3, selectionChangedCounter.get());
		assertEquals(BJORN, testModel.getSelectedItem());
		assertEquals(BJORN, testModel.selection().value());
		assertFalse(testModel.selection().empty().get());
		assertFalse(testModel.selection().nullSelected());
		testModel.setSelectedItem(null);
		assertTrue(testModel.selection().empty().get());
		assertEquals(4, selectionChangedCounter.get());
		assertEquals(NULL, testModel.getSelectedItem());
		assertTrue(testModel.selection().nullSelected());
		assertTrue(testModel.selection().empty().get());
		assertNull(testModel.selection().value());
		testModel.setSelectedItem(SIGGI);
		testModel.clear();
		assertEquals(6, selectionChangedCounter.get());
		testModel.selection().item().removeConsumer(selectionConsumer);
	}

	@Test
	void filterWithSelection() {
		testModel.filterSelectedItem().set(true);
		testModel.setSelectedItem(BJORN);
		testModel.items().visiblePredicate().set(item -> !item.equals(BJORN));
		assertEquals(NULL, testModel.getSelectedItem());
		assertNull(testModel.selection().value());

		testModel.items().visiblePredicate().clear();
		testModel.filterSelectedItem().set(false);
		assertFalse(testModel.filterSelectedItem().get());
		testModel.setSelectedItem(BJORN);
		testModel.items().visiblePredicate().set(item -> !item.equals(BJORN));
		assertNotNull(testModel.getSelectedItem());
		assertEquals(BJORN, testModel.selection().value());
	}

	@Test
	void visiblePredicate() {
		testModel.addListDataListener(listDataListener);

		testModel.items().visiblePredicate().set(item -> false);
		assertEquals(1, testModel.getSize());
		testModel.items().visiblePredicate().set(item -> true);
		assertEquals(6, testModel.getSize());
		testModel.items().visiblePredicate().set(item -> !item.equals(ANNA));
		assertEquals(5, testModel.getSize());
		assertFalse(testModel.items().visible().contains(ANNA));
		assertTrue(testModel.items().filtered().contains(ANNA));
		testModel.items().visiblePredicate().set(item -> item.equals(ANNA));
		assertEquals(2, testModel.getSize());
		assertTrue(testModel.items().visible().contains(ANNA));

		assertEquals(1, testModel.items().visible().count());
		assertEquals(4, testModel.items().filtered().count());
		assertEquals(1, testModel.items().visible().count());
		assertEquals(5, testModel.items().get().size());

		testModel.addItem(BJORN);//already contained
		assertEquals(4, testModel.items().filtered().count());

		assertFalse(testModel.items().visible().contains(BJORN));
		assertTrue(testModel.items().contains(BJORN));

		testModel.removeListDataListener(listDataListener);
	}

	@Test
	void remove() {
		//remove filtered item
		testModel.items().visiblePredicate().set(item -> !item.equals(BJORN));
		testModel.removeItem(BJORN);
		testModel.items().visiblePredicate().clear();
		assertFalse(testModel.items().visible().contains(BJORN));

		//remove visible item
		testModel.removeItem(KALLI);
		assertFalse(testModel.items().visible().contains(KALLI));
	}

	@Test
	void add() {
		testModel.clear();
		//add filtered item
		testModel.items().visiblePredicate().set(item -> !item.equals(BJORN));
		testModel.addItem(BJORN);
		assertFalse(testModel.items().visible().contains(BJORN));

		//add visible item
		testModel.addItem(KALLI);
		assertTrue(testModel.items().visible().contains(KALLI));

		testModel.items().visiblePredicate().clear();
		assertTrue(testModel.items().visible().contains(BJORN));
	}

	@Test
	void setNullValueString() {
		assertTrue(testModel.items().visible().contains(null));
		testModel.refresh();
		assertEquals(5, testModel.items().visible().count());
		assertEquals(testModel.getElementAt(0), NULL);
		testModel.setSelectedItem(null);
		assertEquals(testModel.getSelectedItem(), NULL);
		assertTrue(testModel.selection().nullSelected());
		assertNull(testModel.selection().value());
		testModel.setSelectedItem(NULL);
		assertEquals(NULL, testModel.getElementAt(0));
		assertEquals(ANNA, testModel.getElementAt(1));
	}

	@Test
	void nullItem() {
		FilterComboBoxModel<String> model = new DefaultFilterComboBoxModel<>();
		assertFalse(model.items().contains(null));
		model.includeNull().set(true);
		assertTrue(model.items().contains(null));
		model.includeNull().set(false);
		assertFalse(model.items().contains(null));
		model.includeNull().set(true);
		model.nullItem().set("-");
		assertTrue(model.items().contains(null));
		assertEquals("-", model.getSelectedItem());
		model.setSelectedItem("-");
		assertTrue(model.selection().nullSelected());
	}

	@Test
	void selectorValue() {
		Value<Character> selectorValue = testModel.createSelectorValue(new ItemFinder<String, Character>() {
			@Override
			public Character value(String item) {
				return item.charAt(0);
			}

			@Override
			public Predicate<String> predicate(Character value) {
				return item -> item.charAt(0) == value.charValue();
			}
		});
		assertNull(selectorValue.get());
		testModel.setSelectedItem(ANNA);
		assertEquals('a', selectorValue.get());
		selectorValue.set('k');
		assertEquals(KALLI, testModel.getSelectedItem());
		selectorValue.clear();
		assertTrue(testModel.selection().nullSelected());
		testModel.setSelectedItem(BJORN);
		assertEquals('b', selectorValue.get());
		testModel.setSelectedItem(null);
		assertNull(selectorValue.get());
	}

	@Test
	void setContentsSelectedItem() {
		class Data {
			final int id;
			final String data;

			Data(int id, String data) {
				this.id = id;
				this.data = data;
			}

			@Override
			public boolean equals(Object o) {
				if (this == o) {
					return true;
				}
				if (o == null || getClass() != o.getClass()) {
					return false;
				}

				return id == ((Data) o).id;
			}
		}
		List<Data> items = asList(new Data(1, "1"), new Data(2, "2"), new Data(3, "3"));

		FilterComboBoxModel<Data> model = new DefaultFilterComboBoxModel<>();
		model.items().set(items);
		model.setSelectedItem(items.get(1));
		assertEquals("2", model.selection().value().data);

		items = asList(new Data(1, "1"), new Data(2, "22"), new Data(3, "3"));

		model.items().set(items);
		assertEquals("22", model.selection().value().data);
	}

	@Test
	void includeNull() {
		FilterComboBoxModel<Integer> model = new DefaultFilterComboBoxModel<>();
		model.items().set(asList(1, 2, 3, 4, 5));
		model.includeNull().set(true);
		model.includeNull().set(true);
		assertTrue(model.includeNull().get());
		model.refresh();
	}

	@Test
	void validator() {
		FilterComboBoxModel<Integer> model = new DefaultFilterComboBoxModel<>();
		model.validator().set(item -> item > 0);
		assertThrows(IllegalArgumentException.class, () -> model.items().set(asList(1, 2, 3, 4, 5, 0)));
		assertThrows(NullPointerException.class, () -> model.items().clear());
	}

	@Test
	void items() {
		List<Integer> values = asList(0, 1, 2);
		FilterComboBoxModel<Integer> model = new DefaultFilterComboBoxModel<>();
		model.refresher().items().set(() -> values);
		model.refresher().refresh();
		assertEquals(values.size(), model.items().get().size());
		assertTrue(values.containsAll(model.items().get()));
	}

	@Test
	void validSelectionPredicate() {
		FilterComboBoxModel<Integer> model = new DefaultFilterComboBoxModel<>();
		model.items().set(asList(0, 1, 2));
		model.setSelectedItem(0);
		assertThrows(IllegalArgumentException.class, () -> model.validSelectionPredicate().set(item -> item > 0));
		model.setSelectedItem(1);
		model.validSelectionPredicate().set(item -> item > 0);
		model.setSelectedItem(0);
		assertEquals(1, model.getSelectedItem());
	}

	@BeforeEach
	void setUp() {
		testModel = new DefaultFilterComboBoxModel<>();
		testModel.includeNull().set(true);
		testModel.nullItem().set(NULL);
		List<String> names = new ArrayList<>();
		names.add(ANNA);
		names.add(KALLI);
		names.add(SIGGI);
		names.add(TOMAS);
		names.add(BJORN);
		testModel.items().set(names);
	}

	@AfterEach
	void tearDown() {
		testModel = null;
	}
}
