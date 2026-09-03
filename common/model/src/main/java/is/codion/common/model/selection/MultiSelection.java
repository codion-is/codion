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
 * Copyright (c) 2024 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.model.selection;

import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.Value;

import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

/**
 * A selection model managing multiple selected items.
 * <p>
 * When multiple items are selected, the inherited {@link SingleSelection}
 * {@link #index()} and {@link #item()} methods represent the minimum selected index
 * and its corresponding item. If the selection is empty, {@link #index()} returns null
 * and {@link #item()} returns null.
 * <p>
 * Examples:
 * <ul>
 * <li>Selected indexes [2, 5, 8] → {@code index().get()} returns 2, {@code item().get()} returns item at index 2</li>
 * <li>Selected indexes [7] → {@code index().get()} returns 7, {@code item().get()} returns item at index 7</li>
 * <li>No selection → {@code index().get()} returns null, {@code item().get()} returns null</li>
 * </ul>
 * @param <T> the type of items
 */
public interface MultiSelection<T> extends SingleSelection<T> {

	/**
	 * @return an {@link ObservableState} indicating whether multiple items are selected
	 */
	ObservableState multiple();

	/**
	 * @return an {@link ObservableState} indicating whether a single item is selected
	 */
	ObservableState single();

	/**
	 * @return the {@link State} controlling whether single selection mode is enabled
	 */
	State singleSelection();

	/**
	 * Returns the {@link Value} controlling the selected index.
	 * <p>In a multi-selection context, this represents the minimum selected index.
	 * When multiple items are selected, this value tracks the lowest index among
	 * the selected items. The value's {@code get()} method returns null if the selection is empty.
	 * @return the {@link Value} controlling the minimum selected index, whose {@code get()} method returns null if selection is empty
	 */
	Value<Integer> index();

	/**
	 * <p>Returns the {@link Indexes} controlling the selected indexes.
	 * <p>Notifies only when the selected indexes change.
	 * @return the {@link Indexes} controlling the selected indexes
	 */
	Indexes indexes();

	/**
	 * <p>Returns the {@link Items} controlling the selected items.
	 * <p>Notifies when the selected items change, and when an instance a selected index refers to is replaced, as
	 * happens when the items are refreshed or replaced, the replacement being the same item by {@code equals()}
	 * but not the same object. {@link #indexes()} notifies only when the indexes change.
	 * @return the {@link Items} controlling the selected items
	 */
	Items<T> items();

	/**
	 * Selects all items
	 * @see #indexes()
	 */
	void selectAll();

	/**
	 * @return the number of selected items.
	 */
	int count();

	/**
	 * {@link Grouping} controls whether the subsequent selection events
	 * should be grouped and not triggered individually
	 * <p>Note that grouping is not reentrant, {@code grouping().set(false)} ends the group whether or not it
	 * opened one. Code which may run inside a group of its caller's making saves and restores the state:
	 * {@snippet :
	 * boolean wasGrouping = selection.grouping().is();
	 * selection.grouping().set(true);
	 * try {
	 *   // mutate the selection
	 * }
	 * finally {
	 *   selection.grouping().set(wasGrouping);
	 * }
	 *}
	 * @return the {@link Grouping} instance
	 */
	Grouping grouping();

	/**
	 * <p>Returns an observer notified on every change to the selected indexes, grouped or not, the raw stream of
	 * changes a {@code javax.swing.event.ListSelectionListener} would see. {@link #index()}, {@link #indexes()},
	 * {@link #item()} and {@link #items()} notify once a group of changes has ended, this observer notifies as
	 * the changes happen, so a listener may observe intermediate states, such as the momentarily empty selection
	 * while a refresh restores it. Suited to a live status display, not to anything acting on the selection.
	 * @return an observer notified each time the selected indexes change, grouped or not
	 * @see #grouping()
	 */
	Observer<?> adjusting();

	/**
	 * @param items the indexed items
	 * @return a default {@link MultiSelection} implementation
	 * @param <T> the item type
	 */
	static <T> MultiSelection<T> multiSelection(IndexedItems<T> items) {
		return new DefaultMultiSelection<>(requireNonNull(items));
	}

	/**
	 * @param items the indexed items
	 * @param store the store keeping the selected indexes
	 * @return a default {@link MultiSelection} implementation over the given store
	 * @param <T> the item type
	 * @see IndexStore
	 */
	static <T> MultiSelection<T> multiSelection(IndexedItems<T> items, IndexStore store) {
		return new DefaultMultiSelection<>(requireNonNull(items), requireNonNull(store));
	}

	/**
	 * Manages the selected indexes.
	 */
	interface Indexes extends Value<List<Integer>> {

