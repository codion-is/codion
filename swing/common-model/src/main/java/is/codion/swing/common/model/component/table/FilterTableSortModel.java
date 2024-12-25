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
 * Copyright (c) 2013 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.component.table;

import is.codion.common.observable.Observer;
import is.codion.common.state.State;

import javax.swing.SortOrder;
import java.util.Comparator;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Handles the column sorting states for a {@link FilterTableModel}.
 * @param <R> the type representing a row in the table model
 * @param <C> the type representing the column identifiers in the table model
 */
public interface FilterTableSortModel<R, C> {

	/**
	 * @return a {@link Comparator} based on this sort model
	 */
	Comparator<R> comparator();

	/**
	 * @param identifier the column identifier
	 * @return the {@link Sort} for the given column
	 */
	Sort sort(C identifier);

	/**
	 * @param identifier the column identifier
	 * @return the {@link ColumnSortOrder} associated with the given column
	 */
	ColumnSortOrder<C> columnSortOrder(C identifier);

	/**
	 * @return the current column sort order, in order of priority or an empty list in case this model is unsorted
	 */
	List<ColumnSortOrder<C>> columnSortOrder();

	/**
	 * Clears the sorting states from this sort model. Note that only one sorting change event
	 * will happen, with the first sort column.
	 */
	void clear();

	/**
	 * <p>Locking the sorting for a column will cause a {@link IllegalStateException} to be thrown
	 * when trying to modify its sorting state.
	 * @param identifier the column identifier
	 * @return the {@link State} controlling whether sorting is locked for the given column
	 */
	State locked(C identifier);

	/**
	 * @return an observer notified each time the sorting changes, with the current sorted state as event data
	 */
	Observer<Boolean> observer();

	/**
	 * Specifies a sorting state for a column.
	 */
	interface ColumnSortOrder<C> {

		/**
		 * @return the column identifier
		 */
		C identifier();

		/**
		 * @return the {@link SortOrder} currently associated with the column
		 */
		SortOrder sortOrder();

		/**
		 * @return the sort priority, -1 if not sorted
		 */
		int priority();
	}

	/**
	 * Manages the {@link SortOrder} for a given column
	 */
	interface Sort {

		/**
		 * Clears the sorting state and adds the given sorting order.
		 * @param sortOrder the sorting order
		 * @throws IllegalStateException in case sorting is locked for this column
		 * @see #add(SortOrder)
		 * @see FilterTableSortModel#columnSortOrder()
		 * @see FilterTableSortModel#columnSortOrder(Object)
		 * @see FilterTableSortModel#locked(Object)
		 */
		void set(SortOrder sortOrder);

		/**
		 * Adds the given column sorting order to the currently sorted columns.
		 * If no column sorting is enabled, this call is the equivilent to using
		 * {@link #set(SortOrder)}.
		 * @param sortOrder the sorting order
		 * @throws IllegalStateException in case sorting is locked for this column
		 * @see #set(SortOrder)
		 * @see FilterTableSortModel#columnSortOrder()
		 * @see FilterTableSortModel#columnSortOrder(Object)
		 * @see FilterTableSortModel#locked(Object)
		 */
		void add(SortOrder sortOrder);
	}

	/**
	 * {@link SortOrder#ASCENDING} to {@link SortOrder#DESCENDING} to {@link SortOrder#UNSORTED} to {@link SortOrder#ASCENDING}.
	 * @param currentSortOrder the current sort order
	 * @return the next sort order
	 */
	static SortOrder nextSortOrder(SortOrder currentSortOrder) {
		requireNonNull(currentSortOrder);
		switch (currentSortOrder) {
			case UNSORTED:
				return SortOrder.ASCENDING;
			case ASCENDING:
				return SortOrder.DESCENDING;
			case DESCENDING:
				return SortOrder.UNSORTED;
			default:
				throw new IllegalStateException("Unknown sort order: " + currentSortOrder);
		}
	}
}
