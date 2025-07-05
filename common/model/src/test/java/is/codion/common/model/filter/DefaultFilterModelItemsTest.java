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
package is.codion.common.model.filter;

import is.codion.common.model.filter.FilterModel.Items;
import is.codion.common.model.filter.FilterModel.RefreshStrategy;
import is.codion.common.model.filter.FilterModel.Sort;
import is.codion.common.model.filter.FilterModel.VisibleItems;
import is.codion.common.model.filter.FilterModel.VisiblePredicate;
import is.codion.common.model.selection.MultiSelection;
import is.codion.common.observable.Observable;
import is.codion.common.observable.Observer;
import is.codion.common.state.ObservableState;
import is.codion.common.state.State;
import is.codion.common.value.Value;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for DefaultFilterModelItems.
 * Tests filtering, sorting, refresh strategies, and basic operations.
 */
public class DefaultFilterModelItemsTest {

	private static final String ITEM_PREFIX = "item";
	private static final String FILTERED_PREFIX = "filtered";
	private static final String VISIBLE_PREFIX = "visible";

	@Nested
	@DisplayName("Basic operations")
	class BasicOperationsTest {

		private Items<String> items;
		private TestPredicate visiblePredicate;
		private TestMultiSelection selection;

		@BeforeEach
		void setUp() {
			visiblePredicate = new TestPredicate();
			selection = new TestMultiSelection();

			items = Items.builder()
							.<String>refresher(i -> new TestRefresher())
							.selection(visible -> selection)
							.sort(new TestSort())
							.visiblePredicate(visiblePredicate)
							.build();
		}

		@Test
		@DisplayName("Add single item to visible")
		void add_singleItem_shouldAddToVisible() {
			items.add(ITEM_PREFIX + "1");

			assertEquals(1, items.count());
			assertEquals(1, items.visible().count());
			assertEquals(0, items.filtered().count());
			assertTrue(items.contains(ITEM_PREFIX + "1"));
		}

		@Test
		@DisplayName("Add single item to filtered")
		void add_singleItem_shouldAddToFiltered() {
			visiblePredicate.setPredicate(item -> !item.startsWith(ITEM_PREFIX));
			items.add(ITEM_PREFIX + "1");

			assertEquals(1, items.count());
			assertEquals(0, items.visible().count());
			assertEquals(1, items.filtered().count());
			assertTrue(items.contains(ITEM_PREFIX + "1"));
		}

		@Test
		@DisplayName("Add multiple items mixed visibility")
		void add_multipleItems_shouldSplitByVisibility() {
			visiblePredicate.setPredicate(item -> item.startsWith(VISIBLE_PREFIX));
			List<String> itemsToAdd = asList(
							VISIBLE_PREFIX + "1",
							FILTERED_PREFIX + "1",
							VISIBLE_PREFIX + "2",
							FILTERED_PREFIX + "2"
			);

			items.add(itemsToAdd);

			assertEquals(4, items.count());
			assertEquals(2, items.visible().count());
			assertEquals(2, items.filtered().count());
			assertTrue(items.visible().contains(VISIBLE_PREFIX + "1"));
			assertTrue(items.visible().contains(VISIBLE_PREFIX + "2"));
			assertTrue(items.filtered().contains(FILTERED_PREFIX + "1"));
			assertTrue(items.filtered().contains(FILTERED_PREFIX + "2"));
		}

		@Test
		@DisplayName("Remove single item from visible")
		void remove_singleItem_shouldRemoveFromVisible() {
			items.add(ITEM_PREFIX + "1");
			items.remove(ITEM_PREFIX + "1");

			assertEquals(0, items.count());
			assertFalse(items.contains(ITEM_PREFIX + "1"));
		}

