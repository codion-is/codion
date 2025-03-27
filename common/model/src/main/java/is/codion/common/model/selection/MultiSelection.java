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
 * Copyright (c) 2024 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.model.selection;

import is.codion.common.state.ObservableState;
import is.codion.common.state.State;
import is.codion.common.value.Value;

import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/**
 * A selection model managing multiple selected items.
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
	 * @return the {@link Value} controlling the selected index, -1 if selection is empty
	 */
	Value<Integer> index();

	/**
	 * @return the {@link Indexes} controlling the selected indexes
	 */
	Indexes indexes();

	/**
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
}
