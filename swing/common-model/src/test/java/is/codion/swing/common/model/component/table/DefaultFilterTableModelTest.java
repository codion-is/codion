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
package is.codion.swing.common.model.component.table;

import is.codion.common.model.FilterModel.Items;
import is.codion.common.state.State;
import is.codion.swing.common.model.component.table.FilterTableModel.RefreshStrategy;
import is.codion.swing.common.model.component.table.FilterTableModel.TableColumns;
import is.codion.swing.common.model.component.table.FilterTableModel.TableSelection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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

	private static final class TestColumns implements TableColumns<TestRow, Integer> {
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
						.supplier(() -> ITEMS)
						.build();
	}

	@BeforeEach
	void setUp() {
		tableModel = createTestModel();
	}

	@Test
	void nonUniqueColumnIdentifiers() {
		assertThrows(IllegalArgumentException.class, () -> FilterTableModel.builder(new TableColumns<Object, Object>() {
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
		tableModel.items().refresh();
		tableModel.items().visible().predicate().set(item -> !item.equals(B) && !item.equals(F));
		assertFalse(tableModel.items().visible().contains(B));
		assertTrue(tableModel.items().contains(B));
		assertFalse(tableModel.items().visible().add(0, Collections.singletonList(F)));
		assertFalse(tableModel.items().visible().contains(F));
		assertTrue(tableModel.items().contains(F));
		tableModel.items().visible().predicate().clear();
		assertTrue(tableModel.items().visible().contains(B));
		assertTrue(tableModel.items().visible().contains(F));
	}

	@Test
	void conditionModel() {
		tableModel.items().refresh();
		assertEquals(5, tableModel.items().visible().count());
		tableModel.filters().get(0).operands().equal().set("a");
		assertEquals(1, tableModel.items().visible().count());
		tableModel.filters().get(0).operands().equal().set("b");
		assertEquals(1, tableModel.items().visible().count());
		tableModel.filters().get(0).clear();
	}

	@Test
	void addItemsAt() {
		tableModel.items().refresh();
		assertTrue(tableModel.items().visible().add(2, asList(F, G)));
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
						FilterTableModel.<String, Integer>builder(new TableColumns<String, Integer>() {
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
		Runnable onResult = done::incrementAndGet;
		Consumer<Throwable> onException = exception -> {};
		tableModel.items().refresher().result().addListener(onResult);
		tableModel.items().refresher().exception().addConsumer(onException);
		tableModel.items().refresh();
		assertFalse(tableModel.items().visible().get().isEmpty());
		assertEquals(1, done.get());

		done.set(0);
		tableModel.items().refreshStrategy().set(RefreshStrategy.MERGE);
		tableModel.items().refresh();
		assertEquals(1, done.get());

		tableModel.items().refresher().result().removeListener(onResult);
		tableModel.items().refresher().exception().removeConsumer(onException);
	}

	@Test
	void mergeOnRefresh() {
		AtomicInteger selectionEvents = new AtomicInteger();
		List<TestRow> items = new ArrayList<>(ITEMS);
		FilterTableModel<TestRow, Integer> testModel =
						FilterTableModel.<TestRow, Integer>builder(new TestColumns())
										.supplier(() -> items)
										.build();
		testModel.selection().indexes().addListener(selectionEvents::incrementAndGet);
		testModel.items().refreshStrategy().set(RefreshStrategy.MERGE);
		testModel.sort().ascending(0);
		testModel.items().refresh();
		testModel.selection().index().set(1);//b

		assertEquals(1, selectionEvents.get());
		assertSame(B, testModel.selection().item().get());

		assertTrue(items.remove(C));
		testModel.items().refresh();
		assertEquals(1, selectionEvents.get());

		assertTrue(items.remove(B));
		testModel.items().refresh();
		assertTrue(testModel.selection().isSelectionEmpty());
		assertEquals(2, selectionEvents.get());

		testModel.selection().item().set(E);
		assertEquals(3, selectionEvents.get());

		testModel.items().visible().predicate().set(item -> !item.equals(E));
		assertEquals(4, selectionEvents.get());

		assertTrue(items.add(B));

		testModel.items().refresh();
		//merge does not sort new items
		testModel.items().visible().sort();

		testModel.selection().index().set(1);//b

		assertEquals(5, selectionEvents.get());
		assertSame(B, testModel.selection().item().get());
	}

	@Test
	void addItems() {
		tableModel.sort().ascending(0);
		Items<TestRow> items = tableModel.items();
		items.add(asList(A, B));
		// does not sort
		assertTrue(items.visible().add(0, C));
		assertEquals(0, items.visible().indexOf(C));

		items.remove(C);
		// sorts
		items.add(C);
		assertEquals(2, items.visible().indexOf(C));
		items.visible().predicate().set(item -> !item.equals(C));
		// not visible when removed
		items.remove(C);

		items.add(C);
		items.visible().predicate().clear();
		assertEquals(2, items.visible().indexOf(C));

		// does not sort
		assertTrue(items.visible().add(0, asList(D, E)));
		assertEquals(0, items.visible().indexOf(D));
		assertEquals(1, items.visible().indexOf(E));

		items.visible().predicate().set(item -> !item.equals(E));
		assertEquals(3, items.visible().indexOf(D));
		assertEquals(-1, items.visible().indexOf(E));

		// not visible when removed
		items.remove(E);

		items.visible().predicate().clear();
		assertEquals(asList(A, B, C, D), items.visible().get());

		items.set(asList(B, A, D, C));
		assertEquals(asList(A, B, C, D), items.visible().get());
	}

	@Test
	void removeItems() {
		AtomicInteger events = new AtomicInteger();
		Runnable listener = events::incrementAndGet;
		tableModel.items().visible().addListener(listener);
		tableModel.items().refresh();
		assertEquals(1, events.get());
		tableModel.filters().get(0).operands().equal().set("a");
		tableModel.items().remove(B);
		assertEquals(3, events.get());
		assertFalse(tableModel.items().visible().contains(B));
		assertTrue(tableModel.items().contains(A));
		tableModel.items().remove(A);
		assertEquals(4, events.get());
		assertFalse(tableModel.items().contains(A));
		tableModel.items().remove(asList(D, E));
		assertEquals(4, events.get());//no change event when removing filtered items
		assertFalse(tableModel.items().visible().contains(D));
		assertFalse(tableModel.items().visible().contains(E));
		assertFalse(tableModel.items().filtered().contains(D));
		assertFalse(tableModel.items().filtered().contains(E));
		tableModel.filters().get(0).operands().equal().set(null);
		tableModel.items().refresh();//two events, clear and add
		assertEquals(8, events.get());
		tableModel.items().visible().remove(0, 2);
		assertEquals(9, events.get());//just a single event when removing multiple items
		tableModel.items().visible().remove(0);
		assertEquals(10, events.get());
		tableModel.items().visible().removeListener(listener);
	}

	@Test
	void setItemAt() {
		AtomicInteger dataChangedEvents = new AtomicInteger();
		Runnable listener = dataChangedEvents::incrementAndGet;
		tableModel.items().visible().addListener(listener);
		State selectionChangedState = State.state();
		tableModel.selection().item().addConsumer(item -> selectionChangedState.set(true));
		tableModel.items().refresh();
		assertEquals(1, dataChangedEvents.get());
		tableModel.selection().item().set(B);
		TestRow h = new TestRow("h");
		tableModel.items().visible().set(tableModel.items().visible().indexOf(B), h);
		assertEquals(2, dataChangedEvents.get());
		assertEquals(h, tableModel.selection().item().get());
		assertTrue(selectionChangedState.get());
		tableModel.items().visible().set(tableModel.items().visible().indexOf(h), B);
		assertEquals(3, dataChangedEvents.get());

		selectionChangedState.set(false);
		TestRow newB = new TestRow("b");
		tableModel.items().visible().set(tableModel.items().visible().indexOf(B), newB);
		assertFalse(selectionChangedState.get());
		assertEquals(newB, tableModel.selection().item().get());
		tableModel.items().visible().removeListener(listener);
	}

	@Test
	void removeItemsRange() {
		AtomicInteger events = new AtomicInteger();
		Runnable listener = events::incrementAndGet;
		tableModel.items().visible().addListener(listener);
		tableModel.items().refresh();
		assertEquals(1, events.get());
		List<TestRow> removed = tableModel.items().visible().remove(1, 3);
		assertEquals(2, events.get());
		assertTrue(tableModel.items().contains(A));
		assertFalse(tableModel.items().contains(B));
		assertTrue(removed.contains(B));
		assertFalse(tableModel.items().contains(C));
		assertTrue(removed.contains(C));
		assertTrue(tableModel.items().contains(D));
		assertTrue(tableModel.items().contains(E));

		tableModel.items().refreshStrategy().set(RefreshStrategy.MERGE);
		events.set(0);
		tableModel.items().refresh();
		assertEquals(5, events.get());

		tableModel.items().visible().removeListener(listener);
	}

	@Test
	void clear() {
		tableModel.items().refresh();
		assertTrue(tableModel.items().visible().count() > 0);
		tableModel.items().clear();
		assertEquals(0, tableModel.items().visible().count());
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
		TableSelection<TestRow> selection = tableModel.selection();
		selection.index().addConsumer(consumer);
		selection.indexes().addListener(listener);
		selection.item().addConsumer(consumer);
		selection.items().addConsumer(consumer);

		assertFalse(selection.single().get());
		assertTrue(selection.empty().get());
		assertFalse(selection.multiple().get());

		tableModel.items().refresh();
		selection.index().set(2);
		assertEquals(4, events.get());
		assertTrue(selection.single().get());
		assertFalse(selection.empty().get());
		assertFalse(selection.multiple().get());
		assertEquals(2, selection.index().get());
		selection.indexes().increment();
		assertEquals(8, events.get());
		assertEquals(3, selection.index().get());
		selection.indexes().decrement();
		selection.indexes().decrement();
		assertEquals(1, selection.index().get());

		selection.indexes().increment();
		selection.indexes().increment();

		assertEquals(3, selection.index().get());

		selection.index().set(0);
		selection.indexes().decrement();
		assertEquals(tableModel.items().visible().count() - 1, selection.index().get());

		selection.index().set(tableModel.items().visible().count() - 1);
		selection.indexes().increment();
		assertEquals(0, selection.index().get());

		selection.clearSelection();
		selection.indexes().decrement();
		assertEquals(tableModel.items().visible().count() - 1, selection.index().get());

		selection.clearSelection();
		selection.indexes().increment();
		assertEquals(0, selection.index().get());

		selection.selectAll();
		assertEquals(5, selection.items().get().size());
		selection.clearSelection();
		assertFalse(selection.single().get());
		assertTrue(selection.empty().get());
		assertFalse(selection.multiple().get());
		assertEquals(0, selection.items().get().size());

		selection.item().set(ITEMS.get(0));
		assertFalse(selection.multiple().get());
		assertEquals(ITEMS.get(0), selection.item().get());
		assertEquals(0, selection.index().get());
		assertEquals(1, selection.count());
		assertFalse(selection.isSelectionEmpty());
		selection.indexes().add(1);
		assertTrue(selection.multiple().get());
		assertEquals(ITEMS.get(0), selection.item().get());
		assertEquals(asList(0, 1), selection.indexes().get());
		assertEquals(0, selection.index().get());
		selection.indexes().add(4);
		assertTrue(selection.multiple().get());
		assertEquals(asList(0, 1, 4), selection.indexes().get());
		selection.removeIndexInterval(1, 4);
		assertEquals(singletonList(0), selection.indexes().get());
		assertEquals(0, selection.index().get());
		selection.clearSelection();
		assertEquals(new ArrayList<Integer>(), selection.indexes().get());
		assertEquals(-1, selection.getMinSelectionIndex());
		selection.indexes().add(asList(0, 3, 4));
		assertEquals(asList(0, 3, 4), selection.indexes().get());
		assertEquals(0, selection.getMinSelectionIndex());
		assertEquals(3, selection.count());
		selection.removeSelectionInterval(0, 0);
		assertEquals(3, selection.getMinSelectionIndex());
		selection.removeSelectionInterval(3, 3);
		assertEquals(4, selection.getMinSelectionIndex());

		selection.indexes().add(asList(0, 3, 4));
		assertEquals(0, selection.getMinSelectionIndex());
		selection.removeSelectionInterval(3, 3);
		assertEquals(0, selection.getMinSelectionIndex());
		selection.clearSelection();
		assertEquals(-1, selection.getMinSelectionIndex());

		selection.indexes().add(asList(0, 1, 2, 3, 4));
		assertEquals(0, selection.getMinSelectionIndex());
		selection.removeSelectionInterval(0, 0);
		assertEquals(1, selection.getMinSelectionIndex());
		selection.indexes().remove(1);
		assertEquals(2, selection.getMinSelectionIndex());
		selection.indexes().remove(singletonList(2));
		assertEquals(3, selection.getMinSelectionIndex());
		selection.removeSelectionInterval(3, 3);
		assertEquals(4, selection.getMinSelectionIndex());
		selection.indexes().remove(4);
		assertEquals(-1, selection.getMinSelectionIndex());

		selection.items().add(ITEMS.get(0));
		assertEquals(1, selection.count());
		assertEquals(0, selection.getMinSelectionIndex());
		selection.items().add(asList(ITEMS.get(1), ITEMS.get(2)));
		assertEquals(3, selection.count());
		assertEquals(0, selection.getMinSelectionIndex());
		selection.items().remove(ITEMS.get(1));
		assertEquals(2, selection.count());
		assertEquals(0, selection.getMinSelectionIndex());
		selection.items().remove(ITEMS.get(2));
		assertEquals(1, selection.count());
		assertEquals(0, selection.getMinSelectionIndex());
		selection.items().add(asList(ITEMS.get(1), ITEMS.get(2)));
		assertEquals(3, selection.count());
		assertEquals(0, selection.getMinSelectionIndex());
		selection.items().add(ITEMS.get(4));
		assertEquals(4, selection.count());
		assertEquals(0, selection.getMinSelectionIndex());
		tableModel.items().remove(ITEMS.get(0));
		assertEquals(3, selection.count());
		assertEquals(0, selection.getMinSelectionIndex());

		tableModel.items().clear();
		assertTrue(selection.empty().get());
		assertNull(selection.item().get());

		selection.clearSelection();
		selection.index().removeConsumer(consumer);
		selection.indexes().removeListener(listener);
		selection.item().removeConsumer(consumer);
		selection.items().removeConsumer(consumer);
	}

	@Test
	void selectionAndFiltering() {
		tableModel.items().refresh();
		tableModel.selection().indexes().add(singletonList(3));
		assertEquals(3, tableModel.selection().getMinSelectionIndex());

		tableModel.filters().get(0).operands().equal().set("d");
		assertEquals(0, tableModel.selection().getMinSelectionIndex());
		assertEquals(singletonList(0), tableModel.selection().indexes().get());
		tableModel.filters().get(0).enabled().set(false);
		assertEquals(0, tableModel.selection().getMinSelectionIndex());
		assertEquals(ITEMS.get(3), tableModel.selection().item().get());
	}

	@Test
	void visiblePredicate() {
		tableModel.items().refresh();
		tableModel.items().visible().predicate().set(item -> false);
		assertEquals(0, tableModel.items().visible().count());
	}

	@Test
	void columns() {
		assertEquals(1, tableModel.getColumnCount());
	}

	@Test
	void filterAndRemove() {
		tableModel.items().refresh();
		tableModel.filters().get(0).operands().equal().set("a");
		assertTrue(tableModel.items().contains(B));
		tableModel.items().remove(B);
		assertFalse(tableModel.items().contains(B));
		tableModel.items().remove(A);
		assertFalse(tableModel.items().contains(A));
	}

	@Test
	void filtering() {
		tableModel.items().refresh();
		assertTrue(tableModelContainsAll(ITEMS, false, tableModel));
		assertTrue(tableModel.items().visible().predicate().isNull());

		//test filters
		assertNotNull(tableModel.filters().get(0));
		assertTrue(tableModel.items().visible().contains(B));
		tableModel.filters().get(0).operands().equal().set("a");
		assertTrue(tableModel.items().visible().contains(A));
		assertFalse(tableModel.items().visible().contains(B));
		assertTrue(tableModel.items().filtered().contains(D));

		tableModel.items().visible().predicate().set(strings -> !strings.equals(A));
		assertFalse(tableModel.items().visible().predicate().isNull());
		assertFalse(tableModel.items().visible().contains(A));
		tableModel.items().visible().predicate().clear();
		assertTrue(tableModel.items().visible().contains(A));

		assertFalse(tableModel.items().visible().contains(B));
		assertTrue(tableModel.items().contains(B));
		assertTrue(tableModel.filters().get(0).enabled().get());
		assertEquals(4, tableModel.items().filtered().count());
		assertFalse(tableModelContainsAll(ITEMS, false, tableModel));
		assertTrue(tableModelContainsAll(ITEMS, true, tableModel));

		assertFalse(tableModel.items().visible().get().isEmpty());
		assertFalse(tableModel.items().filtered().get().isEmpty());
		assertFalse(tableModel.items().get().isEmpty());

		tableModel.filters().get(0).enabled().set(false);
		assertFalse(tableModel.filters().get(0).enabled().get());

		assertTrue(tableModelContainsAll(ITEMS, false, tableModel));

		tableModel.filters().get(0).operands().equal().set("t"); // ekki til
		assertTrue(tableModel.filters().get(0).enabled().get());
		assertEquals(5, tableModel.items().filtered().count());
		assertFalse(tableModelContainsAll(ITEMS, false, tableModel));
		assertTrue(tableModelContainsAll(ITEMS, true, tableModel));
		tableModel.filters().get(0).enabled().set(false);
		assertTrue(tableModelContainsAll(ITEMS, false, tableModel));
		assertFalse(tableModel.filters().get(0).enabled().get());

		tableModel.filters().get(0).operands().equal().set("b");
		int rowCount = tableModel.items().visible().count();
		tableModel.items().visible().add(0, singletonList(new TestRow("x")));
		assertEquals(rowCount, tableModel.items().visible().count());

		assertThrows(IllegalArgumentException.class, () -> tableModel.filters().get(1));
	}

	@Test
	void clearFilterModels() {
		assertFalse(tableModel.filters().get(0).enabled().get());
		tableModel.filters().get(0).operands().equal().set("SCOTT");
		assertTrue(tableModel.filters().get(0).enabled().get());
		tableModel.filters().clear();
		assertFalse(tableModel.filters().get(0).enabled().get());
	}

	@Test
	void values() {
		tableModel.items().refresh();
		tableModel.selection().indexes().set(asList(0, 2));
		Collection<String> values = tableModel.values().selected(0);
		assertEquals(2, values.size());
		assertTrue(values.contains("a"));
		assertTrue(values.contains("c"));

		values = tableModel.values().get(0);
		assertEquals(5, values.size());
		assertTrue(values.contains("a"));
		assertTrue(values.contains("b"));
		assertTrue(values.contains("c"));
		assertTrue(values.contains("d"));
		assertTrue(values.contains("e"));
		assertFalse(values.contains("zz"));

		// Unknown identifier
		assertThrows(IllegalArgumentException.class, () -> tableModel.values().get(1));
		assertThrows(IllegalArgumentException.class, () -> tableModel.values().selected(1));
	}

	@Test
	void getColumnClass() {
		assertEquals(String.class, tableModel.getColumnClass(0));
	}

	@Test
	void nullItems() {
		assertThrows(NullPointerException.class, () -> tableModel.items().add((TestRow) null));
		assertThrows(NullPointerException.class, () -> tableModel.items().remove((TestRow) null));
		assertThrows(NullPointerException.class, () -> tableModel.items().add((List<TestRow>) null));
		Set<TestRow> singleNull = singleton(null);
		assertThrows(NullPointerException.class, () -> tableModel.items().add(singleNull));
		assertThrows(NullPointerException.class, () -> tableModel.items().remove(singleNull));
		assertThrows(NullPointerException.class, () -> tableModel.items().contains(null));

		assertThrows(NullPointerException.class, () -> tableModel.items().visible().add(0, (TestRow)null));
		assertThrows(NullPointerException.class, () -> tableModel.items().visible().add(0, (List<TestRow>) null));
		assertThrows(NullPointerException.class, () -> tableModel.items().visible().add(0, singleNull));
		assertThrows(NullPointerException.class, () -> tableModel.items().visible().contains(null));
		assertThrows(NullPointerException.class, () -> tableModel.items().visible().indexOf(null));
		tableModel.items().add(new TestRow("test"));
		assertThrows(NullPointerException.class, () -> tableModel.items().visible().set(0, null));

		assertThrows(NullPointerException.class, () -> tableModel.items().filtered().contains(null));

		TableSelection<TestRow> selection = tableModel.selection();
		assertThrows(NullPointerException.class, () -> selection.items().remove((TestRow) null));
		assertThrows(NullPointerException.class, () -> selection.items().remove((Collection<TestRow>) null));
		assertThrows(NullPointerException.class, () -> selection.items().remove(singleNull));
		assertThrows(NullPointerException.class, () -> selection.items().set(singleNull));
		assertThrows(NullPointerException.class, () -> selection.items().add(singleNull));
		assertThrows(NullPointerException.class, () -> selection.items().contains(null));
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
