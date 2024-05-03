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
package is.codion.swing.common.model.component.table;

import is.codion.common.state.State;
import is.codion.swing.common.model.component.table.FilteredTableModel.Columns;
import is.codion.swing.common.model.component.table.FilteredTableModel.RefreshStrategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * User: Björn Darri
 * Date: 25.7.2010
 * Time: 13:54:59
 */
public final class DefaultFilteredTableModelTest {

	private static final TestRow A = new TestRow("a");
	private static final TestRow B = new TestRow("b");
	private static final TestRow C = new TestRow("c");
	private static final TestRow D = new TestRow("d");
	private static final TestRow E = new TestRow("e");
	private static final TestRow F = new TestRow("f");
	private static final TestRow G = new TestRow("g");
	private static final TestRow NULL = new TestRow(null);
	private static final List<TestRow> ITEMS = unmodifiableList(asList(A, B, C, D, E));

	private FilteredTableModel<TestRow, Integer> tableModel;

	private static final class TestRow {
		private final String value;

		private TestRow(String value) {
			this.value = value;
		}
	}

	private static final class TestColumns implements Columns<TestRow, Integer> {
		@Override
		public List<Integer> identifiers() {
			return singletonList(0);
		}

		@Override
		public Class<?> columnClass(Integer identifier) {
			return String.class;
		}

		@Override
		public Object value(TestRow row, Integer identifier) {
			return row.value;
		}
	}

	private static FilteredTableModel<TestRow, Integer> createTestModel() {
		return FilteredTableModel.<TestRow, Integer>builder(new TestColumns())
						.items(() -> ITEMS)
						.build();
	}

	@BeforeEach
	void setUp() {
		tableModel = createTestModel();
	}

	@Test
	void getColumnCount() {
		assertEquals(1, tableModel.getColumnCount());
	}

	@Test
	void filterItems() {
		tableModel.refresh();
		tableModel.includeCondition().set(item -> !item.equals(B) && !item.equals(F));
		assertFalse(tableModel.visible(B));
		assertTrue(tableModel.containsItem(B));
		tableModel.addItemsAt(0, Collections.singletonList(F));
		assertFalse(tableModel.visible(F));
		assertTrue(tableModel.containsItem(F));
		tableModel.includeCondition().clear();
		assertTrue(tableModel.visible(B));
		assertTrue(tableModel.visible(F));
	}

	@Test
	void filterModel() {
		tableModel.refresh();
		assertEquals(5, tableModel.visibleCount());
		tableModel.filterModel().conditionModel(0).setEqualValue("a");
		assertEquals(1, tableModel.visibleCount());
		tableModel.filterModel().conditionModel(0).setEqualValue("b");
		assertEquals(1, tableModel.visibleCount());
		tableModel.filterModel().conditionModel(0).clear();
	}

	@Test
	void addItemsAt() {
		tableModel.refresh();
		tableModel.addItemsAt(2, asList(F, G));
		assertEquals(2, tableModel.indexOf(F));
		assertEquals(3, tableModel.indexOf(G));
		assertEquals(4, tableModel.indexOf(C));
	}

	@Test
	void nullColumns() {
		assertThrows(NullPointerException.class, () -> FilteredTableModel.<String, Integer>builder(null));
	}

	@Test
	void noColumns() {
		assertThrows(IllegalArgumentException.class, () ->
						FilteredTableModel.<String, Integer>builder(new Columns<String, Integer>() {
							@Override
							public List<Integer> identifiers() {
								return emptyList();
							}

							@Override
							public Class<?> columnClass(Integer integer) {
								return null;
							}

							@Override
							public Object value(String row, Integer integer) {
								return null;
							}
						}));
	}

