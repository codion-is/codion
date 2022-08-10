/*
 * Copyright (c) 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.swing.common.model.component.table.DefaultFilteredTableModel;
import is.codion.swing.common.model.component.table.FilteredTableModel.ColumnValueProvider;

import org.junit.jupiter.api.Test;

import javax.swing.table.TableColumn;

import static java.util.Collections.singletonList;

public final class SelectColumnsPanelTest {

  @Test
  void test() {
    TableColumn column = new TableColumn(0);
    column.setIdentifier(0);
    column.setHeaderValue("Testing");

    DefaultFilteredTableModel<Object, Integer> tableModel = new DefaultFilteredTableModel<>(singletonList(column),
            new ColumnValueProvider<Object, Integer>() {
              @Override
              public Object value(Object row, Integer columnIdentifier) {
                return null;
              }

              @Override
              public Class<?> columnClass(Integer columnIdentifier) {
                return null;
              }
            });

    new SelectColumnsPanel<>(tableModel.columnModel());
  }
}