		@Test
		@DisplayName("Remove single item from filtered")
		void remove_singleItem_shouldRemoveFromFiltered() {
			visiblePredicate.setPredicate(item -> false);
			items.add(ITEM_PREFIX + "1");

			items.remove(ITEM_PREFIX + "1");

			assertEquals(0, items.count());
			assertFalse(items.contains(ITEM_PREFIX + "1"));
		}

		@Test
		@DisplayName("Replace item in visible")
		void replace_itemInVisible_shouldUpdateInPlace() {
			items.add(ITEM_PREFIX + "1");

			items.replace(ITEM_PREFIX + "1", ITEM_PREFIX + "1_replaced");

			assertEquals(1, items.count());
			assertFalse(items.contains(ITEM_PREFIX + "1"));
			assertTrue(items.contains(ITEM_PREFIX + "1_replaced"));
		}

		@Test
		@DisplayName("Replace item moving from visible to filtered")
		void replace_itemMovingToFiltered_shouldMoveCorrectly() {
			visiblePredicate.setPredicate(item -> !item.contains("replaced"));
			items.add(ITEM_PREFIX + "1");

			items.replace(ITEM_PREFIX + "1", ITEM_PREFIX + "1_replaced");

			assertEquals(1, items.count());
			assertEquals(0, items.visible().count());
			assertEquals(1, items.filtered().count());
			assertTrue(items.filtered().contains(ITEM_PREFIX + "1_replaced"));
		}

		@Test
		@DisplayName("Clear all items")
		void clear_shouldRemoveAll() {
			items.add(asList("a", "b", "c"));
			visiblePredicate.setPredicate(item -> false);
			items.add(asList("d", "e", "f")); // These go to filtered

			items.clear();

			assertEquals(0, items.count());
			assertEquals(0, items.visible().count());
			assertEquals(0, items.filtered().count());
		}

		@Test
		@DisplayName("Get returns unmodifiable collection")
		void get_shouldReturnUnmodifiableCollection() {
			items.add(asList("a", "b", "c"));

			Collection<String> allItems = items.get();

			assertThrows(UnsupportedOperationException.class, () -> allItems.add("d"));
			assertThrows(UnsupportedOperationException.class, () -> allItems.remove("a"));
		}

		@Test
		@DisplayName("Null item handling")
		void nullItem_shouldThrowException() {
			assertThrows(NullPointerException.class, () -> items.add((String) null));
			assertThrows(NullPointerException.class, () -> items.remove((String) null));
			assertThrows(NullPointerException.class, () -> items.replace(null, "replacement"));
			assertThrows(NullPointerException.class, () -> items.replace("item", null));
		}

		@Test
		@DisplayName("Null collection handling")
		void nullCollection_shouldThrowException() {
			assertThrows(NullPointerException.class, () -> items.add((Collection<String>) null));
			assertThrows(NullPointerException.class, () -> items.remove((Collection<String>) null));
			assertThrows(NullPointerException.class, () -> items.set(null));
		}
	}

	@Nested
	@DisplayName("Filtering operations")
	class FilteringOperationsTest {

		private Items<String> items;
		private TestPredicate visiblePredicate;

		@BeforeEach
		void setUp() {
			visiblePredicate = new TestPredicate();
			TestMultiSelection selection = new TestMultiSelection();

			items = Items.builder()
							.<String>refresher(i -> new TestRefresher())
							.selection(visible -> selection)
							.sort(new TestSort())
							.visiblePredicate(visiblePredicate)
							.build();
		}

		@Test
		@DisplayName("Filter moves items from visible to filtered")
		void filter_movesItemsToFiltered() {
			items.add(asList("keep1", "filter1", "keep2", "filter2"));
			visiblePredicate.setPredicate(item -> item.startsWith("keep"));

			items.filter();

			assertEquals(4, items.count());
			assertEquals(2, items.visible().count());
			assertEquals(2, items.filtered().count());
			assertTrue(items.visible().contains("keep1"));
			assertTrue(items.visible().contains("keep2"));
			assertTrue(items.filtered().contains("filter1"));
			assertTrue(items.filtered().contains("filter2"));
		}

