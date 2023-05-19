/*
 * Copyright (c) 2013 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.component.table;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static javax.swing.ListSelectionModel.*;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultFilteredTableSelectionModelTest {

  private final FilteredTableSelectionModel<String> testModel;

  public DefaultFilteredTableSelectionModelTest() {
    List<String> data = asList("A", "B", "C");
    FilteredTableColumn<Integer> column = FilteredTableColumn.builder(0)
            .columnClass(String.class)
            .build();
    FilteredTableModel<String, Integer> tableModel = new DefaultFilteredTableModel<String, Integer>(
            singletonList(column), (row, columnIdentifier) -> row) {
      @Override
      protected Collection<String> refreshItems() {
        return data;
      }
    };
    tableModel.refresh();

    testModel = tableModel.selectionModel();
  }

  @Test
  void test() {
    testModel.setSelectedIndex(0);
    assertTrue(testModel.isSelectedItem("A"));
    testModel.clearSelection();
    assertFalse(testModel.isSelectedItem("A"));
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
