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
package is.codion.manual.swing.common.ui.component.table;

import is.codion.manual.swing.common.model.component.table.FilterTableModelDemo.Person;
import is.codion.manual.swing.common.model.component.table.FilterTableModelDemo.PersonColumn;
import is.codion.swing.common.model.component.table.FilterTableModel;
import is.codion.swing.common.ui.component.table.FilterTable;
import is.codion.swing.common.ui.component.table.FilterTableColumn;
import is.codion.swing.common.ui.component.table.FilterTableSearchModel;
import is.codion.swing.common.ui.control.Control;

import javax.swing.JTable;
import java.util.List;

import static is.codion.manual.swing.common.model.component.table.FilterTableModelDemo.createFilterTableModel;

final class FilterTableDemo {

	static void demo() {
		// tag::filterTable[]
		// See FilterTableModel example
		FilterTableModel<Person, PersonColumn> tableModel = createFilterTableModel();

		List<FilterTableColumn<PersonColumn>> columns = List.of(
						FilterTableColumn.builder(PersonColumn.NAME)
										.headerValue("Name")
										.build(),
						FilterTableColumn.builder(PersonColumn.AGE)
										.headerValue("Age")
										.build());

		FilterTable<Person, PersonColumn> filterTable = FilterTable.builder(tableModel, columns)
						.doubleClickAction(Control.command(() ->
										tableModel.selection().item().optional()
														.ifPresent(System.out::println)))
						.autoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS)
						.build();

		// Search for the value "43" in the table
		FilterTableSearchModel searchModel = filterTable.searchModel();
		searchModel.searchPredicate().set(value -> value.equals("43"));

		FilterTableSearchModel.RowColumn searchResult = searchModel.currentResult().get();
		System.out.println(searchResult); // row: 1, column: 1
		// end::filterTable[]
	}
}
