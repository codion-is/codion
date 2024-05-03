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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.manual.swing.common.model.component.table;

import is.codion.swing.common.model.component.table.FilteredTableModel;
import is.codion.swing.common.model.component.table.FilteredTableModel.Columns;
import is.codion.swing.common.model.component.table.FilteredTableSelectionModel;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import static is.codion.manual.swing.common.model.component.table.FilteredTableModelDemo.TableRow.INTEGER_COLUMN;
import static is.codion.manual.swing.common.model.component.table.FilteredTableModelDemo.TableRow.STRING_COLUMN;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

public final class FilteredTableModelDemo {
	// tag::filteredTableModel[]
	// Define a class representing the table rows
	public static final class TableRow {

		public static final int STRING_COLUMN = 0;
		public static final int INTEGER_COLUMN = 1;

		private final String stringValue;

		private final Integer integerValue;

		TableRow(String stringValue, Integer integerValue) {
			this.stringValue = stringValue;
			this.integerValue = integerValue;
		}

		String stringValue() {
			return stringValue;
		}

		Integer integerValue() {
			return integerValue;
		}
	}

	public static FilteredTableModel<TableRow, Integer> createFilteredTableModel() {
		List<Integer> columnIdentifiers =
						unmodifiableList(asList(STRING_COLUMN, INTEGER_COLUMN));

		// Implement Columns, providing the table column configuration
		Columns<TableRow, Integer> columns = new Columns<TableRow, Integer>() {

			@Override
			public List<Integer> identifiers() {
				return columnIdentifiers;
			}

			@Override
			public Class<?> columnClass(Integer identifier) {
				switch (identifier) {
					case STRING_COLUMN:
						return String.class;
					case INTEGER_COLUMN:
						return Integer.class;
					default:
						throw new IllegalArgumentException();
				}
			}
			@Override
			public Object value(TableRow row, Integer identifier) {
				switch (identifier) {
					case STRING_COLUMN:
						return row.stringValue();
					case INTEGER_COLUMN:
						return row.integerValue();
					default:
						throw new IllegalArgumentException();
				}
			}
		};

		// Implement a item supplier responsible for supplying the table row items,
		// without one the table can be populated by adding items manually
		Supplier<Collection<TableRow>> items = () -> asList(
						new TableRow("A string", 42),
						new TableRow("Another string", 43));

		// Create the table model
		FilteredTableModel<TableRow, Integer> tableModel =
						FilteredTableModel.builder(columns)
										.items(items)
										// if true then the item supplier is called in a
										// background thread when the model is refreshed
										.asyncRefresh(false)
										.build();

		// Populate the model
		tableModel.refresh();

		// Select the first row
		FilteredTableSelectionModel<TableRow> selectionModel = tableModel.selectionModel();
		selectionModel.setSelectedIndex(0);

		// With async refresh enabled
		// tableModel.refreshThen(items ->
		//        selectionModel.setSelectedIndex(0));

		return tableModel;
	}
	// end::filteredTableModel[]
}
