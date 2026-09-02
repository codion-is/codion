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
 * Copyright (c) 2010 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.component.table;

import is.codion.common.model.component.table.FilterTableModel;
import is.codion.swing.common.model.component.list.FilterListSelection;

import org.jspecify.annotations.Nullable;

import javax.swing.table.TableModel;
import java.util.function.Function;

/**
 * A Swing {@link TableModel} based on the UI-agnostic
 * {@link is.codion.common.model.component.table.FilterTableModel}, adding the {@link TableModel}
 * interface along with cell editing (via {@link RowEditor}) — mirroring how {@code SwingFilterListModel}
 * extends {@code ListModel}. The rich model logic (items, selection, filtering, sorting, export) lives
 * in the common module; this only adds the Swing coat, with {@link #selection()} narrowed to a
 * {@link FilterListSelection} (a {@code javax.swing.ListSelectionModel}).
 * @param <R> the type representing the rows in this table model
 * @param <C> the type used to identify columns in this table model, Integer for indexed identification for example
 * @see #builder()
 */
public interface SwingFilterTableModel<R, C> extends FilterTableModel<R, C>, TableModel {

	/**
	 * @return the {@link FilterListSelection} instance used by this table model
	 */
	@Override
	FilterListSelection<R> selection();

	/**
	 * @return the {@link RowEditor} providing the row editing functionality
	 */
	RowEditor<R, C> rowEditor();

	/**
	 * Notifies all listeners that all cell values in the table's rows may have changed.
	 * The number of rows may also have changed and the JTable should redraw the table from scratch.
	 * The structure of the table (as in the order of the columns) is assumed to be the same.
	 */
	void fireTableDataChanged();

	/**
	 * Notifies all listeners that the given rows have changed
	 * @param fromIndex the from index
	 * @param toIndex the to index
	 */
	void fireTableRowsUpdated(int fromIndex, int toIndex);

	/**
	 * @return a {@link Builder.ColumnsStep} instance
	 */
	static Builder.ColumnsStep builder() {
		return DefaultSwingFilterTableModel.DefaultBuilder.COLUMNS;
	}

	/**
	 * Builds a {@link SwingFilterTableModel}, the {@link FilterTableModel.Builder} options with the selection based
	 * on a {@code javax.swing.ListSelectionModel}, adding {@link #rowEditor(Function)}.
	 * @param <R> the row type
	 * @param <C> the column identifier type
	 */
	interface Builder<R, C> extends FilterTableModel.Builder<R, C, Builder<R, C>> {

		/**
		 * Provides a {@link Builder} instance
		 */
		interface ColumnsStep extends FilterTableModel.Builder.ColumnsStep {

			@Override
			<R, C> Builder<R, C> columns(TableColumns<R, C> columns);
		}

		/**
		 * @param rowEditor supplies the row editor
		 * @return this builder instance
		 */
		Builder<R, C> rowEditor(Function<SwingFilterTableModel<R, C>, RowEditor<R, C>> rowEditor);

		@Override
		SwingFilterTableModel<R, C> build();
	}

	/**
	 * Handles the editing of rows
	 * @param <R> the row type
	 * @param <C> the column identifier type
	 */
	interface RowEditor<R, C> {

		/**
		 * @param row the row
		 * @param identifier the column identifier
		 * @return true if the given cell is editable
		 * @see TableModel#isCellEditable(int, int)
		 */
		boolean editable(R row, C identifier);

		/**
		 * <p>Sets the value of the given column and row.
		 * <p>This method is responsible for notifying the model of the change.
		 * @param value the value to set
		 * @param rowIndex the row index
		 * @param row the row object
		 * @param identifier the column identifier
		 * @throws IllegalStateException in case the cell is not editable
		 * @see TableModel#setValueAt(Object, int, int)
		 * @see #editable(Object, Object)
		 */
		void set(@Nullable Object value, int rowIndex, R row, C identifier);
	}
}