	@Test
	void refreshEvents() {
		AtomicInteger done = new AtomicInteger();
		AtomicInteger cleared = new AtomicInteger();
		Runnable successfulConsumer = done::incrementAndGet;
		Consumer<Throwable> failedConsumer = exception -> {};
		Runnable clearedListener = cleared::incrementAndGet;
		tableModel.refresher().refreshEvent().addListener(successfulConsumer);
		tableModel.refresher().refreshFailedEvent().addConsumer(failedConsumer);
		tableModel.clearedEvent().addListener(clearedListener);
		tableModel.refresh();
		assertTrue(tableModel.getRowCount() > 0);
		assertEquals(1, done.get());
		assertEquals(1, cleared.get());

		done.set(0);
		cleared.set(0);
		tableModel.refreshStrategy().set(RefreshStrategy.MERGE);
		tableModel.refresh();
		assertEquals(1, done.get());
		assertEquals(0, cleared.get());

		tableModel.refresher().refreshEvent().removeListener(successfulConsumer);
		tableModel.refresher().refreshFailedEvent().removeConsumer(failedConsumer);
		tableModel.clearedEvent().removeListener(clearedListener);
	}

	@Test
	void mergeOnRefresh() {
		AtomicInteger selectionEvents = new AtomicInteger();
		List<TestRow> items = new ArrayList<>(ITEMS);
		FilteredTableModel<TestRow, Integer> testModel =
						FilteredTableModel.<TestRow, Integer>builder(new TestColumns())
										.items(() -> items)
										.build();
		testModel.selectionModel().selectionEvent().addListener(selectionEvents::incrementAndGet);
		testModel.refreshStrategy().set(RefreshStrategy.MERGE);
		testModel.refresh();
		testModel.comparator().set(Comparator.comparing(o -> o.value));
//		testModel.sortModel().setSortOrder(0, SortOrder.ASCENDING);
		testModel.selectionModel().setSelectedIndex(1);//b

		assertEquals(1, selectionEvents.get());
		assertSame(B, testModel.selectionModel().getSelectedItem());

		items.remove(C);
		testModel.refresh();
		assertEquals(1, selectionEvents.get());

		items.remove(B);
		testModel.refresh();
		assertTrue(testModel.selectionModel().isSelectionEmpty());
		assertEquals(2, selectionEvents.get());

		testModel.selectionModel().setSelectedItem(E);
		assertEquals(3, selectionEvents.get());

		testModel.includeCondition().set(item -> !item.equals(E));
		assertEquals(4, selectionEvents.get());

		items.add(B);

		testModel.refresh();
		//merge does not sort new items
		testModel.sortItems();

		testModel.selectionModel().setSelectedIndex(1);//b

		assertEquals(5, selectionEvents.get());
		assertSame(B, testModel.selectionModel().getSelectedItem());
	}

	@Test
	void removeItems() {
		AtomicInteger events = new AtomicInteger();
		Runnable listener = events::incrementAndGet;
		tableModel.dataChangedEvent().addListener(listener);
		tableModel.refresh();
		assertEquals(1, events.get());
		tableModel.filterModel().conditionModel(0).setEqualValue("a");
		tableModel.removeItem(B);
		assertEquals(3, events.get());
		assertFalse(tableModel.visible(B));
		assertTrue(tableModel.containsItem(A));
		tableModel.removeItem(A);
		assertEquals(4, events.get());
		assertFalse(tableModel.containsItem(A));
		tableModel.removeItems(asList(D, E));
		assertEquals(4, events.get());//no change event when removing filtered items
		assertFalse(tableModel.visible(D));
		assertFalse(tableModel.visible(E));
		assertFalse(tableModel.filtered(D));
		assertFalse(tableModel.filtered(E));
		tableModel.filterModel().conditionModel(0).setEqualValue(null);
		tableModel.refresh();
		assertEquals(7, events.get());
		tableModel.removeItems(0, 2);
		assertEquals(8, events.get());//just a single event when removing multiple items
		tableModel.removeItemAt(0);
		assertEquals(9, events.get());
		tableModel.dataChangedEvent().removeListener(listener);
	}