		@Test
		@DisplayName("Filter moves items from filtered to visible")
		void filter_movesItemsToVisible() {
			visiblePredicate.setPredicate(item -> false);
			items.add(asList("item1", "item2", "item3"));
			assertEquals(3, items.filtered().count());

			visiblePredicate.setPredicate(item -> true);
			items.filter();

			assertEquals(3, items.visible().count());
			assertEquals(0, items.filtered().count());
		}

		@Test
		@DisplayName("Filter with sorting applied")
		void filter_withSorting_shouldSortVisible() {
			TestSort sort = new TestSort();
			sort.setComparator(Comparator.reverseOrder());

			items = Items.builder()
							.<String>refresher(i -> new TestRefresher())
							.selection(visible -> new TestMultiSelection())
							.sort(sort)
							.visiblePredicate(visiblePredicate)
							.build();

			items.add(asList("a", "b", "c", "d"));

			visiblePredicate.setPredicate(item -> item.compareTo("c") <= 0);
			items.filter();

			List<String> visible = items.visible().get();
			assertEquals(asList("c", "b", "a"), visible);
		}
	}

	@Nested
	@DisplayName("Refresh strategies")
	class RefreshStrategiesTest {

		private Items<String> items;
		private TestRefresher refresher;

		@BeforeEach
		void setUp() {
			refresher = new TestRefresher();

			items = Items.builder()
							.<String>refresher(i -> refresher)
							.selection(visible -> new TestMultiSelection())
							.sort(new TestSort())
							.refreshStrategy(RefreshStrategy.CLEAR)
							.build();

			refresher.setTargetItems(items);
		}

		@Test
		@DisplayName("Clear refresh strategy replaces all items")
		void refresh_clearStrategy_replacesAll() {
			items.add(asList("old1", "old2", "old3"));

			refresher.setItems(asList("new1", "new2", "new3", "new4"));
			items.refresh();

			assertEquals(4, items.count());
			assertTrue(items.contains("new1"));
			assertTrue(items.contains("new2"));
			assertTrue(items.contains("new3"));
			assertTrue(items.contains("new4"));
			assertFalse(items.contains("old1"));
			assertFalse(items.contains("old2"));
			assertFalse(items.contains("old3"));
		}

		@Test
		@DisplayName("Merge refresh strategy updates existing and adds new")
		void refresh_mergeStrategy_mergesItems() {
			items.refreshStrategy().set(RefreshStrategy.MERGE);
			items.add(asList("keep1", "remove1", "keep2"));

			refresher.setItems(asList("keep1", "keep2", "new1"));
			items.refresh();

			assertEquals(3, items.count());
			assertTrue(items.contains("keep1"));
			assertTrue(items.contains("keep2"));
			assertTrue(items.contains("new1"));
			assertFalse(items.contains("remove1"));
		}

		@Test
		@DisplayName("Refresh with callback")
		void refresh_withCallback_shouldNotifyResult() {
			refresher.setItems(asList("a", "b", "c"));

			CountDownLatch latch = new CountDownLatch(1);
			List<String> result = new ArrayList<>();

			items.refresh(refreshedItems -> {
				result.addAll(refreshedItems);
				latch.countDown();
			});

			try {
				assertTrue(latch.await(1, TimeUnit.SECONDS));
				assertEquals(asList("a", "b", "c"), result);
			}
			catch (InterruptedException e) {
				fail("Refresh callback was not called");
			}
		}

		@Test
		@DisplayName("Set operation respects refresh strategy")
		void set_respectsRefreshStrategy() {
			items.add(asList("old1", "old2"));

			// With CLEAR strategy
			items.refreshStrategy().set(RefreshStrategy.CLEAR);
			items.set(asList("new1", "new2"));
			assertEquals(2, items.count());
			assertTrue(items.contains("new1"));
			assertFalse(items.contains("old1"));

			// With MERGE strategy
			items.refreshStrategy().set(RefreshStrategy.MERGE);
			items.set(asList("new1", "new3"));
			assertEquals(2, items.count());
			assertTrue(items.contains("new1"));
			assertTrue(items.contains("new3"));
			assertFalse(items.contains("new2"));
		}
	}

