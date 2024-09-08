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
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.model.table;

import is.codion.common.event.EventObserver;
import is.codion.common.observable.Observable;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/**
 * A table selection model
 * @param <R> the type of rows in the table model
 */
public interface TableSelectionModel<R> {

	/**
	 * @return a {@link StateObserver} indicating whether the selection is empty
	 */
	StateObserver selectionEmpty();

	/**
	 * @return a {@link StateObserver} indicating whether one or more items are selected
	 */
	StateObserver selectionNotEmpty();

	/**
	 * @return a {@link StateObserver} indicating whether multiple rows are selected
	 */
	StateObserver multipleSelection();

	/**
	 * @return a {@link StateObserver} indicating whether a single row is selected
	 */
	StateObserver singleSelection();

	/**
	 * @return a State controlling the single selection mode of this selection model
	 */
	State singleSelectionMode();

	/**
	 * To prevent a selection change, add a listener throwing a {@link is.codion.common.model.CancelException}.
	 * @return an observer notified when the selection is about to change
	 */
	EventObserver<?> selectionChanging();

	/**
	 * @return an {@link Observable} for the index of the selected row, -1 if none is selected and
	 * the minimum selected index if more than one row is selected
	 */
	Observable<Integer> selectedIndex();

	/**
	 * @return the SelectedIndexes
	 */
	SelectedIndexes selectedIndexes();

	/**
	 * @return an {@link Observable} for the selected item
	 */
	Observable<R> selectedItem();

	/**
	 * @return the {@link SelectedItems}
	 */
	SelectedItems<R> selectedItems();

	/**
	 * Selects all visible rows
	 * @see #selectedIndexes()
	 */
	void selectAll();

	/**
	 * @return the number of selected indexes in the underlying selection model.
	 */
	int selectionCount();

	/**
	 * @param item the item
	 * @return true if the item is selected
	 */
	boolean isSelected(R item);

	/**
	 * Clears the selection
	 */
	void clearSelection();

	/**
	 * Controls the selected indexes.
	 */
	interface SelectedIndexes extends Observable<List<Integer>> {

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
		 * Moves all selected indexes down one index, wraps around.
		 * If the selection is empty the first item is selected.
		 */
		void moveDown();

		/**
		 * Moves all selected indexes up one index, wraps around.
		 * If the selection is empty the last item is selected.
		 */
		void moveUp();
	}

	/**
	 * Controls the selected items
	 * @param <R> the item type
	 */
	interface SelectedItems<R> extends Observable<List<R>> {

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
	}
}
