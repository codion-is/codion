/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
