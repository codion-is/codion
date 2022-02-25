/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.table;

import is.codion.swing.common.model.table.SwingFilteredTableColumnModel;

import org.junit.jupiter.api.Test;

import javax.swing.table.TableColumn;

import static java.util.Collections.singletonList;

public final class SelectColumnsPanelTest {

  @Test
  void test() {
    TableColumn column = new TableColumn(0);
    column.setIdentifier(0);
    column.setHeaderValue("Testing");

    SwingFilteredTableColumnModel<Object> columnModel =
            new SwingFilteredTableColumnModel<>(singletonList(column));

    new SelectColumnsPanel<>(columnModel);
  }
}
