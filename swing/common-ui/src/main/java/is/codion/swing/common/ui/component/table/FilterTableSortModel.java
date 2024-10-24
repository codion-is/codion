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
package is.codion.swing.common.ui.component.table;

import is.codion.common.observer.Observer;

import javax.swing.SortOrder;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Handles the column sorting states for a {@link FilterTable}.
 * @param <R> the type representing a row in the table model
 * @param <C> the type representing the column identifiers in the table model
 */
public interface FilterTableSortModel<R, C> {

	/**
	 * @return a {@link Comparator} based on this sort model
	 */
	Comparator<R> comparator();

	/**
	 * Clears the sorting state and adds the given column sorting order.
	 * @param identifier the identifier of the column to sort by
	 * @param sortOrder the sorting order
	 * @throws IllegalStateException in case sorting is disabled for the given column
	 * @see #addSortOrder(Object, SortOrder)
	 * @see #columnSortOrder()
	 * @see #columnSortOrder(Object)
	 * @see #setSortingEnabled(Object, boolean)
	 */
	void setSortOrder(C identifier, SortOrder sortOrder);

	/**
	 * Adds the given column sorting order to the currently sorted columns.
	 * If no column sorting is enabled, this call is the equivilent to using
	 * {@link #setSortOrder(Object, SortOrder)}.
	 * @param identifier the identifier of the column to sort by
	 * @param sortOrder the sorting order
	 * @throws IllegalStateException in case sorting is disabled for the given column
	 * @see #setSortOrder(Object, SortOrder)
	 * @see #columnSortOrder()
	 * @see #columnSortOrder(Object)
	 * @see #setSortingEnabled(Object, boolean)
	 */
	void addSortOrder(C identifier, SortOrder sortOrder);

	/**
	 * @param identifier the column identifier
	 * @return the {@link ColumnSortOrder} associated with the given column or an empty Optional if the column is not sorted
	 */
	Optional<ColumnSortOrder<C>> columnSortOrder(C identifier);

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
	 * Disabling sorting will cause {@link #setSortOrder(Object, SortOrder)} and
	 * {@link #addSortOrder(Object, SortOrder)} to throw a {@link IllegalStateException} for the given column.
	 * @param identifier the column identifier
	 * @param sortingEnabled true if sorting should be enabled for the given column
	 */
	void setSortingEnabled(C identifier, boolean sortingEnabled);

	/**
	 * @param identifier the column identifier
	 * @return true if sorting is enabled for the given column
	 */
	boolean isSortingEnabled(C identifier);

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
		 * @return the sort priority
		 */
		int priority();
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
