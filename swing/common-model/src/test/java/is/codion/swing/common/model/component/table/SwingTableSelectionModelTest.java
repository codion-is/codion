/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.component.table;

import org.junit.jupiter.api.Test;

import javax.swing.table.TableColumn;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static javax.swing.ListSelectionModel.*;
import static org.junit.jupiter.api.Assertions.*;

public class SwingTableSelectionModelTest {

  private final SwingTableSelectionModel<String> testModel;

  public SwingTableSelectionModelTest() {
    List<String> data = asList("A", "B", "C");
    TableColumn column = new TableColumn(0);
    column.setIdentifier(0);
    AbstractTableSortModel<String, Integer> sortModel = new AbstractTableSortModel<String, Integer>() {
      @Override
      public Class<String> getColumnClass(Integer columnIdentifier) {
        return String.class;
      }

      @Override
      protected Object getColumnValue(String row, Integer columnIdentifier) {
        return row;
      }
    };
    AbstractFilteredTableModel<String, Integer> tableModel = new AbstractFilteredTableModel<String, Integer>(
            new SwingFilteredTableColumnModel<>(singletonList(column)), sortModel) {
      @Override
      protected Collection<String> refreshItems() {
        return data;
      }

      @Override
      public Object getValueAt(int rowIndex, int columnIndex) {
        return data.get(rowIndex);
      }

      @Override
      public boolean allowSelectionChange() {
        String selected = getSelectionModel().getSelectedItem();
        return !"C".equals(selected);
      }
    };
    tableModel.refresh();

    testModel = tableModel.getSelectionModel();
  }

  @Test
  void vetoSelectionChange() {
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
  void selectionMode() {
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

  @Test
  void events() {
    AtomicInteger emptyCounter = new AtomicInteger();
    testModel.getSelectionEmptyObserver().addListener(emptyCounter::incrementAndGet);
    testModel.setSelectedIndex(0);
    assertEquals(1, emptyCounter.get());
    testModel.addSelectedIndex(1);
    assertEquals(1, emptyCounter.get());
    testModel.setSelectedIndexes(asList(1, 2));
    assertEquals(1, emptyCounter.get());
    testModel.addSelectionInterval(0, 1);
    assertEquals(1, emptyCounter.get());
    testModel.moveSelectionDown();
    assertEquals(1, emptyCounter.get());
    testModel.clearSelection();
    assertEquals(2, emptyCounter.get());
  }
}
