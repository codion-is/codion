/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.model.table;

import org.junit.jupiter.api.Test;

import javax.swing.table.TableColumn;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SwingTableSelectionModelTest {

  private final SwingTableSelectionModel<String> testModel;

  public SwingTableSelectionModelTest() {
    final List<String> data = asList("A", "B", "C");
    final TableColumn column = new TableColumn(0);
    column.setIdentifier(0);
    final AbstractTableSortModel<String, Integer> sortModel = new AbstractTableSortModel<String, Integer>(singletonList(column)) {
      @Override
      public Class getColumnClass(final Integer columnIdentifier) {
        return String.class;
      }

      @Override
      protected Comparable getComparable(final String row, final Integer columnIdentifier) {
        return row;
      }
    };
    final AbstractFilteredTableModel<String, Integer> tableModel = new AbstractFilteredTableModel<String, Integer>(sortModel) {
      @Override
      protected void doRefresh() {
        clear();
        addItems(data, true, false);
      }

      @Override
      public Object getValueAt(final int rowIndex, final int columnIndex) {
        return data.get(rowIndex);
      }

      @Override
      public boolean allowSelectionChange() {
        final String selected = getSelectionModel().getSelectedItem();
        return !"C".equals(selected);
      }
    };
    tableModel.refresh();

    testModel = (SwingTableSelectionModel<String>) tableModel.getSelectionModel();
  }

  @Test
  public void vetoSelectionChange() {
    testModel.setSelectedIndex(0);
    assertEquals("A", testModel.getSelectedItem());
    testModel.setSelectedIndex(1);
    assertEquals("B", testModel.getSelectedItem());
    testModel.setSelectedIndex(2);
    assertEquals("C", testModel.getSelectedItem());
    testModel.setSelectedIndex(1);
    assertEquals("C", testModel.getSelectedItem());
    testModel.setSelectedIndex(0);
    assertEquals("C", testModel.getSelectedItem());
  }
}
