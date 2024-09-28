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
 * Copyright (c) 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.model.selection;

import is.codion.common.observer.Mutable;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/**
 * A selection model
 * @param <T> the type of items
 */
public interface MultiItemSelection<T> extends SingleItemSelection<T> {

	/**
	 * @return a {@link StateObserver} indicating whether multiple items are selected
	 */
	StateObserver multiple();

	/**
	 * @return a {@link StateObserver} indicating whether a single item is selected
	 */
	StateObserver single();

	/**
	 * @return a State controlling the single selection mode of this selection model
	 */
	State singleSelectionMode();

	/**
	 * @return a {@link Mutable} for the index of the selected item, -1 if none is selected and
	 * the minimum selected index if more than one item is selected
	 */
	Mutable<Integer> index();

	/**
	 * @return the seleted {@link Indexes}
	 */
	Indexes indexes();

	/**
	 * @return the selected {@link Items}
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
	 * Controls the selected indexes.
	 */
	interface Indexes extends Mutable<List<Integer>> {

		/**
		 * @param indexes the indexes to select
		 */
		void set(Collection<Integer> indexes);

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
		 * Adds these indexes to the selection
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
		 * Moves all selected indexes down one index, wraps around.
		 * If the selection is empty the first index is selected.
		 */
		void moveDown();

		/**
		 * Moves all selected indexes up one index, wraps around.
		 * If the selection is empty the last index is selected.
		 */
		void moveUp();
	}

	/**
	 * Controls the selected items
	 * @param <R> the item type
	 */
	interface Items<R> extends Mutable<List<R>> {

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
