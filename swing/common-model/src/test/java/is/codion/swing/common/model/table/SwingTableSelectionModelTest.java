/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.table;

import org.junit.jupiter.api.Test;

import javax.swing.table.TableColumn;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static javax.swing.ListSelectionModel.*;
import static org.junit.jupiter.api.Assertions.*;

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
        addItems(data);
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

  @Test
  public void selectionMode() {
    assertFalse(testModel.getSingleSelectionModeState().get());
    testModel.setSelectionMode(SINGLE_SELECTION);
    assertTrue(testModel.getSingleSelectionModeState().get());
    testModel.setSelectionMode(SINGLE_INTERVAL_SELECTION);
    assertFalse(testModel.getSingleSelectionModeState().get());
    testModel.setSelectionMode(MULTIPLE_INTERVAL_SELECTION);
    assertFalse(testModel.getSingleSelectionModeState().get());
    testModel.setSelectionMode(SINGLE_SELECTION);
    assertTrue(testModel.getSingleSelectionModeState().get());
    testModel.getSingleSelectionModeState().set(false);
    assertEquals(MULTIPLE_INTERVAL_SELECTION, testModel.getSelectionMode());
    testModel.setSelectionMode(SINGLE_SELECTION);
    assertTrue(testModel.getSingleSelectionModeState().get());
    assertEquals(SINGLE_SELECTION, testModel.getSelectionMode());
  }
}
