/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.model.table;

import org.jminor.common.model.table.SelectionModel;

import org.junit.Test;

import javax.swing.table.TableColumn;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

//todo move rename refactor
public class DefaultTableSelectionModelTest {

  private final SelectionModel<String> testModel;

  public DefaultTableSelectionModelTest() {
    final List<String> data = Arrays.asList("A", "B", "C");
    final TableColumn column = new TableColumn(0);
    column.setIdentifier(0);
    final TableSortModel<String, Integer> sortModel = new AbstractTableSortModel<String, Integer>(Collections.singletonList(column)) {
      @Override
      public Class getColumnClass(final Integer columnIdentifier) {
        return String.class;
      }

      @Override
      protected Comparable getComparable(final String rowObject, final Integer columnIdentifier) {
        return rowObject;
      }
    };
    final FilteredTableModel<String, Integer> tableModel = new AbstractFilteredTableModel<String, Integer>(sortModel, null) {
      @Override
      protected void doRefresh() {
        clear();
        addItems(data, true);
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

    testModel = tableModel.getSelectionModel();
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
