/*
 * Copyright (c) 2013 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.component.table;

import is.codion.common.event.EventDataListener;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;

import static is.codion.swing.common.model.component.table.FilteredTableColumn.filteredTableColumn;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultFilteredTableColumnModelTest {

  @Test
  void testModel() {
    FilteredTableColumnModel<Integer> testModel = createTestModel();
    Collection<Object> hidden = new ArrayList<>();
    Collection<Object> shown = new ArrayList<>();
    EventDataListener<Integer> hideListener = hidden::add;
    EventDataListener<Integer> showListener = shown::add;
    testModel.addColumnHiddenListener(hideListener);
    testModel.addColumnShownListener(showListener);

    assertEquals(1, testModel.getColumnCount());
    assertNotNull(testModel.column(0));

    testModel.setColumnVisible(0, false);
    assertFalse(testModel.isColumnVisible(0));
    assertEquals(1, hidden.size());
    assertEquals(1, testModel.hiddenColumns().size());
    testModel.setColumnVisible(0, true);
    assertTrue(testModel.isColumnVisible(0));
    assertEquals(1, shown.size());

    testModel.removeColumnHiddenListener(hideListener);
    testModel.removeColumnShownListener(showListener);

    assertTrue(testModel.containsColumn(0));
    assertFalse(testModel.containsColumn(1));
  }

  @Test
  void tableColumnNotFound() {
    FilteredTableColumnModel<Integer> testModel = createTestModel();
    assertThrows(IllegalArgumentException.class, () -> testModel.column(42));
  }

  @Test
  void constructorNullColumns() {
    assertThrows(NullPointerException.class, () -> new DefaultFilteredTableColumnModel<>(null));
  }

  @Test
  void constructorNoColumns() {
    assertThrows(IllegalArgumentException.class, () -> new DefaultFilteredTableColumnModel<>(new ArrayList<>()));
  }

  @Test
  void setColumns() {
    FilteredTableColumn<Integer> column0 = filteredTableColumn(0);
    FilteredTableColumn<Integer> column1 = filteredTableColumn(1);
    FilteredTableColumn<Integer> column2 = filteredTableColumn(2);
    FilteredTableColumn<Integer> column3 = filteredTableColumn(3);

    DefaultFilteredTableColumnModel<Integer> columnModel =
            new DefaultFilteredTableColumnModel<>(asList(column0, column1, column2, column3));

    columnModel.setVisibleColumns(1, 3);
    assertTrue(columnModel.isColumnVisible(1));
    assertTrue(columnModel.isColumnVisible(3));
    assertFalse(columnModel.isColumnVisible(0));
    assertFalse(columnModel.isColumnVisible(2));
    assertEquals(0, columnModel.getColumnIndex(1));
    assertEquals(1, columnModel.getColumnIndex(3));
    columnModel.setVisibleColumns(0, 1);
    assertTrue(columnModel.isColumnVisible(0));
    assertTrue(columnModel.isColumnVisible(1));
    assertFalse(columnModel.isColumnVisible(2));
    assertFalse(columnModel.isColumnVisible(3));
    assertEquals(0, columnModel.getColumnIndex(0));
    assertEquals(1, columnModel.getColumnIndex(1));
    columnModel.setVisibleColumns(3);
    assertTrue(columnModel.isColumnVisible(3));
    assertFalse(columnModel.isColumnVisible(2));
    assertFalse(columnModel.isColumnVisible(1));
    assertFalse(columnModel.isColumnVisible(0));
    assertEquals(0, columnModel.getColumnIndex(3));
    columnModel.setVisibleColumns();
    assertFalse(columnModel.isColumnVisible(3));
    assertFalse(columnModel.isColumnVisible(2));
    assertFalse(columnModel.isColumnVisible(1));
    assertFalse(columnModel.isColumnVisible(0));
    columnModel.setVisibleColumns(3, 2, 1, 0);
    assertTrue(columnModel.isColumnVisible(3));
    assertTrue(columnModel.isColumnVisible(2));
    assertTrue(columnModel.isColumnVisible(1));
    assertTrue(columnModel.isColumnVisible(0));
    assertEquals(0, columnModel.getColumnIndex(3));
    assertEquals(1, columnModel.getColumnIndex(2));
    assertEquals(2, columnModel.getColumnIndex(1));
    assertEquals(3, columnModel.getColumnIndex(0));
  }

  @Test
  void lock() {
    FilteredTableColumn<Integer> column0 = filteredTableColumn(0);
    FilteredTableColumn<Integer> column1 = filteredTableColumn(1);
    FilteredTableColumn<Integer> column2 = filteredTableColumn(2);
    FilteredTableColumn<Integer> column3 = filteredTableColumn(3);

    FilteredTableColumnModel<Integer> columnModel =
            new DefaultFilteredTableColumnModel<>(asList(column0, column1, column2, column3));

    columnModel.lockedState().set(true);
    assertThrows(IllegalStateException.class, () -> columnModel.setColumnVisible(0, false));
    columnModel.lockedState().set(false);
    columnModel.setColumnVisible(0, false);
    columnModel.lockedState().set(true);
    assertThrows(IllegalStateException.class, () -> columnModel.setColumnVisible(0, true));
    assertThrows(IllegalStateException.class, () -> columnModel.setVisibleColumns(0));

    columnModel.lockedState().set(false);
    columnModel.setVisibleColumns(3, 2, 1);
    columnModel.lockedState().set(true);
    assertThrows(IllegalStateException.class, () -> columnModel.setVisibleColumns(1, 0, 2));
  }

  private static FilteredTableColumnModel<Integer> createTestModel() {
    return new DefaultFilteredTableColumnModel<>(singletonList(filteredTableColumn(0)));
  }
}