	@Test
	void setItemAt() {
		AtomicInteger dataChangedEvents = new AtomicInteger();
		Runnable listener = dataChangedEvents::incrementAndGet;
		tableModel.dataChangedEvent().addListener(listener);
		State selectionChangedState = State.state();
		tableModel.selectionModel().selectedItemEvent().addConsumer((item) -> selectionChangedState.set(true));
		tableModel.refresh();
		assertEquals(1, dataChangedEvents.get());
		tableModel.selectionModel().setSelectedItem(B);
		TestRow h = new TestRow("h");
		tableModel.setItemAt(tableModel.indexOf(B), h);
		assertEquals(2, dataChangedEvents.get());
		assertEquals(h, tableModel.selectionModel().getSelectedItem());
		assertTrue(selectionChangedState.get());
		tableModel.setItemAt(tableModel.indexOf(h), B);
		assertEquals(3, dataChangedEvents.get());

		selectionChangedState.set(false);
		TestRow newB = new TestRow("b");
		tableModel.setItemAt(tableModel.indexOf(B), newB);
		assertFalse(selectionChangedState.get());
		assertEquals(newB, tableModel.selectionModel().getSelectedItem());
		tableModel.dataChangedEvent().removeListener(listener);
	}

	@Test
	void removeItemsRange() {
		AtomicInteger events = new AtomicInteger();
		Runnable listener = events::incrementAndGet;
		tableModel.dataChangedEvent().addListener(listener);
		tableModel.refresh();
		assertEquals(1, events.get());
		List<TestRow> removed = tableModel.removeItems(1, 3);
		assertEquals(2, events.get());
		assertTrue(tableModel.containsItem(A));
		assertFalse(tableModel.containsItem(B));
		assertTrue(removed.contains(B));
		assertFalse(tableModel.containsItem(C));
		assertTrue(removed.contains(C));
		assertTrue(tableModel.containsItem(D));
		assertTrue(tableModel.containsItem(E));

		tableModel.refreshStrategy().set(RefreshStrategy.MERGE);
		events.set(0);
		tableModel.refresh();
		assertEquals(5, events.get());

		tableModel.dataChangedEvent().removeListener(listener);
	}

	@Test
	void clear() {
		tableModel.refresh();
		assertTrue(tableModel.getRowCount() > 0);
		tableModel.clear();
		assertEquals(0, tableModel.getRowCount());
	}

	@Test
	void addSelectedIndexesNegative() {
		Collection<Integer> indexes = new ArrayList<>();
		indexes.add(1);
		indexes.add(-1);
		assertThrows(IndexOutOfBoundsException.class, () -> tableModel.selectionModel().addSelectedIndexes(indexes));
	}

	@Test
	void addSelectedIndexesOutOfBounds() {
		Collection<Integer> indexes = new ArrayList<>();
		indexes.add(1);
		indexes.add(10);
		assertThrows(IndexOutOfBoundsException.class, () -> tableModel.selectionModel().addSelectedIndexes(indexes));
	}

	@Test
	void setSelectedIndexesNegative() {
		Collection<Integer> indexes = new ArrayList<>();
		indexes.add(1);
		indexes.add(-1);
		assertThrows(IndexOutOfBoundsException.class, () -> tableModel.selectionModel().setSelectedIndexes(indexes));
	}

	@Test
	void setSelectedIndexesOutOfBounds() {
		Collection<Integer> indexes = new ArrayList<>();
		indexes.add(1);
		indexes.add(10);
		assertThrows(IndexOutOfBoundsException.class, () -> tableModel.selectionModel().setSelectedIndexes(indexes));
	}

	@Test
	void setSelectedIndexNegative() {
		assertThrows(IndexOutOfBoundsException.class, () -> tableModel.selectionModel().setSelectedIndex(-1));
	}

	@Test
	void setSelectedIndexOutOfBounds() {
		assertThrows(IndexOutOfBoundsException.class, () -> tableModel.selectionModel().setSelectedIndex(10));
	}

	@Test
	void addSelectedIndexNegative() {
		assertThrows(IndexOutOfBoundsException.class, () -> tableModel.selectionModel().addSelectedIndex(-1));
	}

	@Test
	void addSelectedIndexOutOfBounds() {
		assertThrows(IndexOutOfBoundsException.class, () -> tableModel.selectionModel().addSelectedIndex(10));
	}

