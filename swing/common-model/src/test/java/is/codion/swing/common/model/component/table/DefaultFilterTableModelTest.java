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
import is.codion.swing.common.model.component.table.FilterTableModel.Columns;
import is.codion.swing.common.model.component.table.FilterTableModel.RefreshStrategy;

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
public final class DefaultFilterTableModelTest {

	private static final TestRow A = new TestRow("a");
	private static final TestRow B = new TestRow("b");
	private static final TestRow C = new TestRow("c");
	private static final TestRow D = new TestRow("d");
	private static final TestRow E = new TestRow("e");
	private static final TestRow F = new TestRow("f");
	private static final TestRow G = new TestRow("g");
	private static final List<TestRow> ITEMS = unmodifiableList(asList(A, B, C, D, E));

	private FilterTableModel<TestRow, Integer> tableModel;

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

	private static FilterTableModel<TestRow, Integer> createTestModel() {
		return FilterTableModel.<TestRow, Integer>builder(new TestColumns())
						.items(() -> ITEMS)
						.build();
	}

	@BeforeEach
	void setUp() {
		tableModel = createTestModel();
	}

	@Test
	void nonUniqueColumnIdentifiers() {
		assertThrows(IllegalArgumentException.class, () -> FilterTableModel.builder(new Columns<Object, Object>() {
			@Override
			public List<Object> identifiers() {
				return List.of(0, 1, 0);
			}

			@Override
			public Class<?> columnClass(Object o) {
				return Object.class;
			}

			@Override
			public Object value(Object row, Object o) {
				return null;
			}
		}));
	}

	@Test
	void getColumnCount() {
		assertEquals(1, tableModel.getColumnCount());
	}

	@Test
	void filterItems() {
		tableModel.refresh();
		tableModel.items().visiblePredicate().set(item -> !item.equals(B) && !item.equals(F));
		assertFalse(tableModel.items().visible().contains(B));
		assertTrue(tableModel.items().contains(B));
		tableModel.addItemsAt(0, Collections.singletonList(F));
		assertFalse(tableModel.items().visible().contains(F));
		assertTrue(tableModel.items().contains(F));
		tableModel.items().visiblePredicate().clear();
		assertTrue(tableModel.items().visible().contains(B));
		assertTrue(tableModel.items().visible().contains(F));
	}

	@Test
	void filterModel() {
		tableModel.refresh();
		assertEquals(5, tableModel.items().visible().size());
		tableModel.filterModel().conditionModel(0).operands().equal().set("a");
		assertEquals(1, tableModel.items().visible().size());
		tableModel.filterModel().conditionModel(0).operands().equal().set("b");
		assertEquals(1, tableModel.items().visible().size());
		tableModel.filterModel().conditionModel(0).clear();
	}

	@Test
	void addItemsAt() {
		tableModel.refresh();
		tableModel.addItemsAt(2, asList(F, G));
		assertEquals(2, tableModel.items().visible().indexOf(F));
		assertEquals(3, tableModel.items().visible().indexOf(G));
		assertEquals(4, tableModel.items().visible().indexOf(C));
	}

	@Test
	void nullColumns() {
		assertThrows(NullPointerException.class, () -> FilterTableModel.<String, Integer>builder(null));
	}

