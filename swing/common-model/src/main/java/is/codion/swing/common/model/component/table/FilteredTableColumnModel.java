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

import is.codion.common.event.EventObserver;
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
public interface FilteredTableColumnModel<C> extends TableColumnModel {

	/**
	 * @return an unmodifiable view of all columns in this model, both hidden and visible, in no particular order
	 */
	Collection<FilteredTableColumn<C>> columns();

	/**
	 * @return an unmodifiable view of the currently visible columns
	 */
	List<FilteredTableColumn<C>> visible();

	/**
	 * @return an unmodifiable view of currently hidden columns, in no particular order
	 */
	Collection<FilteredTableColumn<C>> hidden();

	/**
	 * Returns a {@link State} instance controlling whether this model is locked or not.
	 * A locked column model does not allow adding or removing of columns, but columns can be reordered.
	 * @return a {@link State} controlling whether this model is locked or not
	 */
	State locked();

	/**
	 * Arranges the columns so that only the given columns are visible and in the given order
	 * @param columnIdentifiers the column identifiers
	 * @throws IllegalArgumentException in case a column is not found
	 */
	void setVisibleColumns(C... columnIdentifiers);

	/**
	 * Arranges the columns so that only the given columns are visible and in the given order
	 * @param columnIdentifiers the column identifiers
	 * @throws IllegalArgumentException in case a column is not found
	 */
	void setVisibleColumns(List<C> columnIdentifiers);

	/**
	 * Returns the TableColumn with the given identifier
	 * @param columnIdentifier the column identifier
	 * @return the TableColumn with the given identifier
	 * @throws IllegalArgumentException in case this table model does not contain a column with the given identifier
	 */
	FilteredTableColumn<C> column(C columnIdentifier);

	@Override
	FilteredTableColumn<C> getColumn(int columnIndex);

	/**
	 * Returns the State for controlling the column visibility
	 * @param columnIdentifier the column identifier
	 * @return a State for controlling the column visibility
	 * @throws IllegalArgumentException in case the column is not found
	 */
	State visible(C columnIdentifier);

	/**
	 * @param columnIdentifier the column identifier
	 * @return true if this column model contains a column with the given identifier
	 */
	boolean containsColumn(C columnIdentifier);

	/**
	 * @param modelColumnIndex the column model index
	 * @return the column identifier
	 */
	C columnIdentifier(int modelColumnIndex);

	/**
	 * Resets the columns to their original location and visibility
	 */
	void resetColumns();

	/**
	 * @return an observer notified each time a column is hidden
	 */
	EventObserver<C> columnHiddenEvent();

	/**
	 * @return an observer notified each time a column is shown
	 */
	EventObserver<C> columnShownEvent();
}