	@Nested
	@DisplayName("Sorting operations")
	class SortingOperationsTest {

		private Items<String> items;
		private TestSort sort;

		@BeforeEach
		void setUp() {
			sort = new TestSort();

			items = Items.builder()
							.<String>refresher(i -> new TestRefresher())
							.selection(visible -> new TestMultiSelection())
							.sort(sort)
							.build();
		}

		@Test
		@DisplayName("Sort visible items on add")
		void add_withSorting_shouldSortVisible() {
			sort.setComparator(Comparator.naturalOrder());
			items.add(asList("c", "a", "b"));

			assertEquals(asList("a", "b", "c"), items.visible().get());
		}

		@Test
		@DisplayName("Manual sort")
		void sort_manually_shouldSort() {
			items.add(asList("c", "a", "b"));

			sort.setComparator(Comparator.naturalOrder());
			items.visible().sort();

			assertEquals(asList("a", "b", "c"), items.visible().get());
		}

		@Test
		@DisplayName("Disable sorting")
		void sort_disabled_maintainsInsertionOrder() {
			sort.setSorted(false);
			items.add(asList("c", "a", "b"));

			assertEquals(asList("c", "a", "b"), items.visible().get());
		}
	}

	@Nested
	@DisplayName("Visible items operations")
	class VisibleItemsOperationsTest {

		private Items<String> items;
		private VisibleItems<String> visible;

		@BeforeEach
		void setUp() {
			items = Items.builder()
							.<String>refresher(i -> new TestRefresher())
							.selection(v -> new TestMultiSelection())
							.sort(new TestSort())
							.build();

			visible = items.visible();
		}

		@Test
		@DisplayName("Add at specific index")
		void add_atIndex_shouldInsertCorrectly() {
			items.add(asList("a", "c"));
			visible.add(1, "b");

			assertEquals(asList("a", "b", "c"), visible.get());
			assertEquals(1, visible.indexOf("b"));
		}

		@Test
		@DisplayName("Set at specific index")
		void set_atIndex_shouldReplace() {
			items.add(asList("a", "b", "c"));
			visible.set(1, "B");

			assertEquals(asList("a", "B", "c"), visible.get());
		}

		@Test
		@DisplayName("Remove at specific index")
		void remove_atIndex_shouldRemove() {
			items.add(asList("a", "b", "c"));

			String removed = visible.remove(1);

			assertEquals("b", removed);
			assertEquals(asList("a", "c"), visible.get());
		}

		@Test
		@DisplayName("Get at specific index")
		void get_atIndex_shouldReturnItem() {
			items.add(asList("a", "b", "c"));

			assertEquals("a", visible.get(0));
			assertEquals("b", visible.get(1));
			assertEquals("c", visible.get(2));
		}

		@Test
		@DisplayName("Index bounds checking")
		void indexOperations_boundsChecking() {
			items.add(asList("a", "b", "c"));

			assertThrows(IndexOutOfBoundsException.class, () -> visible.get(-1));
			assertThrows(IndexOutOfBoundsException.class, () -> visible.get(3));
			assertThrows(IndexOutOfBoundsException.class, () -> visible.remove(-1));
			assertThrows(IndexOutOfBoundsException.class, () -> visible.set(3, "d"));
		}
	}

	@Nested
	@DisplayName("Validation")
	class ValidationTest {

