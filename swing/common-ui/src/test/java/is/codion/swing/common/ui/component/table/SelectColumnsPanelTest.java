/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.swing.common.model.component.table.FilteredTableColumnModel;

import org.junit.jupiter.api.Test;

import javax.swing.table.TableColumn;

import static java.util.Collections.singletonList;

public final class SelectColumnsPanelTest {

  @Test
  void test() {
    TableColumn column = new TableColumn(0);
    column.setIdentifier(0);
    column.setHeaderValue("Testing");

    FilteredTableColumnModel<Object> columnModel =
            FilteredTableColumnModel.create(singletonList(column));

    new SelectColumnsPanel<>(columnModel);
  }
}
