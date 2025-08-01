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
 * Copyright (c) 2013 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.common.observer.Observable;
import is.codion.common.observer.Observer;
import is.codion.common.state.State;

import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.util.Collection;
import java.util.List;

/**
 * A TableColumnModel handling hidden columns.
 * Note that this column model does not support dynamically adding or removing columns,
 * {@link #addColumn(TableColumn)} and {@link #removeColumn(TableColumn)} both throw {@link UnsupportedOperationException}.
 * @param <C> the type of column identifier
 */
public interface FilterTableColumnModel<C> extends TableColumnModel {

	/**
	 * @return an unmodifiable view of all columns in this model, both hidden and visible, in no particular order
	 */
	Collection<FilterTableColumn<C>> columns();

	/**
	 * @return an unmodifiable view of all column identifiers in this model, both hidden and visible, in no particular order
	 */
	Collection<C> identifiers();

	/**
	 * @return the visible columns
	 */
	VisibleColumns<C> visible();

	/**
	 * @return the hidden columns
	 */
	HiddenColumns<C> hidden();

	/**
	 * Returns a {@link State} instance controlling whether this model is locked or not.
	 * A locked column model does not allow adding or removing of columns, but columns can be reordered.
	 * @return a {@link State} controlling whether this model is locked or not
	 */
	State locked();

	/**
	 * Returns the TableColumn with the given identifier
	 * @param identifier the column identifier
	 * @return the TableColumn with the given identifier
	 * @throws IllegalArgumentException in case this table model does not contain a column with the given identifier
	 */
	FilterTableColumn<C> column(C identifier);

	@Override
	FilterTableColumn<C> getColumn(int columnIndex);

	/**
	 * Returns the {@link State} for controlling the column visibility
	 * @param identifier the column identifier
	 * @return a {@link State} for controlling the column visibility
	 * @throws IllegalArgumentException in case the column is not found
	 */
	State visible(C identifier);

	/**
	 * @param identifier the column identifier
	 * @return true if this column model contains a column with the given identifier
	 */
	boolean contains(C identifier);

	/**
	 * @param modelColumnIndex the column model index
	 * @return the column identifier
	 */
	C identifier(int modelColumnIndex);

	/**
	 * Resets the columns to their original location and visibility
	 */
	void reset();

	/**
	 * @return an observer notified each time a column is hidden
	 */
	Observer<C> columnHidden();

	/**
	 * @return an observer notified each time a column is shown
	 */
	Observer<C> columnShown();

	/**
	 * Controls the visible columns
	 * @param <C> the column identifier type
	 */
	interface VisibleColumns<C> extends Observable<List<C>> {

		/**
		 * @return the visible columns or an empty list in case no columns are visible
		 */
		@Override
		List<C> get();

		/**
		 * Arranges the columns so that only the given columns are visible and in the given order
		 * @param identifiers the column identifiers
		 * @throws IllegalArgumentException in case a column is not found
		 */
		void set(C... identifiers);

		/**
		 * Arranges the columns so that only the given columns are visible and in the given order
		 * @param identifiers the column identifiers
		 * @throws IllegalArgumentException in case a column is not found
		 */
		void set(List<C> identifiers);

		/**
		 * @return an unmodifiable view of the currently visible columns
		 */
		List<FilterTableColumn<C>> columns();
	}

	/**
	 * Observes the hidden columns
	 * @param <C> the column identifier type
	 */
	interface HiddenColumns<C> extends Observable<Collection<C>> {

		/**
		 * @return the hidden columns or an empty list in case no columns are hidden
		 */
		@Override
		Collection<C> get();

		/**
		 * @return an unmodifiable view of the currently hidden columns
		 */
		Collection<FilterTableColumn<C>> columns();
	}
}