	@Test
	void selection() {
		AtomicInteger events = new AtomicInteger();
		Runnable listener = events::incrementAndGet;
		Consumer consumer = data -> listener.run();
		FilteredTableSelectionModel<TestRow> selectionModel = tableModel.selectionModel();
		selectionModel.selectedIndexEvent().addConsumer(consumer);
		selectionModel.selectionEvent().addListener(listener);
		selectionModel.selectedItemEvent().addConsumer(consumer);
		selectionModel.selectedItemsEvent().addConsumer(consumer);

		assertFalse(selectionModel.singleSelection().get());
		assertTrue(selectionModel.selectionEmpty().get());
		assertFalse(selectionModel.selectionNotEmpty().get());
		assertFalse(selectionModel.multipleSelection().get());

		tableModel.refresh();
		selectionModel.setSelectedIndex(2);
		assertEquals(4, events.get());
		assertTrue(selectionModel.singleSelection().get());
		assertFalse(selectionModel.selectionEmpty().get());
		assertFalse(selectionModel.multipleSelection().get());
		assertEquals(2, selectionModel.getSelectedIndex());
		selectionModel.moveSelectionDown();
		assertEquals(8, events.get());
		assertEquals(3, selectionModel.getSelectedIndex());
		selectionModel.moveSelectionUp();
		selectionModel.moveSelectionUp();
		assertEquals(1, selectionModel.getSelectedIndex());

		selectionModel.moveSelectionDown();
		selectionModel.moveSelectionDown();

		assertEquals(3, selectionModel.getSelectedIndex());

		selectionModel.setSelectedIndex(0);
		selectionModel.moveSelectionUp();
		assertEquals(tableModel.getRowCount() - 1, selectionModel.getSelectedIndex());

		selectionModel.setSelectedIndex(tableModel.getRowCount() - 1);
		selectionModel.moveSelectionDown();
		assertEquals(0, selectionModel.getSelectedIndex());

		selectionModel.clearSelection();
		selectionModel.moveSelectionUp();
		assertEquals(tableModel.getRowCount() - 1, selectionModel.getSelectedIndex());

		selectionModel.clearSelection();
		selectionModel.moveSelectionDown();
		assertEquals(0, selectionModel.getSelectedIndex());

		selectionModel.selectAll();
		assertEquals(5, selectionModel.getSelectedItems().size());
		selectionModel.clearSelection();
		assertFalse(selectionModel.singleSelection().get());
		assertTrue(selectionModel.selectionEmpty().get());
		assertFalse(selectionModel.multipleSelection().get());
		assertEquals(0, selectionModel.getSelectedItems().size());

		selectionModel.setSelectedItem(ITEMS.get(0));
		assertFalse(selectionModel.multipleSelection().get());
		assertEquals(ITEMS.get(0), selectionModel.getSelectedItem());
		assertEquals(0, selectionModel.getSelectedIndex());
		assertEquals(1, selectionModel.selectionCount());
		assertFalse(selectionModel.isSelectionEmpty());
		selectionModel.addSelectedIndex(1);
		assertTrue(selectionModel.multipleSelection().get());
		assertEquals(ITEMS.get(0), selectionModel.getSelectedItem());
		assertEquals(asList(0, 1), selectionModel.getSelectedIndexes());
		assertEquals(0, selectionModel.getSelectedIndex());
		selectionModel.addSelectedIndex(4);
		assertTrue(selectionModel.multipleSelection().get());
		assertEquals(asList(0, 1, 4), selectionModel.getSelectedIndexes());
		selectionModel.removeIndexInterval(1, 4);
		assertEquals(singletonList(0), selectionModel.getSelectedIndexes());
		assertEquals(0, selectionModel.getSelectedIndex());
		selectionModel.clearSelection();
		assertEquals(new ArrayList<Integer>(), selectionModel.getSelectedIndexes());
		assertEquals(-1, selectionModel.getMinSelectionIndex());
		selectionModel.addSelectedIndexes(asList(0, 3, 4));
		assertEquals(asList(0, 3, 4), selectionModel.getSelectedIndexes());
		assertEquals(0, selectionModel.getMinSelectionIndex());
		assertEquals(3, selectionModel.selectionCount());
		selectionModel.removeSelectionInterval(0, 0);
		assertEquals(3, selectionModel.getMinSelectionIndex());
		selectionModel.removeSelectionInterval(3, 3);
		assertEquals(4, selectionModel.getMinSelectionIndex());

		selectionModel.addSelectedIndexes(asList(0, 3, 4));
		assertEquals(0, selectionModel.getMinSelectionIndex());
		selectionModel.removeSelectionInterval(3, 3);
		assertEquals(0, selectionModel.getMinSelectionIndex());
		selectionModel.clearSelection();
		assertEquals(-1, selectionModel.getMinSelectionIndex());

		selectionModel.addSelectedIndexes(asList(0, 1, 2, 3, 4));
		assertEquals(0, selectionModel.getMinSelectionIndex());
		selectionModel.removeSelectionInterval(0, 0);
		assertEquals(1, selectionModel.getMinSelectionIndex());
		selectionModel.removeSelectedIndex(1);
		assertEquals(2, selectionModel.getMinSelectionIndex());
		selectionModel.removeSelectedIndexes(singletonList(2));
		assertEquals(3, selectionModel.getMinSelectionIndex());
		selectionModel.removeSelectionInterval(3, 3);
		assertEquals(4, selectionModel.getMinSelectionIndex());
		selectionModel.removeSelectedIndex(4);
		assertEquals(-1, selectionModel.getMinSelectionIndex());

		selectionModel.addSelectedItem(ITEMS.get(0));
		assertEquals(1, selectionModel.selectionCount());
		assertEquals(0, selectionModel.getMinSelectionIndex());
		selectionModel.addSelectedItems(asList(ITEMS.get(1), ITEMS.get(2)));
		assertEquals(3, selectionModel.selectionCount());
		assertEquals(0, selectionModel.getMinSelectionIndex());
		selectionModel.removeSelectedItem(ITEMS.get(1));
		assertEquals(2, selectionModel.selectionCount());
		assertEquals(0, selectionModel.getMinSelectionIndex());
		selectionModel.removeSelectedItem(ITEMS.get(2));
		assertEquals(1, selectionModel.selectionCount());
		assertEquals(0, selectionModel.getMinSelectionIndex());
		selectionModel.addSelectedItems(asList(ITEMS.get(1), ITEMS.get(2)));
		assertEquals(3, selectionModel.selectionCount());
		assertEquals(0, selectionModel.getMinSelectionIndex());
		selectionModel.addSelectedItem(ITEMS.get(4));
		assertEquals(4, selectionModel.selectionCount());
		assertEquals(0, selectionModel.getMinSelectionIndex());
		tableModel.removeItem(ITEMS.get(0));
		assertEquals(3, selectionModel.selectionCount());
		assertEquals(0, selectionModel.getMinSelectionIndex());

		tableModel.clear();
		assertTrue(selectionModel.selectionEmpty().get());
		assertFalse(selectionModel.selectionNotEmpty().get());
		assertNull(selectionModel.getSelectedItem());

		selectionModel.clearSelection();
		selectionModel.selectedIndexEvent().removeConsumer(consumer);
		selectionModel.selectionEvent().removeListener(listener);
		selectionModel.selectedItemEvent().removeConsumer(consumer);
		selectionModel.selectedItemsEvent().removeConsumer(consumer);
	}