		@Test
		@DisplayName("Validator rejects invalid items")
		void validator_rejectsInvalid() {
			Predicate<String> validator = item -> !item.contains("invalid");

			Items<String> items = Items.builder()
							.<String>refresher(i -> new TestRefresher())
							.selection(visible -> new TestMultiSelection())
							.sort(new TestSort())
							.validator(validator)
							.build();

			items.add("valid");
			assertEquals(1, items.count());

			assertThrows(IllegalArgumentException.class, () -> items.add("invalid_item"));
			assertEquals(1, items.count());
		}

		@Test
		@DisplayName("Default validator accepts all")
		void defaultValidator_acceptsAll() {
			Items<String> items = Items.builder()
							.<String>refresher(i -> new TestRefresher())
							.selection(visible -> new TestMultiSelection())
							.sort(new TestSort())
							.build();

			// Should accept any non-null string
			items.add("anything");
			items.add("123456789012345678901234567890");
			items.add("");

			assertEquals(3, items.count());
		}
	}

	@Nested
	@DisplayName("Concurrent access")
	class ConcurrentAccessTest {

		private static final int THREAD_COUNT = 10;
		private static final int OPERATIONS_PER_THREAD = 100;

		private Items<String> items;
		private ExecutorService executor;

		@BeforeEach
		void setUp() {
			items = Items.builder()
							.<String>refresher(i -> new TestRefresher())
							.selection(visible -> new TestMultiSelection())
							.sort(new TestSort())
							.build();
			executor = Executors.newFixedThreadPool(THREAD_COUNT);
		}

		@Test
		@DisplayName("Concurrent adds are thread-safe")
		void concurrentAdds_threadSafe() throws InterruptedException {
			CountDownLatch startLatch = new CountDownLatch(1);
			CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);

			for (int i = 0; i < THREAD_COUNT; i++) {
				final int threadId = i;
				executor.submit(() -> {
					try {
						startLatch.await();
						for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
							items.add("thread" + threadId + "_item" + j);
						}
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					finally {
						doneLatch.countDown();
					}
				});
			}

			startLatch.countDown();
			assertTrue(doneLatch.await(5, TimeUnit.SECONDS));

