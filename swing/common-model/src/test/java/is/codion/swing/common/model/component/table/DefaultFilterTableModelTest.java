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

import is.codion.common.model.filter.FilterModel.Items;
import is.codion.common.state.State;
import is.codion.swing.common.model.component.list.FilterListSelection;
import is.codion.swing.common.model.component.table.FilterTableModel.TableColumns;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

		@Override
		public boolean equals(Object object) {
			if (object == null || getClass() != object.getClass()) {
				return false;
			}
			TestRow testRow = (TestRow) object;
			return Objects.equals(value, testRow.value);
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(value);
		}

		@Override
		public String toString() {
			return value;
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
		return FilterTableModel.builder()
						.columns(new TestColumns())
						.supplier(() -> ITEMS)
						.build();
	}

	@BeforeEach
	void setUp() {
		tableModel = createTestModel();
	}

	@Test
	void nonUniqueColumnIdentifiers() {
		assertThrows(IllegalArgumentException.class, () -> FilterTableModel.builder()
						.columns(new TableColumns<Object, Object>() {
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
		assertThrows(NullPointerException.class, () -> FilterTableModel.<String, Integer>builder().columns(null));
	}

	@Test
	void noColumns() {
		assertThrows(IllegalArgumentException.class, () ->
						FilterTableModel.builder().columns(new TableColumns<String, Integer>() {
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
		tableModel.items().refresh();
		assertEquals(1, done.get());

		tableModel.items().refresher().result().removeListener(onResult);
		tableModel.items().refresher().exception().removeConsumer(onException);
	}

	@Test
	void refreshWithClearStrategyPreservesSelectionWhenItemsRemain() {
		AtomicInteger emptySelectionEvents = new AtomicInteger();
		AtomicInteger selectionChangeEvents = new AtomicInteger();

		List<TestRow> items = new ArrayList<>(ITEMS);
		FilterTableModel<TestRow, Integer> testModel =
						FilterTableModel.builder()
										.columns(new TestColumns())
										.supplier(() -> items)
										.build();

		testModel.selection().empty().addConsumer(empty -> {
			if (empty) {
				emptySelectionEvents.incrementAndGet();
			}
		});

		testModel.selection().indexes().addListener(selectionChangeEvents::incrementAndGet);

		testModel.items().refresh();
		testModel.selection().items().set(asList(B, D));

		assertEquals(1, selectionChangeEvents.get());
		assertEquals(0, emptySelectionEvents.get());
		assertEquals(asList(B, D), testModel.selection().items().get());

		// Test 1: Refresh with same data - should preserve selection without empty event
		testModel.items().refresh();
		
		assertEquals(asList(B, D), testModel.selection().items().get());
		assertEquals(0, emptySelectionEvents.get()); // No empty selection event!
		// Note: We may get an extra selection event due to the surgical update process,
		// but the important thing is that selection is preserved without empty events
		assertTrue(selectionChangeEvents.get() <= 2);

		// Test 2: Refresh with partial data - only D remains, selection should update but not go empty
		items.clear();
		items.addAll(asList(A, D, E)); // B is removed, D remains
		testModel.items().refresh();

		assertEquals(asList(D), testModel.selection().items().get());
		assertEquals(0, emptySelectionEvents.get()); // Still no empty event!
		assertTrue(selectionChangeEvents.get() >= 2); // Selection changed from [B,D] to [D]

		// Test 3: Refresh removing all selected items - should trigger empty selection
		int changeEventsBefore = selectionChangeEvents.get();
		items.clear();
		items.addAll(asList(A, B, C)); // D is removed
		testModel.items().refresh();

		assertTrue(testModel.selection().empty().is());
		assertEquals(1, emptySelectionEvents.get()); // Now we get empty event
		assertTrue(selectionChangeEvents.get() > changeEventsBefore); // Selection cleared

		// Test 4: Select multiple items and refresh with reordered data
		testModel.selection().items().set(asList(A, C));
		assertEquals(1, emptySelectionEvents.get()); // Still just one

		items.clear();
		items.addAll(asList(C, B, A)); // Same items, different order
		testModel.items().refresh();

		// Selection should be preserved even with different order
		assertEquals(2, testModel.selection().items().get().size());
		assertTrue(testModel.selection().items().get().contains(A));
		assertTrue(testModel.selection().items().get().contains(C));
		assertEquals(1, emptySelectionEvents.get()); // No new empty event

		// Test 5: With sorting enabled
		testModel.sort().ascending(0);
		testModel.selection().items().set(asList(A, B));

		items.clear();
		items.addAll(asList(E, D, C, B, A)); // Reverse order
		testModel.items().refresh();

		// After sort, A and B should still be selected
		assertEquals(asList(A, B), testModel.selection().items().get());
		assertEquals(1, emptySelectionEvents.get()); // Still no new empty event
	}

	@Test
	void addItems() {
		tableModel.sort().ascending(0);
		Items<TestRow> items = tableModel.items();
		items.add(asList(A, B));
		// sorts
		assertTrue(items.visible().add(0, C));
		assertEquals(2, items.visible().indexOf(C));

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

		// sorts
		assertTrue(items.visible().add(0, asList(D, E)));
		assertEquals(3, items.visible().indexOf(D));
		assertEquals(4, items.visible().indexOf(E));

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
		tableModel.items().refresh();
		assertEquals(12, events.get());
		tableModel.items().remove(asList(B, D, E, G));//does not contain G
		assertEquals(13, events.get());//just a single event when removing multiple items
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
		assertTrue(selectionChangedState.is());
		tableModel.items().visible().set(tableModel.items().visible().indexOf(h), B);
		assertEquals(3, dataChangedEvents.get());

		selectionChangedState.set(false);
		TestRow newB = new TestRow("b");
		tableModel.items().visible().set(tableModel.items().visible().indexOf(B), newB);
		assertFalse(selectionChangedState.is());
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

		events.set(0);
		tableModel.items().refresh();
		assertEquals(2, events.get());

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
	void selectionBoundsValidation() {
		// Single index bounds checking
		assertThrows(IndexOutOfBoundsException.class, () -> tableModel.selection().index().set(-1));
		assertThrows(IndexOutOfBoundsException.class, () -> tableModel.selection().index().set(10));
		assertThrows(IndexOutOfBoundsException.class, () -> tableModel.selection().indexes().add(-1));
		assertThrows(IndexOutOfBoundsException.class, () -> tableModel.selection().indexes().add(10));

		// Multiple indexes bounds checking
		List<Integer> negativeIndexes = Arrays.asList(1, -1);
		List<Integer> outOfBoundsIndexes = Arrays.asList(1, 10);

		assertThrows(IndexOutOfBoundsException.class, () -> tableModel.selection().indexes().add(negativeIndexes));
		assertThrows(IndexOutOfBoundsException.class, () -> tableModel.selection().indexes().add(outOfBoundsIndexes));
		assertThrows(IndexOutOfBoundsException.class, () -> tableModel.selection().indexes().set(negativeIndexes));
		assertThrows(IndexOutOfBoundsException.class, () -> tableModel.selection().indexes().set(outOfBoundsIndexes));
	}

	@Test
	void selection() {
		AtomicInteger events = new AtomicInteger();
		Runnable listener = events::incrementAndGet;
		Consumer consumer = data -> listener.run();
		FilterListSelection<TestRow> selection = tableModel.selection();
		selection.index().addConsumer(consumer);
		selection.indexes().addListener(listener);
		selection.item().addConsumer(consumer);
		selection.items().addConsumer(consumer);

		assertFalse(selection.single().is());
		assertTrue(selection.empty().is());
		assertFalse(selection.multiple().is());

		tableModel.items().refresh();
		selection.index().set(2);
		assertEquals(4, events.get());
		assertTrue(selection.single().is());
		assertFalse(selection.empty().is());
		assertFalse(selection.multiple().is());
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
		assertFalse(selection.single().is());
		assertTrue(selection.empty().is());
		assertFalse(selection.multiple().is());
		assertEquals(0, selection.items().get().size());

		selection.item().set(ITEMS.get(0));
		assertFalse(selection.multiple().is());
		assertEquals(ITEMS.get(0), selection.item().get());
		assertEquals(0, selection.index().get());
		assertEquals(1, selection.count());
		assertFalse(selection.isSelectionEmpty());
		selection.indexes().add(1);
		assertTrue(selection.multiple().is());
		assertEquals(ITEMS.get(0), selection.item().get());
		assertEquals(asList(0, 1), selection.indexes().get());
		assertEquals(0, selection.index().get());
		selection.indexes().add(4);
		assertTrue(selection.multiple().is());
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
		assertTrue(selection.empty().is());
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
		assertTrue(tableModel.filters().get(0).enabled().is());
		assertEquals(4, tableModel.items().filtered().count());
		assertFalse(tableModelContainsAll(ITEMS, false, tableModel));
		assertTrue(tableModelContainsAll(ITEMS, true, tableModel));

		assertFalse(tableModel.items().visible().get().isEmpty());
		assertFalse(tableModel.items().filtered().get().isEmpty());
		assertFalse(tableModel.items().get().isEmpty());

		tableModel.filters().get(0).enabled().set(false);
		assertFalse(tableModel.filters().get(0).enabled().is());

		assertTrue(tableModelContainsAll(ITEMS, false, tableModel));

		tableModel.filters().get(0).operands().equal().set("t"); // ekki til
		assertTrue(tableModel.filters().get(0).enabled().is());
		assertEquals(5, tableModel.items().filtered().count());
		assertFalse(tableModelContainsAll(ITEMS, false, tableModel));
		assertTrue(tableModelContainsAll(ITEMS, true, tableModel));
		tableModel.filters().get(0).enabled().set(false);
		assertTrue(tableModelContainsAll(ITEMS, false, tableModel));
		assertFalse(tableModel.filters().get(0).enabled().is());

		tableModel.filters().get(0).operands().equal().set("b");
		int rowCount = tableModel.items().visible().count();
		tableModel.items().visible().add(0, singletonList(new TestRow("x")));
		assertEquals(rowCount, tableModel.items().visible().count());

		assertThrows(IllegalArgumentException.class, () -> tableModel.filters().get(1));
	}

	@Test
	void clearFilterModels() {
		assertFalse(tableModel.filters().get(0).enabled().is());
		tableModel.filters().get(0).operands().equal().set("SCOTT");
		assertTrue(tableModel.filters().get(0).enabled().is());
		tableModel.filters().clear();
		assertFalse(tableModel.filters().get(0).enabled().is());
	}

	@Test
	void values() {
		tableModel.items().refresh();
		tableModel.selection().indexes().set(asList(0, 2));
		List<String> values = tableModel.values().selected(0);
		assertEquals(2, values.size());
		assertEquals(0, values.indexOf("a"));
		assertEquals(1, values.indexOf("c"));

		values = tableModel.values().get(0);
		assertEquals(5, values.size());
		assertEquals(0, values.indexOf("a"));
		assertEquals(1, values.indexOf("b"));
		assertEquals(2, values.indexOf("c"));
		assertEquals(3, values.indexOf("d"));
		assertEquals(4, values.indexOf("e"));
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

		assertThrows(NullPointerException.class, () -> tableModel.items().visible().add(0, (TestRow) null));
		assertThrows(NullPointerException.class, () -> tableModel.items().visible().add(0, (List<TestRow>) null));
		assertThrows(NullPointerException.class, () -> tableModel.items().visible().add(0, singleNull));
		assertThrows(NullPointerException.class, () -> tableModel.items().visible().contains(null));
		assertThrows(NullPointerException.class, () -> tableModel.items().visible().indexOf(null));
		tableModel.items().add(new TestRow("test"));
		assertThrows(NullPointerException.class, () -> tableModel.items().visible().set(0, null));

		assertThrows(NullPointerException.class, () -> tableModel.items().filtered().contains(null));

		FilterListSelection<TestRow> selection = tableModel.selection();
		assertThrows(NullPointerException.class, () -> selection.items().remove((TestRow) null));
		assertThrows(NullPointerException.class, () -> selection.items().remove((Collection<TestRow>) null));
		assertThrows(NullPointerException.class, () -> selection.items().remove(singleNull));
		assertThrows(NullPointerException.class, () -> selection.items().set(singleNull));
		assertThrows(NullPointerException.class, () -> selection.items().add(singleNull));
		assertThrows(NullPointerException.class, () -> selection.items().contains(null));
	}

	@Test
	void replace() {
		tableModel.sort().ascending(0);
		tableModel.items().refresh();
		//a, b, c, d, e
		//replace d with f
		tableModel.items().replace(D, F);
		assertTrue(tableModel.items().contains(F));
		assertTrue(tableModel.items().visible().contains(F));
		assertEquals(4, tableModel.items().visible().indexOf(F));
		assertFalse(tableModel.items().contains(D));
		assertFalse(tableModel.items().visible().contains(D));
		//filter f and replace with d
		tableModel.items().visible().predicate().set(testRow -> testRow != F);
		tableModel.items().replace(F, D);
		assertTrue(tableModel.items().contains(D));
		assertTrue(tableModel.items().visible().contains(D));
		assertEquals(3, tableModel.items().visible().indexOf(D));
		assertFalse(tableModel.items().contains(F));
		assertFalse(tableModel.items().filtered().contains(F));
		//replace d with f
		tableModel.items().replace(D, F);
		assertFalse(tableModel.items().contains(D));
		assertFalse(tableModel.items().visible().contains(D));
		assertTrue(tableModel.items().contains(F));
		assertTrue(tableModel.items().filtered().contains(F));
		//filter both d and f and replace f with d
		tableModel.items().visible().predicate().set(testRow -> testRow != F && testRow != D);
		tableModel.items().replace(F, D);
		assertFalse(tableModel.items().contains(F));
		assertFalse(tableModel.items().filtered().contains(F));
		assertTrue(tableModel.items().contains(D));
		assertTrue(tableModel.items().filtered().contains(D));

		tableModel.items().refresh();
		//a, b, c, d, e
		//replace d with f and b with g
		Map<TestRow, TestRow> replacements = new HashMap<>();
		replacements.put(D, F);
		replacements.put(B, G);

		tableModel.items().replace(replacements);
		assertFalse(tableModel.items().contains(D));
		assertTrue(tableModel.items().contains(F));
		assertFalse(tableModel.items().contains(B));
		assertTrue(tableModel.items().contains(G));

		//filter f and b and replace f and g with d an b
		tableModel.items().visible().predicate().set(testRow -> testRow != F && testRow != B);
		replacements.clear();
		replacements.put(F, D);
		replacements.put(G, B);

		tableModel.items().replace(replacements);
		assertTrue(tableModel.items().contains(D));
		assertTrue(tableModel.items().visible().contains(D));
		assertFalse(tableModel.items().contains(F));
		assertTrue(tableModel.items().contains(B));
		assertTrue(tableModel.items().filtered().contains(B));
		assertFalse(tableModel.items().contains(G));
	}

	@Test
	void editor() {
		class ItemEditor implements FilterTableModel.Editor<TestRow, Integer> {

			private final Items<TestRow> items;

			private ItemEditor(FilterTableModel<TestRow, Integer> model) {
				items = model.items();
			}

			@Override
			public boolean editable(TestRow row, Integer identifier) {
				return true;
			}

			@Override
			public void set(Object value, int rowIndex, TestRow row, Integer identifier) {
				items.replace(row, new TestRow((String) value));
			}
		}
		FilterTableModel<TestRow, Integer> model = FilterTableModel.builder()
						.columns(new TestColumns())
						.supplier(() -> ITEMS)
						.editor(ItemEditor::new)
						.build();

		model.items().refresh();
		model.sort().ascending(0);

		assertTrue(model.items().contains(A));
		assertFalse(model.items().contains(G));
		assertTrue(model.isCellEditable(0, 0));
		model.setValueAt("g", 0, 0);
		assertFalse(model.items().contains(A));
		assertTrue(model.items().contains(G));
		assertEquals(4, model.items().visible().indexOf(G));
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
