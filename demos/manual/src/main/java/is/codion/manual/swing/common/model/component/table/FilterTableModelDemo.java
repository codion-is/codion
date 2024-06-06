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

import is.codion.swing.common.model.component.table.FilterTableModel;
import is.codion.swing.common.model.component.table.FilterTableModel.Columns;
import is.codion.swing.common.model.component.table.FilterTableSelectionModel;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

public final class FilterTableModelDemo {
	// tag::filterTableModel[]
	// Define a enum denoting the columns
	public enum Column {
		INTEGER,
		STRING
	}

	// Define a class representing the table rows
	public static final class Row {

		private final String stringValue;
		private final Integer integerValue;

		Row(String stringValue, Integer integerValue) {
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

	public static FilterTableModel<Row, Column> createFilterTableModel() {
		List<Column> columnIdentifiers =
						unmodifiableList(asList(Column.values()));

		// Implement Columns, providing the table column configuration
		Columns<Row, Column> columns = new Columns<Row, Column>() {

			@Override
			public List<Column> identifiers() {
				return columnIdentifiers;
			}

			@Override
			public Class<?> columnClass(Column identifier) {
				switch (identifier) {
					case STRING:
						return String.class;
					case INTEGER:
						return Integer.class;
					default:
						throw new IllegalArgumentException();
				}
			}

			@Override
			public Object value(Row row, Column identifier) {
				switch (identifier) {
					case STRING:
						return row.stringValue();
					case INTEGER:
						return row.integerValue();
					default:
						throw new IllegalArgumentException();
				}
			}
		};

		// Implement a item supplier responsible for supplying the table row items,
		// without one the table can be populated by adding items manually
		Supplier<Collection<Row>> items = () -> asList(
						new Row("A string", 42),
						new Row("Another string", 43));

		// Create the table model
		FilterTableModel<Row, Column> tableModel =
						FilterTableModel.builder(columns)
										.items(items)
										// if true then the item supplier is called in a
										// background thread when the model is refreshed
										.asyncRefresh(false)
										.build();

		// Populate the model
		tableModel.refresh();

		// Select the first row
		FilterTableSelectionModel<Row> selectionModel = tableModel.selectionModel();
		selectionModel.setSelectedIndex(0);

		// With async refresh enabled
		// tableModel.refreshThen(items ->
		//        selectionModel.setSelectedIndex(0));

		return tableModel;
	}
	// end::filterTableModel[]
}
