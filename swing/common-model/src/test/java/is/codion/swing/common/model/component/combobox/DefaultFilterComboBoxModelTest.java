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
 * Copyright (c) 2008 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.component.combobox;

import is.codion.common.item.Item;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel.ItemFinder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static is.codion.common.item.Item.item;
import static is.codion.swing.common.model.component.combobox.FilterComboBoxModel.booleanItems;
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

	private static final List<String> ITEMS = asList(ANNA, KALLI, SIGGI, TOMAS, BJORN);

	private final ListDataListener listDataListener = new ListDataListener() {
		@Override
		public void intervalAdded(ListDataEvent e) {}

		@Override
		public void intervalRemoved(ListDataEvent e) {}

		@Override
		public void contentsChanged(ListDataEvent e) {}
	};

	@Test
	void sorting() {
		String first = "ari";
		String second = "ári";
		String third = "eg";
		String fourth = "ég";
		String fifth = "inn";
		String sixth = "ínn";
		String seventh = "oli";
		String eigtht = "óli";

		FilterComboBoxModel<String> comboBoxModel = FilterComboBoxModel.builder(asList(eigtht, fourth, seventh, second, first, fifth, sixth, third))
						.build();

		assertEquals(0, comboBoxModel.items().visible().indexOf(first));
		assertEquals(1, comboBoxModel.items().visible().indexOf(second));
		assertEquals(2, comboBoxModel.items().visible().indexOf(third));
		assertEquals(3, comboBoxModel.items().visible().indexOf(fourth));
		assertEquals(4, comboBoxModel.items().visible().indexOf(fifth));
		assertEquals(5, comboBoxModel.items().visible().indexOf(sixth));
		assertEquals(6, comboBoxModel.items().visible().indexOf(seventh));
		assertEquals(7, comboBoxModel.items().visible().indexOf(eigtht));
	}

	@Test
	void testRefreshClear() {
		testModel.items().refresh();
		assertEquals(5, testModel.items().visible().count());
		testModel.items().clear();
		assertEquals(1, testModel.getSize());//null item
		assertTrue(testModel.items().cleared());
	}

	@Test
	void testDataListeners() {
		testModel.addListDataListener(listDataListener);
		testModel.removeListDataListener(listDataListener);
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
		assertEquals(BJORN, testModel.selection().item().get());
		assertFalse(testModel.selection().empty().get());
		testModel.setSelectedItem(null);
		assertTrue(testModel.selection().empty().get());
		assertEquals(4, selectionChangedCounter.get());
		assertEquals(NULL, testModel.getSelectedItem());
		assertTrue(testModel.selection().empty().get());
		assertNull(testModel.selection().item().get());
		testModel.setSelectedItem(SIGGI);
		testModel.items().clear();
		assertEquals(6, selectionChangedCounter.get());
		testModel.selection().item().removeConsumer(selectionConsumer);
	}

	@Test
	void filterWithSelection() {
		Predicate<String> hideBjorn = item -> !item.equals(BJORN);

		FilterComboBoxModel<String> model = FilterComboBoxModel.builder(ITEMS)
						.nullItem(NULL)
						.filterSelected(true)
						.build();
		model.setSelectedItem(BJORN);
		model.items().visible().predicate().set(hideBjorn);
		assertEquals(NULL, model.getSelectedItem());
		assertNull(model.selection().item().get());

		model = FilterComboBoxModel.builder(ITEMS)
						.nullItem(NULL)
						.filterSelected(false)
						.build();
		model.setSelectedItem(BJORN);
		model.items().visible().predicate().set(hideBjorn);
		assertNotNull(model.getSelectedItem());
		assertEquals(BJORN, model.selection().item().get());
	}

	@Test
	void visiblePredicate() {
		testModel.addListDataListener(listDataListener);

		testModel.items().visible().predicate().set(item -> false);
		assertEquals(1, testModel.getSize());
		testModel.items().visible().predicate().set(item -> true);
		assertEquals(6, testModel.getSize());
		testModel.items().visible().predicate().set(item -> !item.equals(ANNA));
		assertEquals(5, testModel.getSize());
		assertFalse(testModel.items().visible().contains(ANNA));
		assertTrue(testModel.items().filtered().contains(ANNA));
		testModel.items().visible().predicate().set(item -> item.equals(ANNA));
		assertEquals(2, testModel.getSize());
		assertTrue(testModel.items().visible().contains(ANNA));

		assertEquals(1, testModel.items().visible().count());
		assertEquals(4, testModel.items().filtered().count());
		assertEquals(1, testModel.items().visible().count());
		assertEquals(5, testModel.items().get().size());

		testModel.items().add(BJORN);//already contained
		assertEquals(4, testModel.items().filtered().count());

		assertFalse(testModel.items().visible().contains(BJORN));
		assertTrue(testModel.items().contains(BJORN));

		testModel.removeListDataListener(listDataListener);
	}

	@Test
	void remove() {
		//remove filtered item
		testModel.items().visible().predicate().set(item -> !item.equals(BJORN));
		testModel.items().remove(BJORN);
		testModel.items().visible().predicate().clear();
		assertFalse(testModel.items().visible().contains(BJORN));

		testModel.selection().item().set(SIGGI);
		//remove visible item
		testModel.items().remove(asList(KALLI, SIGGI));
		assertNull(testModel.selection().item().get());
		assertFalse(testModel.items().visible().contains(KALLI));
		assertFalse(testModel.items().visible().contains(SIGGI));
	}

	@Test
	void add() {
		testModel.items().clear();
		//add filtered item
		testModel.items().visible().predicate().set(item -> !item.equals(BJORN));
		testModel.items().add(BJORN);
		assertFalse(testModel.items().visible().contains(BJORN));

		//add visible items
		testModel.items().add(asList(KALLI, SIGGI));
		assertTrue(testModel.items().visible().contains(KALLI));
		assertTrue(testModel.items().visible().contains(SIGGI));

		testModel.items().visible().predicate().clear();
		assertTrue(testModel.items().visible().contains(BJORN));
	}

	@Test
	void replace() {
		String bjorninn = "bjorninn";
		testModel.selection().item().set(BJORN);
		testModel.items().replace(BJORN, bjorninn);
		assertSame(bjorninn, testModel.selection().item().get());

		String test = "test";
		testModel.items().replace("none", test);
		assertFalse(testModel.items().visible().contains(test));

		testModel.items().refresh();
		testModel.selection().item().set(bjorninn);
		testModel.items().visible().predicate().set(item -> !item.equals(bjorninn));
		assertFalse(testModel.items().visible().contains(bjorninn));
		testModel.items().replace(bjorninn, BJORN);
		assertSame(BJORN, testModel.selection().item().get());
		assertTrue(testModel.items().contains(BJORN));
		assertTrue(testModel.items().visible().contains(BJORN));

		testModel.items().refresh();
		//ANNA, KALLI, SIGGI, TOMAS, BJORN
		String karl = "karl";
		String bjorn = "bjorn";
		//replace KALLI with karl and BJORN with bjorn
		Map<String, String> replacements = new HashMap<>();
		replacements.put(KALLI, karl);
		replacements.put(BJORN, bjorn);

		testModel.items().replace(replacements);
		assertFalse(testModel.items().contains(KALLI));
		assertTrue(testModel.items().contains(karl));
		assertFalse(testModel.items().contains(BJORN));
		assertTrue(testModel.items().contains(bjorn));

		//filter karl and BJORN and replace karl and bjorn with KALLI an BJORN
		testModel.items().visible().predicate().set(testRow -> testRow != karl && testRow != BJORN);
		replacements.clear();
		replacements.put(karl, KALLI);
		replacements.put(bjorn, BJORN);

		testModel.items().replace(replacements);
		assertTrue(testModel.items().contains(KALLI));
		assertTrue(testModel.items().visible().contains(KALLI));
		assertFalse(testModel.items().contains(karl));
		assertTrue(testModel.items().contains(BJORN));
		assertTrue(testModel.items().filtered().contains(BJORN));
		assertFalse(testModel.items().contains(bjorn));
	}

	@Test
	void events() {
		AtomicInteger visibleCounter = new AtomicInteger();

		FilterComboBoxModel<String> model = FilterComboBoxModel.builder(ITEMS)
						.nullItem(NULL)
						.build();
		model.items().visible().addListener(visibleCounter::incrementAndGet);
		model.items().visible().predicate().set(item -> !item.equals(BJORN));

		assertEquals(1, visibleCounter.get());

		model.items().clear();
		assertEquals(2, visibleCounter.get());

		model.items().add(BJORN);//filtered
		assertEquals(2, visibleCounter.get());

		model.items().add(ANNA);//visible
		assertEquals(3, visibleCounter.get());

		model.items().add(asList(KALLI, SIGGI));//visible
		assertEquals(5, visibleCounter.get());

		model.items().remove(BJORN);//filtered
		assertEquals(5, visibleCounter.get());

		model.items().add(asList(BJORN, TOMAS));//filtered and visible
		assertEquals(6, visibleCounter.get());

		model.items().visible().predicate().clear();
		assertEquals(7, visibleCounter.get());
	}

	@Test
	void setNullValueString() {
		assertTrue(testModel.items().visible().contains(null));
		testModel.items().refresh();
		assertEquals(5, testModel.items().visible().count());
		assertEquals(NULL, testModel.getElementAt(0));
		testModel.setSelectedItem(null);
		assertEquals(NULL, testModel.getSelectedItem());
		assertNull(testModel.selection().item().get());
		testModel.setSelectedItem(NULL);
		assertEquals(NULL, testModel.getElementAt(0));
		assertEquals(ANNA, testModel.getElementAt(1));
	}

	@Test
	void nullItem() {
		FilterComboBoxModel<String> model = FilterComboBoxModel.<String>builder().build();
		assertFalse(model.items().contains(null));
		model = FilterComboBoxModel.<String>builder()
						.includeNull(true)
						.build();
		assertTrue(model.items().contains(null));
		model = FilterComboBoxModel.<String>builder()
						.nullItem("-")
						.build();
		assertTrue(model.items().contains(null));
		assertEquals("-", model.getSelectedItem());
		model.setSelectedItem("-");
		assertNull(model.selection().item().get());
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
		assertNull(testModel.selection().item().get());
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

		FilterComboBoxModel<Data> model = FilterComboBoxModel.builder(items).build();
		model.setSelectedItem(items.get(1));
		assertEquals("2", model.selection().item().getOrThrow().data);

		items = asList(new Data(1, "1"), new Data(2, "22"), new Data(3, "3"));

		model.items().set(items);
		assertEquals("22", model.selection().item().getOrThrow().data);
	}

	@Test
	void items() {
		List<Integer> values = asList(0, 1, 2);
		FilterComboBoxModel<Integer> model = FilterComboBoxModel.builder(() -> values).build();
		model.items().refresh();
		assertEquals(values.size(), model.items().get().size());
		assertTrue(values.containsAll(model.items().get()));
	}

	@Test
	void testItemComboBox() {
		Item<Integer> nullItem = item(null, "");
		Item<Integer> aOne = item(1, "AOne");
		Item<Integer> bTwo = item(2, "BTwo");
		Item<Integer> cThree = item(3, "CThree");
		Item<Integer> dFour = item(4, "DFour");

		List<Item<Integer>> items = asList(nullItem, cThree, bTwo, aOne, dFour);

		assertThrows(IllegalArgumentException.class, () -> FilterComboBoxModel.builder(items).selected(5));

		FilterComboBoxModel<Item<Integer>> model = FilterComboBoxModel.builder(items)
						.sorted(true)
						.selected(3)
						.build();

		assertEquals(0, model.items().visible().indexOf(null));
		assertEquals(1, model.items().visible().indexOf(aOne));
		assertEquals(2, model.items().visible().indexOf(bTwo));
		assertEquals(3, model.items().visible().indexOf(cThree));
		assertEquals(4, model.items().visible().indexOf(dFour));
		assertEquals(3, model.selection().item().get().value());

		model.setSelectedItem(1);
		assertEquals(model.getSelectedItem(), aOne);
		assertEquals(1, model.selection().item().getOrThrow().value());
		assertEquals("AOne", model.selection().item().getOrThrow().toString());
		model.setSelectedItem(2);
		assertEquals(2, model.selection().item().getOrThrow().value());
		assertEquals(model.getSelectedItem(), bTwo);
		model.setSelectedItem(4);
		assertEquals(4, model.selection().item().getOrThrow().value());
		assertEquals(model.getSelectedItem(), dFour);
		model.setSelectedItem(null);
		assertNull(model.selection().item().get());
		assertEquals(model.getSelectedItem(), nullItem);

		model.items().refresh();

		assertEquals(0, model.items().visible().indexOf(null));
		assertEquals(1, model.items().visible().indexOf(aOne));
		assertEquals(2, model.items().visible().indexOf(bTwo));
		assertEquals(3, model.items().visible().indexOf(cThree));
		assertEquals(4, model.items().visible().indexOf(dFour));

		FilterComboBoxModel<Item<Integer>> unsortedModel = FilterComboBoxModel.builder(items).build();

		assertEquals(0, unsortedModel.items().visible().indexOf(null));
		assertEquals(1, unsortedModel.items().visible().indexOf(cThree));
		assertEquals(2, unsortedModel.items().visible().indexOf(bTwo));
		assertEquals(3, unsortedModel.items().visible().indexOf(aOne));
		assertEquals(4, unsortedModel.items().visible().indexOf(dFour));
	}

	@Test
	void booleanItemComboBoxModel() {
		List<Item<Boolean>> items = booleanItems();
		FilterComboBoxModel<Item<Boolean>> model = FilterComboBoxModel.builder(items).build();
		assertSame(items.get(0), model.getSelectedItem());
		assertNull(model.selection().item().get());
		model.setSelectedItem(false);
		assertEquals(false, model.selection().item().getOrThrow().value());
		model.setSelectedItem(true);
		assertEquals(true, model.selection().item().getOrThrow().value());
		model.setSelectedItem(null);
		assertNull(model.selection().item().get());
	}

	@BeforeEach
	void setUp() {
		testModel = FilterComboBoxModel.builder(ITEMS)
						.nullItem(NULL)
						.build();
	}

	@AfterEach
	void tearDown() {
		testModel = null;
	}
}