	@Test
	void noColumns() {
		assertThrows(IllegalArgumentException.class, () ->
						FilterTableModel.<String, Integer>builder(new Columns<String, Integer>() {
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
		Runnable successfulListener = done::incrementAndGet;
		Consumer<Throwable> failedConsumer = exception -> {};
		tableModel.refresher().success().addListener(successfulListener);
		tableModel.refresher().failure().addConsumer(failedConsumer);
		tableModel.refresh();
		assertFalse(tableModel.items().visible().get().isEmpty());
		assertEquals(1, done.get());

		done.set(0);
		tableModel.refreshStrategy().set(RefreshStrategy.MERGE);
		tableModel.refresh();
		assertEquals(1, done.get());

		tableModel.refresher().success().removeListener(successfulListener);
		tableModel.refresher().failure().removeConsumer(failedConsumer);
	}

	@Test
	void mergeOnRefresh() {
		AtomicInteger selectionEvents = new AtomicInteger();
		List<TestRow> items = new ArrayList<>(ITEMS);
		FilterTableModel<TestRow, Integer> testModel =
						FilterTableModel.<TestRow, Integer>builder(new TestColumns())
										.items(() -> items)
										.build();
		testModel.selection().indexes().addListener(selectionEvents::incrementAndGet);
		testModel.refreshStrategy().set(RefreshStrategy.MERGE);
		testModel.refresh();
		testModel.comparator().set(Comparator.comparing(o -> o.value));
//		testModel.sortModel().setSortOrder(0, SortOrder.ASCENDING);
		testModel.selection().index().set(1);//b

		assertEquals(1, selectionEvents.get());
		assertSame(B, testModel.selection().item().get());

		items.remove(C);
		testModel.refresh();
		assertEquals(1, selectionEvents.get());

		items.remove(B);
		testModel.refresh();
		assertTrue(testModel.selection().isSelectionEmpty());
		assertEquals(2, selectionEvents.get());

		testModel.selection().item().set(E);
		assertEquals(3, selectionEvents.get());

		testModel.items().visiblePredicate().set(item -> !item.equals(E));
		assertEquals(4, selectionEvents.get());

		items.add(B);

		testModel.refresh();
		//merge does not sort new items
		testModel.sort();

		testModel.selection().index().set(1);//b

		assertEquals(5, selectionEvents.get());
		assertSame(B, testModel.selection().item().get());
	}

	@Test
	void removeItems() {
		AtomicInteger events = new AtomicInteger();
		Runnable listener = events::incrementAndGet;
		tableModel.items().visible().addListener(listener);
		tableModel.refresh();
		assertEquals(1, events.get());
		tableModel.filterModel().conditionModel(0).operands().equal().set("a");
		tableModel.removeItem(B);
		assertEquals(3, events.get());
		assertFalse(tableModel.items().visible().contains(B));
		assertTrue(tableModel.items().contains(A));
		tableModel.removeItem(A);
		assertEquals(4, events.get());
		assertFalse(tableModel.items().contains(A));
		tableModel.removeItems(asList(D, E));
		assertEquals(4, events.get());//no change event when removing filtered items
		assertFalse(tableModel.items().visible().contains(D));
		assertFalse(tableModel.items().visible().contains(E));
		assertFalse(tableModel.items().filtered().contains(D));
		assertFalse(tableModel.items().filtered().contains(E));
		tableModel.filterModel().conditionModel(0).operands().equal().set(null);
		tableModel.refresh();//two events, clear and add
		assertEquals(8, events.get());
		tableModel.removeItems(0, 2);
		assertEquals(9, events.get());//just a single event when removing multiple items
		tableModel.removeItemAt(0);
		assertEquals(10, events.get());
		tableModel.items().visible().removeListener(listener);
	}

	@Test
	void setItemAt() {
		AtomicInteger dataChangedEvents = new AtomicInteger();
		Runnable listener = dataChangedEvents::incrementAndGet;
		tableModel.items().visible().addListener(listener);
		State selectionChangedState = State.state();
		tableModel.selection().item().addConsumer((item) -> selectionChangedState.set(true));
		tableModel.refresh();
		assertEquals(1, dataChangedEvents.get());
		tableModel.selection().item().set(B);
		TestRow h = new TestRow("h");
		tableModel.setItemAt(tableModel.items().visible().indexOf(B), h);
		assertEquals(2, dataChangedEvents.get());
		assertEquals(h, tableModel.selection().item().get());
		assertTrue(selectionChangedState.get());
		tableModel.setItemAt(tableModel.items().visible().indexOf(h), B);
		assertEquals(3, dataChangedEvents.get());

		selectionChangedState.set(false);
		TestRow newB = new TestRow("b");
		tableModel.setItemAt(tableModel.items().visible().indexOf(B), newB);
		assertFalse(selectionChangedState.get());
		assertEquals(newB, tableModel.selection().item().get());
		tableModel.items().visible().removeListener(listener);
	}

	@Test
	void removeItemsRange() {
		AtomicInteger events = new AtomicInteger();
		Runnable listener = events::incrementAndGet;
		tableModel.items().visible().addListener(listener);
		tableModel.refresh();
		assertEquals(1, events.get());
		List<TestRow> removed = tableModel.removeItems(1, 3);
		assertEquals(2, events.get());
		assertTrue(tableModel.items().contains(A));
		assertFalse(tableModel.items().contains(B));
		assertTrue(removed.contains(B));
		assertFalse(tableModel.items().contains(C));
		assertTrue(removed.contains(C));
		assertTrue(tableModel.items().contains(D));
		assertTrue(tableModel.items().contains(E));

		tableModel.refreshStrategy().set(RefreshStrategy.MERGE);
		events.set(0);
		tableModel.refresh();
		assertEquals(5, events.get());

		tableModel.items().visible().removeListener(listener);
	}

	@Test
	void clear() {
		tableModel.refresh();
		assertTrue(tableModel.items().visible().size() > 0);
		tableModel.clear();
		assertEquals(0, tableModel.items().visible().size());
	}

	@Test
	void addSelectedIndexesNegative() {
		Collection<Integer> indexes = new ArrayList<>();
		indexes.add(1);
		indexes.add(-1);
		assertThrows(IndexOutOfBoundsException.class, () -> tableModel.selection().indexes().add(indexes));
	}

	@Test
	void addSelectedIndexesOutOfBounds() {
		Collection<Integer> indexes = new ArrayList<>();
		indexes.add(1);
		indexes.add(10);
		assertThrows(IndexOutOfBoundsException.class, () -> tableModel.selection().indexes().add(indexes));
	}

	@Test
	void setSelectedIndexesNegative() {
		List<Integer> indexes = new ArrayList<>();
		indexes.add(1);
		indexes.add(-1);
		assertThrows(IndexOutOfBoundsException.class, () -> tableModel.selection().indexes().set(indexes));
	}

	@Test
	void setSelectedIndexesOutOfBounds() {
		List<Integer> indexes = new ArrayList<>();
		indexes.add(1);
		indexes.add(10);
		assertThrows(IndexOutOfBoundsException.class, () -> tableModel.selection().indexes().set(indexes));
	}

	@Test
	void setSelectedIndexNegative() {
		assertThrows(IndexOutOfBoundsException.class, () -> tableModel.selection().index().set(-1));
	}

	@Test
	void setSelectedIndexOutOfBounds() {
		assertThrows(IndexOutOfBoundsException.class, () -> tableModel.selection().index().set(10));
	}

	@Test
	void addSelectedIndexNegative() {
		assertThrows(IndexOutOfBoundsException.class, () -> tableModel.selection().indexes().add(-1));
	}

	@Test
	void addSelectedIndexOutOfBounds() {
		assertThrows(IndexOutOfBoundsException.class, () -> tableModel.selection().indexes().add(10));
	}

	@Test
	void selection() {
		AtomicInteger events = new AtomicInteger();
		Runnable listener = events::incrementAndGet;
		Consumer consumer = data -> listener.run();
		FilterTableSelectionModel<TestRow> selectionModel = tableModel.selection();
		selectionModel.index().addConsumer(consumer);
		selectionModel.indexes().addListener(listener);
		selectionModel.item().addConsumer(consumer);
		selectionModel.items().addConsumer(consumer);

		assertFalse(selectionModel.single().get());
		assertTrue(selectionModel.empty().get());
		assertFalse(selectionModel.multiple().get());

		tableModel.refresh();
		selectionModel.index().set(2);
		assertEquals(4, events.get());
		assertTrue(selectionModel.single().get());
		assertFalse(selectionModel.empty().get());
		assertFalse(selectionModel.multiple().get());
		assertEquals(2, selectionModel.index().get());
		selectionModel.indexes().moveDown();
		assertEquals(8, events.get());
		assertEquals(3, selectionModel.index().get());
		selectionModel.indexes().moveUp();
		selectionModel.indexes().moveUp();
		assertEquals(1, selectionModel.index().get());

		selectionModel.indexes().moveDown();
		selectionModel.indexes().moveDown();

		assertEquals(3, selectionModel.index().get());

		selectionModel.index().set(0);
		selectionModel.indexes().moveUp();
		assertEquals(tableModel.items().visible().size() - 1, selectionModel.index().get());

		selectionModel.index().set(tableModel.items().visible().size() - 1);
		selectionModel.indexes().moveDown();
		assertEquals(0, selectionModel.index().get());

		selectionModel.clearSelection();
		selectionModel.indexes().moveUp();
		assertEquals(tableModel.items().visible().size() - 1, selectionModel.index().get());

		selectionModel.clearSelection();
		selectionModel.indexes().moveDown();
		assertEquals(0, selectionModel.index().get());

		selectionModel.selectAll();
		assertEquals(5, selectionModel.items().get().size());
		selectionModel.clearSelection();
		assertFalse(selectionModel.single().get());
		assertTrue(selectionModel.empty().get());
		assertFalse(selectionModel.multiple().get());
		assertEquals(0, selectionModel.items().get().size());

		selectionModel.item().set(ITEMS.get(0));
		assertFalse(selectionModel.multiple().get());
		assertEquals(ITEMS.get(0), selectionModel.item().get());
		assertEquals(0, selectionModel.index().get());
		assertEquals(1, selectionModel.count());
		assertFalse(selectionModel.isSelectionEmpty());
		selectionModel.indexes().add(1);
		assertTrue(selectionModel.multiple().get());
		assertEquals(ITEMS.get(0), selectionModel.item().get());
		assertEquals(asList(0, 1), selectionModel.indexes().get());
		assertEquals(0, selectionModel.index().get());
		selectionModel.indexes().add(4);
		assertTrue(selectionModel.multiple().get());
		assertEquals(asList(0, 1, 4), selectionModel.indexes().get());
		selectionModel.removeIndexInterval(1, 4);
		assertEquals(singletonList(0), selectionModel.indexes().get());
		assertEquals(0, selectionModel.index().get());
		selectionModel.clearSelection();
		assertEquals(new ArrayList<Integer>(), selectionModel.indexes().get());
		assertEquals(-1, selectionModel.getMinSelectionIndex());
		selectionModel.indexes().add(asList(0, 3, 4));
		assertEquals(asList(0, 3, 4), selectionModel.indexes().get());
		assertEquals(0, selectionModel.getMinSelectionIndex());
		assertEquals(3, selectionModel.count());
		selectionModel.removeSelectionInterval(0, 0);
		assertEquals(3, selectionModel.getMinSelectionIndex());
		selectionModel.removeSelectionInterval(3, 3);
		assertEquals(4, selectionModel.getMinSelectionIndex());

		selectionModel.indexes().add(asList(0, 3, 4));
		assertEquals(0, selectionModel.getMinSelectionIndex());
		selectionModel.removeSelectionInterval(3, 3);
		assertEquals(0, selectionModel.getMinSelectionIndex());
		selectionModel.clearSelection();
		assertEquals(-1, selectionModel.getMinSelectionIndex());

		selectionModel.indexes().add(asList(0, 1, 2, 3, 4));
		assertEquals(0, selectionModel.getMinSelectionIndex());
		selectionModel.removeSelectionInterval(0, 0);
		assertEquals(1, selectionModel.getMinSelectionIndex());
		selectionModel.indexes().remove(1);
		assertEquals(2, selectionModel.getMinSelectionIndex());
		selectionModel.indexes().remove(singletonList(2));
		assertEquals(3, selectionModel.getMinSelectionIndex());
		selectionModel.removeSelectionInterval(3, 3);
		assertEquals(4, selectionModel.getMinSelectionIndex());
		selectionModel.indexes().remove(4);
		assertEquals(-1, selectionModel.getMinSelectionIndex());

		selectionModel.items().add(ITEMS.get(0));
		assertEquals(1, selectionModel.count());
		assertEquals(0, selectionModel.getMinSelectionIndex());
		selectionModel.items().add(asList(ITEMS.get(1), ITEMS.get(2)));
		assertEquals(3, selectionModel.count());
		assertEquals(0, selectionModel.getMinSelectionIndex());
		selectionModel.items().remove(ITEMS.get(1));
		assertEquals(2, selectionModel.count());
		assertEquals(0, selectionModel.getMinSelectionIndex());
		selectionModel.items().remove(ITEMS.get(2));
		assertEquals(1, selectionModel.count());
		assertEquals(0, selectionModel.getMinSelectionIndex());
		selectionModel.items().add(asList(ITEMS.get(1), ITEMS.get(2)));
		assertEquals(3, selectionModel.count());
		assertEquals(0, selectionModel.getMinSelectionIndex());
		selectionModel.items().add(ITEMS.get(4));
		assertEquals(4, selectionModel.count());
		assertEquals(0, selectionModel.getMinSelectionIndex());
		tableModel.removeItem(ITEMS.get(0));
		assertEquals(3, selectionModel.count());
		assertEquals(0, selectionModel.getMinSelectionIndex());

		tableModel.clear();
		assertTrue(selectionModel.empty().get());
		assertNull(selectionModel.item().get());

		selectionModel.clearSelection();
		selectionModel.index().removeConsumer(consumer);
		selectionModel.indexes().removeListener(listener);
		selectionModel.item().removeConsumer(consumer);
		selectionModel.items().removeConsumer(consumer);
	}

	@Test
	void selectionAndFiltering() {
		tableModel.refresh();
		tableModel.selection().indexes().add(singletonList(3));
		assertEquals(3, tableModel.selection().getMinSelectionIndex());

		tableModel.filterModel().conditionModel(0).operands().equal().set("d");
		assertEquals(0, tableModel.selection().getMinSelectionIndex());
		assertEquals(singletonList(0), tableModel.selection().indexes().get());
		tableModel.filterModel().conditionModel(0).enabled().set(false);
		assertEquals(0, tableModel.selection().getMinSelectionIndex());
		assertEquals(ITEMS.get(3), tableModel.selection().item().get());
	}

	@Test
	void visiblePredicate() {
		tableModel.refresh();
		tableModel.items().visiblePredicate().set(item -> false);
		assertEquals(0, tableModel.items().visible().size());
	}

	@Test
	void columns() {
		assertEquals(1, tableModel.getColumnCount());
	}

	@Test
	void filterAndRemove() {
		tableModel.refresh();
		tableModel.filterModel().conditionModel(0).operands().equal().set("a");
		assertTrue(tableModel.items().contains(B));
		tableModel.removeItem(B);
		assertFalse(tableModel.items().contains(B));
		tableModel.removeItem(A);
		assertFalse(tableModel.items().contains(A));
	}

	@Test
	void filtering() {
		tableModel.refresh();
		assertTrue(tableModelContainsAll(ITEMS, false, tableModel));
		assertTrue(tableModel.items().visiblePredicate().isNull());

		//test filters
		assertNotNull(tableModel.filterModel().conditionModel(0));
		assertTrue(tableModel.items().visible().contains(B));
		tableModel.filterModel().conditionModel(0).operands().equal().set("a");
		assertTrue(tableModel.items().visible().contains(A));
		assertFalse(tableModel.items().visible().contains(B));
		assertTrue(tableModel.items().filtered().contains(D));

		tableModel.items().visiblePredicate().set(strings -> !strings.equals(A));
		assertTrue(tableModel.items().visiblePredicate().isNotNull());
		assertFalse(tableModel.items().visible().contains(A));
		tableModel.items().visiblePredicate().clear();
		assertTrue(tableModel.items().visible().contains(A));

		assertFalse(tableModel.items().visible().contains(B));
		assertTrue(tableModel.items().contains(B));
		assertTrue(tableModel.filterModel().conditionModel(0).enabled().get());
		assertEquals(4, tableModel.items().filtered().size());
		assertFalse(tableModelContainsAll(ITEMS, false, tableModel));
		assertTrue(tableModelContainsAll(ITEMS, true, tableModel));

		assertFalse(tableModel.items().visible().get().isEmpty());
		assertFalse(tableModel.items().filtered().get().isEmpty());
		assertFalse(tableModel.items().get().isEmpty());

		tableModel.filterModel().conditionModel(0).enabled().set(false);
		assertFalse(tableModel.filterModel().conditionModel(0).enabled().get());

		assertTrue(tableModelContainsAll(ITEMS, false, tableModel));

		tableModel.filterModel().conditionModel(0).operands().equal().set("t"); // ekki til
		assertTrue(tableModel.filterModel().conditionModel(0).enabled().get());
		assertEquals(5, tableModel.items().filtered().size());
		assertFalse(tableModelContainsAll(ITEMS, false, tableModel));
		assertTrue(tableModelContainsAll(ITEMS, true, tableModel));
		tableModel.filterModel().conditionModel(0).enabled().set(false);
		assertTrue(tableModelContainsAll(ITEMS, false, tableModel));
		assertFalse(tableModel.filterModel().conditionModel(0).enabled().get());

		tableModel.filterModel().conditionModel(0).operands().equal().set("b");
		int rowCount = tableModel.items().visible().size();
		tableModel.addItemsAt(0, singletonList(new TestRow("x")));
		assertEquals(rowCount, tableModel.items().visible().size());

		assertThrows(IllegalArgumentException.class, () -> tableModel.filterModel().conditionModel(1));
	}

	@Test
	void clearFilterModels() {
		assertFalse(tableModel.filterModel().enabled(0).get());
		tableModel.filterModel().conditionModel(0).operands().equal().set("SCOTT");
		assertTrue(tableModel.filterModel().enabled(0).get());
		tableModel.filterModel().clear();
		assertFalse(tableModel.filterModel().enabled(0).get());
	}

	@Test
	void values() {
		tableModel.refresh();
		tableModel.selection().indexes().set(asList(0, 2));
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
																							 FilterTableModel<TestRow, Integer> model) {
		for (TestRow row : rows) {
			if (includeFiltered) {
				if (!model.items().contains(row)) {
					return false;
				}
			}
			else if (!model.items().visible().contains(row)) {
				return false;
			}
		}

		return true;
	}
}
