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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.manual.swing.common.ui.component.table;

import is.codion.manual.swing.common.model.component.table.FilterTableModelDemo.Person;
import is.codion.swing.common.model.component.table.FilterTableModel;
import is.codion.swing.common.ui.component.table.FilterTable;
import is.codion.swing.common.ui.component.table.FilterTableColumn;
import is.codion.swing.common.ui.component.table.FilterTableColumnModel;
import is.codion.swing.common.ui.component.table.FilterTableSearchModel;
import is.codion.swing.common.ui.component.table.FilterTableSearchModel.RowColumn;
import is.codion.swing.common.ui.control.Control;

import javax.swing.JTable;
import java.util.List;

import static is.codion.manual.swing.common.model.component.table.FilterTableModelDemo.createFilterTableModel;

final class FilterTableDemo {

	static void demo() {
		// tag::filterTable[]
		// See FilterTableModel example
		FilterTableModel<Person, String> tableModel = createFilterTableModel();

		// Create the columns, specifying the identifier and the model index
		List<FilterTableColumn<String>> columns = List.of(
						FilterTableColumn.builder(Person.NAME, 0).build(),
						FilterTableColumn.builder(Person.AGE, 1).build());

		FilterTable<Person, String> table =
						FilterTable.builder(tableModel, columns)
										.doubleClickAction(Control.command(() ->
														tableModel.selection().item().optional()
																		.ifPresent(System.out::println)))
										.autoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS)
										.build();
		// end::filterTable[]
	}

	static void search(FilterTable<Person, String> table) {
		// tag::search[]
		FilterTableSearchModel search = table.search();

		// Search for the value "43" in the table
		search.predicate().set(value -> value.equals("43"));

		RowColumn searchResult = search.results().current().get();

		System.out.println(searchResult); // row: 1, column: 1

		// Print the next available result
		search.results().next().ifPresent(System.out::println);
		// end::search[]
	}

	static void columns(FilterTable<Person, String> table) {
		// tag::columns[]
		FilterTableColumnModel<String> columns = table.columnModel();

		// Reorder the columns
		columns.visible().set(Person.AGE, Person.NAME);

		// Print hidden columns when they change
		columns.hidden().addConsumer(System.out::println);

		// Hide the age column
		columns.visible(Person.AGE).set(false);

		// Only show the age column
		columns.visible().set(Person.AGE);

		// Reset columns to their default location and visibility
		columns.reset();
		// end::columns[]
	}

	static void export(FilterTable<Person, String> table) {
		// tag::export[]
		String tabDelimited = table.export()
						// Tab delimited
						.delimiter('\t')
						// Include hidden columns
						.hidden(true)
						// Include header
						.header(true)
						// Only selected rows
						.selected(true)
						.get();
		// end::export[]
	}
}