			assertEquals(THREAD_COUNT * OPERATIONS_PER_THREAD, items.count());
		}
	}

	// Test implementations

	private static class TestPredicate implements VisiblePredicate<String> {
		private final Value<Predicate<String>> predicate = Value.nullable(item -> true);

		void setPredicate(Predicate<String> newPredicate) {
			predicate.set(newPredicate);
		}

		@Override
		public boolean test(String item) {
			Predicate<String> current = predicate.get();
			return current != null && current.test(item);
		}

		@Override
		public void set(Predicate<String> value) {
			predicate.set(value);
		}

		@Override
		public void clear() {
			predicate.clear();
		}

		@Override
		public Predicate<String> get() {
			return predicate.get();
		}

		@Override
		public boolean addListener(Runnable listener) {
			return predicate.addListener(listener);
		}

		@Override
		public boolean addConsumer(Consumer<? super Predicate<String>> consumer) {
			return predicate.addConsumer(consumer);
		}

		@Override
		public boolean addWeakListener(Runnable listener) {
			return predicate.addWeakListener(listener);
		}

		@Override
		public boolean addWeakConsumer(Consumer<? super Predicate<String>> consumer) {
			return predicate.addWeakConsumer(consumer);
		}

		@Override
		public boolean removeListener(Runnable listener) {
			return predicate.removeListener(listener);
		}

		@Override
		public boolean removeConsumer(Consumer<? super Predicate<String>> consumer) {
			return predicate.removeConsumer(consumer);
		}

		@Override
		public boolean removeWeakListener(Runnable listener) {
			return predicate.removeWeakListener(listener);
		}

		@Override
		public boolean removeWeakConsumer(Consumer<? super Predicate<String>> consumer) {
			return predicate.removeWeakConsumer(consumer);
		}

		@Override
		public void map(UnaryOperator<Predicate<String>> mapper) {
			predicate.map(mapper);
		}

		@Override
		public void validate(Predicate<String> value) {
			predicate.validate(value);
		}

		@Override
		public boolean addValidator(Validator<? super Predicate<String>> validator) {
			return predicate.addValidator(validator);
		}

		@Override
		public boolean removeValidator(Validator<? super Predicate<String>> validator) {
			return predicate.removeValidator(validator);
		}

		@Override
		public is.codion.common.observable.Observable<Predicate<String>> observable() {
			return predicate.observable();
		}

		@Override
		public Observer<Predicate<String>> observer() {
			return predicate.observer();
		}

		@Override
		public void link(Value<Predicate<String>> originalValue) {
			predicate.link(originalValue);
		}

		@Override
		public void unlink(Value<Predicate<String>> originalValue) {
			predicate.unlink(originalValue);
		}

		@Override
		public void link(is.codion.common.observable.Observable<Predicate<String>> observable) {
			predicate.link(observable);
		}

		@Override
		public void unlink(is.codion.common.observable.Observable<Predicate<String>> observable) {
			predicate.unlink(observable);
		}

		@Override
		public boolean isNullable() {
			return predicate.isNullable();
		}
	}

	private static class TestSort implements Sort<String> {
		private Comparator<String> comparator = null;
		private boolean sorted = true;
		private final List<Runnable> listeners = new ArrayList<>();

		void setComparator(Comparator<String> comparator) {
			this.comparator = comparator;
			listeners.forEach(Runnable::run);
		}

		void setSorted(boolean sorted) {
			this.sorted = sorted;
		}

		@Override
		public int compare(String o1, String o2) {
			if (comparator != null) {
				return comparator.compare(o1, o2);
			}
			return 0;
		}

		@Override
		public boolean sorted() {
			return sorted && comparator != null;
		}

		@Override
		public Observer<Boolean> observer() {
			return new Observer<Boolean>() {
				@Override
				public boolean addListener(Runnable listener) {
					listeners.add(listener);
					return true;
				}

				@Override
				public boolean addConsumer(Consumer<? super Boolean> consumer) {return true;}

				@Override
				public boolean addWeakListener(Runnable listener) {
					return addListener(listener);
				}

				@Override
				public boolean addWeakConsumer(Consumer<? super Boolean> consumer) {return true;}

				@Override
				public boolean removeListener(Runnable listener) {
					return listeners.remove(listener);
				}

				@Override
				public boolean removeConsumer(Consumer<? super Boolean> consumer) {return true;}

				@Override
				public boolean removeWeakListener(Runnable listener) {
					return removeListener(listener);
				}

				@Override
				public boolean removeWeakConsumer(Consumer<? super Boolean> consumer) {return true;}
			};
		}
	}

	private static class TestRefresher extends FilterModel.AbstractRefresher<String> {
		private List<String> items = new ArrayList<>();
		private Items<String> targetItems;

		TestRefresher() {
			super(null, false); // No supplier, sync refresh
		}

		void setItems(List<String> items) {
			this.items = new ArrayList<>(items);
		}

		void setTargetItems(Items<String> targetItems) {
			this.targetItems = targetItems;
		}

		@Override
		protected boolean isUserInterfaceThread() {
			return true; // For test simplicity
		}

		@Override
		protected void refreshAsync(Consumer<Collection<String>> onResult) {
			refreshSync(onResult); // Delegate to sync for tests
		}

		@Override
		protected void refreshSync(Consumer<Collection<String>> onResult) {
			Collection<String> result = new ArrayList<>(items);
			processResult(result);
			if (onResult != null) {
				onResult.accept(result);
			}
		}

		@Override
		protected void processResult(Collection<String> result) {
			if (targetItems != null) {
				targetItems.set(result);
			}
		}
	}

	private static class TestMultiSelection implements MultiSelection<String> {
		private final Value<List<String>> selectedItems = Value.nonNull(new ArrayList<String>());
		private final State singleSelection = State.state();

		@Override
		public ObservableState empty() {
			return State.state(selectedItems.get().isEmpty());
		}

		@Override
		public Observer<?> changing() {
			return selectedItems.observer();
		}

		@Override
		public Value<String> item() {
			return Value.nullable(selectedItems.get().isEmpty() ? null : selectedItems.get().get(0));
		}

		@Override
		public void clear() {
			selectedItems.set(new ArrayList<>());
		}

		@Override
		public ObservableState multiple() {
			return State.state(selectedItems.get().size() > 1);
		}

		@Override
		public ObservableState single() {
			return State.state(selectedItems.get().size() == 1);
		}

		@Override
		public State singleSelection() {
			return singleSelection;
		}

		@Override
		public Value<Integer> index() {
			return Value.nonNull(selectedItems.get().isEmpty() ? -1 : 0);
		}

		@Override
		public Indexes indexes() {
			// Simplified implementation for tests
			return new TestIndexes();
		}

		@Override
		public Items<String> items() {
			return new TestItems();
		}

		@Override
		public void selectAll() {}

		@Override
		public int count() {
			return selectedItems.get().size();
		}

		@Override
		public void adjusting(boolean adjusting) {}

		private class TestIndexes implements Indexes {
			private final Value<List<Integer>> indexes = Value.nonNull(new ArrayList<Integer>());

			@Override
			public List<Integer> get() {
				return indexes.get();
			}

			@Override
			public void set(List<Integer> indexList) {
				indexes.set(indexList);
			}

			@Override
			public void clear() {
				indexes.clear();
			}

			@Override
			public void add(int index) {}

			@Override
			public void remove(int index) {}

			@Override
			public void add(Collection<Integer> indexList) {}

			@Override
			public void remove(Collection<Integer> indexList) {}

			@Override
			public boolean contains(int index) {
				return false;
			}

			@Override
			public void increment() {}

			@Override
			public void decrement() {}

			@Override
			public boolean addListener(Runnable listener) {
				return indexes.addListener(listener);
			}

			@Override
			public boolean addConsumer(Consumer<? super List<Integer>> consumer) {
				return indexes.addConsumer(consumer);
			}

			@Override
			public boolean addWeakListener(Runnable listener) {
				return indexes.addWeakListener(listener);
			}

			@Override
			public boolean addWeakConsumer(Consumer<? super List<Integer>> consumer) {
				return indexes.addWeakConsumer(consumer);
			}

			@Override
			public boolean removeListener(Runnable listener) {
				return indexes.removeListener(listener);
			}

			@Override
			public boolean removeConsumer(Consumer<? super List<Integer>> consumer) {
				return indexes.removeConsumer(consumer);
			}

			@Override
			public boolean removeWeakListener(Runnable listener) {
				return indexes.removeWeakListener(listener);
			}

			@Override
			public boolean removeWeakConsumer(Consumer<? super List<Integer>> consumer) {
				return indexes.removeWeakConsumer(consumer);
			}

			@Override
			public void map(UnaryOperator<List<Integer>> mapper) {
				indexes.map(mapper);
			}

			@Override
			public void validate(List<Integer> value) {
				indexes.validate(value);
			}

			@Override
			public boolean addValidator(Validator<? super List<Integer>> validator) {
				return indexes.addValidator(validator);
			}

			@Override
			public boolean removeValidator(Validator<? super List<Integer>> validator) {
				return indexes.removeValidator(validator);
			}

			@Override
			public Observable<List<Integer>> observable() {
				return indexes.observable();
			}

			@Override
			public Observer<List<Integer>> observer() {
				return indexes.observer();
			}

			@Override
			public void link(Value<List<Integer>> originalValue) {
				indexes.link(originalValue);
			}

			@Override
			public void unlink(Value<List<Integer>> originalValue) {
				indexes.unlink(originalValue);
			}

			@Override
			public void link(Observable<List<Integer>> observable) {
				indexes.link(observable);
			}

			@Override
			public void unlink(Observable<List<Integer>> observable) {
				indexes.unlink(observable);
			}

			@Override
			public boolean isNullable() {
				return indexes.isNullable();
			}
		}

		private class TestItems implements Items<String> {
			@Override
			public List<String> get() {
				return new ArrayList<>(selectedItems.get());
			}

			@Override
			public void set(List<String> items) {
				selectedItems.set(new ArrayList<>(items));
			}

			@Override
			public void clear() {
				selectedItems.clear();
			}

			@Override
			public void set(Collection<String> items) {
				selectedItems.set(new ArrayList<>(items));
			}

			@Override
			public void set(Predicate<String> predicate) {}

			@Override
			public void add(Predicate<String> predicate) {}

			@Override
			public void add(String item) {
				List<String> current = new ArrayList<>(selectedItems.get());
				current.add(item);
				selectedItems.set(current);
			}

			@Override
			public void add(Collection<String> items) {
				List<String> current = new ArrayList<>(selectedItems.get());
				current.addAll(items);
				selectedItems.set(current);
			}

			@Override
			public void remove(String item) {
				List<String> current = new ArrayList<>(selectedItems.get());
				current.remove(item);
				selectedItems.set(current);
			}

			@Override
			public void remove(Collection<String> items) {
				List<String> current = new ArrayList<>(selectedItems.get());
				current.removeAll(items);
				selectedItems.set(current);
			}

			@Override
			public boolean contains(String item) {
				return selectedItems.get().contains(item);
			}

			@Override
			public boolean addListener(Runnable listener) {
				return selectedItems.addListener(listener);
			}

			@Override
			public boolean addConsumer(Consumer<? super List<String>> consumer) {
				return selectedItems.addConsumer(consumer);
			}

			@Override
			public boolean addWeakListener(Runnable listener) {
				return selectedItems.addWeakListener(listener);
			}

			@Override
			public boolean addWeakConsumer(Consumer<? super List<String>> consumer) {
				return selectedItems.addWeakConsumer(consumer);
			}

			@Override
			public boolean removeListener(Runnable listener) {
				return selectedItems.removeListener(listener);
			}

			@Override
			public boolean removeConsumer(Consumer<? super List<String>> consumer) {
				return selectedItems.removeConsumer(consumer);
			}

			@Override
			public boolean removeWeakListener(Runnable listener) {
				return selectedItems.removeWeakListener(listener);
			}

			@Override
			public boolean removeWeakConsumer(Consumer<? super List<String>> consumer) {
				return selectedItems.removeWeakConsumer(consumer);
			}

			@Override
			public void map(UnaryOperator<List<String>> mapper) {
				selectedItems.map(mapper);
			}

			@Override
			public void validate(List<String> value) {
				selectedItems.validate(value);
			}

			@Override
			public boolean addValidator(Validator<? super List<String>> validator) {
				return selectedItems.addValidator(validator);
			}

			@Override
			public boolean removeValidator(Validator<? super List<String>> validator) {
				return selectedItems.removeValidator(validator);
			}

			@Override
			public is.codion.common.observable.Observable<List<String>> observable() {
				return selectedItems.observable();
			}

			@Override
			public Observer<List<String>> observer() {
				return selectedItems.observer();
			}

			@Override
			public void link(Value<List<String>> originalValue) {
				selectedItems.link(originalValue);
			}

			@Override
			public void unlink(Value<List<String>> originalValue) {
				selectedItems.unlink(originalValue);
			}

			@Override
			public void link(is.codion.common.observable.Observable<List<String>> observable) {
				selectedItems.link(observable);
			}

			@Override
			public void unlink(is.codion.common.observable.Observable<List<String>> observable) {
				selectedItems.unlink(observable);
			}

			@Override
			public boolean isNullable() {
				return selectedItems.isNullable();
			}
		}
	}
}