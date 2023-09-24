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
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.swing.common.model.component.table.FilteredTableColumn;
import is.codion.swing.common.model.component.table.FilteredTableModel;

import org.junit.jupiter.api.Test;

import static is.codion.swing.common.model.component.table.FilteredTableColumn.filteredTableColumn;
import static java.util.Collections.singletonList;

public final class ColumnSelectionPanelTest {

  @Test
  void test() {
    FilteredTableColumn<Integer> column = filteredTableColumn(0);
    column.setHeaderValue("Testing");

    FilteredTableModel<Object, Integer> tableModel =
            FilteredTableModel.<Object, Integer>builder(() -> singletonList(column), (row, columnIdentifier) -> null)
                    .build();

    new ColumnSelectionPanel<>(tableModel.columnModel());
  }
}
