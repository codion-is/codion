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
 * Copyright (c) 2023, Björn Darri Sigurðsson.
 */
package is.codion.manual.swing.common.ui.component.table;

import is.codion.manual.swing.common.model.component.table.FilteredTableModelDemo.TableRow;
import is.codion.swing.common.model.component.table.FilteredTableModel;
import is.codion.swing.common.ui.component.table.FilteredTable;
import is.codion.swing.common.ui.control.Control;

import javax.swing.JTable;

import static is.codion.manual.swing.common.model.component.table.FilteredTableModelDemo.createFilteredTableModel;

final class FilteredTableDemo {

  static void demo() {
    // tag::filteredTable[]
    // See FilteredTableModel example
    FilteredTableModel<TableRow, Integer> tableModel = createFilteredTableModel();

    FilteredTable<TableRow, Integer> filteredTable = FilteredTable.builder(tableModel)
            .doubleClickAction(Control.control(() ->
                    tableModel.selectionModel().selectedItem()
                            .ifPresent(System.out::println)))
            .autoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS)
            .build();
    // end::filteredTable[]
  }
}