	@Test
	void selectionAndFiltering() {
		tableModel.refresh();
		tableModel.selectionModel().addSelectedIndexes(singletonList(3));
		assertEquals(3, tableModel.selectionModel().getMinSelectionIndex());

		tableModel.filterModel().conditionModel(0).setEqualValue("d");
		assertEquals(0, tableModel.selectionModel().getMinSelectionIndex());
		assertEquals(singletonList(0), tableModel.selectionModel().getSelectedIndexes());
		tableModel.filterModel().conditionModel(0).enabled().set(false);
		assertEquals(0, tableModel.selectionModel().getMinSelectionIndex());
		assertEquals(ITEMS.get(3), tableModel.selectionModel().getSelectedItem());
	}

	@Test
	void includeCondition() {
		tableModel.refresh();
		tableModel.includeCondition().set(item -> false);
		assertEquals(0, tableModel.getRowCount());
	}

	@Test
	void columns() {
		assertEquals(1, tableModel.getColumnCount());
	}

	@Test
	void filterAndRemove() {
		tableModel.refresh();
		tableModel.filterModel().conditionModel(0).setEqualValue("a");
		assertTrue(tableModel.containsItem(B));
		tableModel.removeItem(B);
		assertFalse(tableModel.containsItem(B));
		tableModel.removeItem(A);
		assertFalse(tableModel.containsItem(A));
	}

