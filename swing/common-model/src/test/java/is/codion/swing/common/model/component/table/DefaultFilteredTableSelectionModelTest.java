/*
 * Copyright (c) 2013 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.component.table;

import is.codion.swing.common.model.component.table.FilteredTableModel.ColumnValueProvider;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static is.codion.swing.common.model.component.table.FilteredTableColumn.filteredTableColumn;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static javax.swing.ListSelectionModel.*;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultFilteredTableSelectionModelTest {

  private final FilteredTableSelectionModel<String> testModel;

  public DefaultFilteredTableSelectionModelTest() {
    List<String> data = asList("A", "B", "C");
    FilteredTableColumn<Integer> column = filteredTableColumn(0);
    FilteredTableModel<String, Integer> tableModel = new DefaultFilteredTableModel<String, Integer>(
            singletonList(column), new ColumnValueProvider<String, Integer>() {
      @Override
      public Object value(String row, Integer columnIdentifier) {
        return row;
      }

      @Override
      public Class<?> columnClass(Integer columnIdentifier) {
        return String.class;
      }
    }) {
      @Override
      protected Collection<String> refreshItems() {
        return data;
      }

      @Override
      public boolean allowSelectionChange() {
        String selected = selectionModel().getSelectedItem();
        return !"C".equals(selected);
      }
    };
    tableModel.refresh();

    testModel = tableModel.selectionModel();
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
    assertFalse(testModel.singleSelectionModeState().get());
    testModel.setSelectionMode(SINGLE_SELECTION);
    assertTrue(testModel.singleSelectionModeState().get());
    testModel.setSelectionMode(SINGLE_INTERVAL_SELECTION);
    assertFalse(testModel.singleSelectionModeState().get());
    testModel.setSelectionMode(MULTIPLE_INTERVAL_SELECTION);
    assertFalse(testModel.singleSelectionModeState().get());
    testModel.setSelectionMode(SINGLE_SELECTION);
    assertTrue(testModel.singleSelectionModeState().get());
    testModel.singleSelectionModeState().set(false);
    assertEquals(MULTIPLE_INTERVAL_SELECTION, testModel.getSelectionMode());
    testModel.setSelectionMode(SINGLE_SELECTION);
    assertTrue(testModel.singleSelectionModeState().get());
    assertEquals(SINGLE_SELECTION, testModel.getSelectionMode());
  }

  @Test
  void events() {
    AtomicInteger emptyCounter = new AtomicInteger();
    testModel.selectionEmptyObserver().addListener(emptyCounter::incrementAndGet);
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