		@Override
		@NonNull List<Integer> get();

		/**
		 * Adds the given index to the selected indexes
		 * @param index the index
		 */
		void add(int index);

		/**
		 * Removes the given index from the selection
		 * @param index the index
		 */
		void remove(int index);

		/**
		 * Adds the given indexes to the selection
		 * @param indexes the indexes to add to the selection
		 */
		void add(Collection<Integer> indexes);

		/**
		 * Removes the given indexes from the selection
		 * @param indexes the indexes
		 */
		void remove(Collection<Integer> indexes);

		/**
		 * @param index the index
		 * @return true if the given index is selected
		 */
		boolean contains(int index);

		/**
		 * Increments all selected indexes by one, with wrap-around.
		 * If the selection is empty the lowest available index is selected.
		 */
		void increment();

		/**
		 * Decrements all selected indexes by one, with wrap-around.
		 * If the selection is empty the highest available index is selected.
		 */
		void decrement();
	}

	/**
	 * Manages the selected items.
	 * @param <R> the item type
	 */
	interface Items<R> extends Value<List<R>> {

		@Override
		@NonNull List<R> get();

		/**
		 * @param items the items to select
		 */
		void set(Collection<R> items);

		/**
		 * Sets the items passing the predicate test as the selection
		 * @param predicate the predicate
		 */
		void set(Predicate<R> predicate);

		/**
		 * Adds the items passing the predicate test to the selection
		 * @param predicate the predicate
		 */
		void add(Predicate<R> predicate);

		/**
		 * Adds the given item to the selection
		 * @param item the item to add to the selection
		 */
		void add(R item);

		/**
		 * Adds the given items to the selection
		 * @param items the items to add to the selection
		 */
		void add(Collection<R> items);

		/**
		 * Remove the given item from the selection
		 * @param item the item to remove from the selection
		 */
		void remove(R item);

		/**
		 * Remove the given items from the selection
		 * @param items the items to remove from the selection
		 */
		void remove(Collection<R> items);

		/**
		 * @param item the item
		 * @return true if the given item is selected
		 */
		boolean contains(R item);
	}

	/**
	 * Provides access to indexed items
	 * @param <R> the item type
	 */
	interface IndexedItems<R> {

		/**
		 * @return the number of items
		 */
		int size();

		/**
		 * @param index the row index
		 * @return the item at the given index in this model
		 * @throws IndexOutOfBoundsException in case the index is out of bounds
		 */
		R get(int index);

		/**
		 * @param item the item
		 * @return the index of the item in this model, -1 if it is not included
		 */
		int indexOf(R item);

		/**
		 * @return an unmodifiable view of the items
		 */
		List<R> get();
	}

	/**
	 * Controls whether selection change grouping is enabled
	 */
	interface Grouping {

		/**
		 * @param grouping the grouping value
		 */
		void set(boolean grouping);

		/**
		 * @return true if grouping is enabled
		 */
		boolean is();
	}

	/**
	 * <p>The selected indexes a {@link MultiSelection} is a view over, the one part of a selection that differs per toolkit.
	 * <p>A store notifies {@link #changing()} before and {@link #changed()} after its indexes change, whoever
	 * changed them, the selection deriving its index and item values from {@link #get()} on each {@link #changed()}.
	 * @see #multiSelection(IndexedItems, IndexStore)
	 */
	interface IndexStore {

		/**
		 * @return an unmodifiable snapshot of the selected indexes, iterated in ascending order
		 */
		Set<Integer> get();

		/**
		 * <p>Replaces the selected indexes. A no-op if they are unchanged, otherwise {@link #changing()} is notified
		 * before and {@link #changed()} after, the latter at the end of the adjustment while {@link #grouping()}.
		 * <p>Under {@link #singleSelection()} only the highest of the given indexes is selected.
		 * @param indexes the indexes to select
		 */
		void set(Collection<Integer> indexes);

		/**
		 * @return the number of stored indexes
		 */
		int size();

		/**
		 * @param index the index
		 * @return true if the given index is selected
		 */
		boolean contains(int index);

		/**
		 * @return the {@link State} controlling single selection mode, the selection is cleared when it changes
		 */
		State singleSelection();

		/**
		 * @return the {@link Grouping} instance
		 */
		Grouping grouping();

		/**
		 * @return an observer notified before the selected indexes change
		 */
		Observer<?> changing();

		/**
		 * @return an observer notified after the selected indexes changed, by whoever changed them
		 */
		Observer<?> changed();

		/**
		 * @return an observer notified on every change to the selected indexes, grouped or not, as the changes happen
		 * @see #changed()
		 */
		Observer<?> adjusting();
	}
}