	@Test
	void filtering() {
		tableModel.refresh();
		assertTrue(tableModelContainsAll(ITEMS, false, tableModel));
		assertTrue(tableModel.includeCondition().isNull());

		//test filters
		assertNotNull(tableModel.filterModel().conditionModel(0));
		assertTrue(tableModel.visible(B));
		tableModel.filterModel().conditionModel(0).setEqualValue("a");
		assertTrue(tableModel.visible(A));
		assertFalse(tableModel.visible(B));
		assertTrue(tableModel.filtered(D));

		tableModel.includeCondition().set(strings -> !strings.equals(A));
		assertTrue(tableModel.includeCondition().isNotNull());
		assertFalse(tableModel.visible(A));
		tableModel.includeCondition().clear();
		assertTrue(tableModel.visible(A));

		assertFalse(tableModel.visible(B));
		assertTrue(tableModel.containsItem(B));
		assertTrue(tableModel.filterModel().conditionModel(0).enabled().get());
		assertEquals(4, tableModel.filteredCount());
		assertFalse(tableModelContainsAll(ITEMS, false, tableModel));
		assertTrue(tableModelContainsAll(ITEMS, true, tableModel));

		assertFalse(tableModel.visibleItems().isEmpty());
		assertFalse(tableModel.filteredItems().isEmpty());
		assertFalse(tableModel.items().isEmpty());

		tableModel.filterModel().conditionModel(0).enabled().set(false);
		assertFalse(tableModel.filterModel().conditionModel(0).enabled().get());

		assertTrue(tableModelContainsAll(ITEMS, false, tableModel));

		tableModel.filterModel().conditionModel(0).setEqualValue("t"); // ekki til
		assertTrue(tableModel.filterModel().conditionModel(0).enabled().get());
		assertEquals(5, tableModel.filteredCount());
		assertFalse(tableModelContainsAll(ITEMS, false, tableModel));
		assertTrue(tableModelContainsAll(ITEMS, true, tableModel));
		tableModel.filterModel().conditionModel(0).enabled().set(false);
		assertTrue(tableModelContainsAll(ITEMS, false, tableModel));
		assertFalse(tableModel.filterModel().conditionModel(0).enabled().get());

		tableModel.filterModel().conditionModel(0).setEqualValue("b");
		int rowCount = tableModel.getRowCount();
		tableModel.addItemsAt(0, singletonList(new TestRow("x")));
		assertEquals(rowCount, tableModel.getRowCount());

		assertThrows(IllegalArgumentException.class, () -> tableModel.filterModel().conditionModel(1));
	}

	@Test
	void clearFilterModels() {
		assertFalse(tableModel.filterModel().enabled(0));
		tableModel.filterModel().conditionModel(0).setEqualValue("SCOTT");
		assertTrue(tableModel.filterModel().enabled(0));
		tableModel.filterModel().clear();
		assertFalse(tableModel.filterModel().enabled(0));
	}

	@Test
	void values() {
		tableModel.refresh();
		tableModel.selectionModel().setSelectedIndexes(asList(0, 2));
		Collection<String> values = tableModel.selectedValues(0);
		assertEquals(2, values.size());
		assertTrue(values.contains("a"));
		assertTrue(values.contains("c"));

		values = tableModel.values(0);
		assertEquals(5, values.size());
		assertTrue(values.contains("a"));
		assertTrue(values.contains("b"));
		assertTrue(values.contains("c"));
		assertTrue(values.contains("d"));
		assertTrue(values.contains("e"));
		assertFalse(values.contains("zz"));
	}

	@Test
	void getColumnClass() {
		assertEquals(String.class, tableModel.getColumnClass(0));
	}

	private static boolean tableModelContainsAll(List<TestRow> rows, boolean includeFiltered,
																							 FilteredTableModel<TestRow, Integer> model) {
		for (TestRow row : rows) {
			if (includeFiltered) {
				if (!model.containsItem(row)) {
					return false;
				}
			}
			else if (!model.visible(row)) {
				return false;
			}
		}

		return true;
	}
}
