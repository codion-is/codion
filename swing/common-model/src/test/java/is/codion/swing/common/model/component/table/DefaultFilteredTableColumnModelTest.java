/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.component.table;

import is.codion.common.event.EventDataListener;

import org.junit.jupiter.api.Test;

import javax.swing.table.TableColumn;
import java.util.ArrayList;
import java.util.Collection;

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
    assertNotNull(testModel.getTableColumn(0));

    testModel.setColumnVisible(0, false);
    assertFalse(testModel.isColumnVisible(0));
    assertEquals(1, hidden.size());
    assertEquals(1, testModel.getHiddenColumns().size());
    testModel.setColumnVisible(0, true);
    assertTrue(testModel.isColumnVisible(0));
    assertEquals(1, shown.size());

    testModel.removeColumnHiddenListener(hideListener);
    testModel.removeColumnShownListener(showListener);

    assertTrue(testModel.containsColumn(0));
    assertFalse(testModel.containsColumn(1));
  }

  @Test
  void getTableColumnNotFound() {
    FilteredTableColumnModel<Integer> testModel = createTestModel();
    assertThrows(IllegalArgumentException.class, () -> testModel.getTableColumn(42));
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
    TableColumn column0 = new TableColumn(0);
    column0.setIdentifier(0);
    TableColumn column1 = new TableColumn(1);
    column1.setIdentifier(1);
    TableColumn column2 = new TableColumn(2);
    column2.setIdentifier(2);
    TableColumn column3 = new TableColumn(3);
    column3.setIdentifier(3);

    DefaultFilteredTableColumnModel<Integer> columnModel =
            new DefaultFilteredTableColumnModel<>(asList(column0, column1, column2, column3));

    columnModel.setColumns(1, 3);
    assertTrue(columnModel.isColumnVisible(1));
    assertTrue(columnModel.isColumnVisible(3));
    assertFalse(columnModel.isColumnVisible(0));
    assertFalse(columnModel.isColumnVisible(2));
    assertEquals(0, columnModel.getColumnIndex(1));
    assertEquals(1, columnModel.getColumnIndex(3));
    columnModel.setColumns(0, 1);
    assertTrue(columnModel.isColumnVisible(0));
    assertTrue(columnModel.isColumnVisible(1));
    assertFalse(columnModel.isColumnVisible(2));
    assertFalse(columnModel.isColumnVisible(3));
    assertEquals(0, columnModel.getColumnIndex(0));
    assertEquals(1, columnModel.getColumnIndex(1));
    columnModel.setColumns(3);
    assertTrue(columnModel.isColumnVisible(3));
    assertFalse(columnModel.isColumnVisible(2));
    assertFalse(columnModel.isColumnVisible(1));
    assertFalse(columnModel.isColumnVisible(0));
    assertEquals(0, columnModel.getColumnIndex(3));
    columnModel.setColumns();
    assertFalse(columnModel.isColumnVisible(3));
    assertFalse(columnModel.isColumnVisible(2));
    assertFalse(columnModel.isColumnVisible(1));
    assertFalse(columnModel.isColumnVisible(0));
    columnModel.setColumns(3, 2, 1, 0);
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
    TableColumn column0 = new TableColumn(0);
    column0.setIdentifier(0);
    TableColumn column1 = new TableColumn(1);
    column1.setIdentifier(1);
    TableColumn column2 = new TableColumn(2);
    column2.setIdentifier(2);
    TableColumn column3 = new TableColumn(3);
    column3.setIdentifier(3);

    FilteredTableColumnModel<Integer> columnModel =
            new DefaultFilteredTableColumnModel<>(asList(column0, column1, column2, column3));

    columnModel.getLockedState().set(true);
    assertThrows(IllegalStateException.class, () -> columnModel.setColumnVisible(0, false));
    columnModel.getLockedState().set(false);
    columnModel.setColumnVisible(0, false);
    columnModel.getLockedState().set(true);
    assertThrows(IllegalStateException.class, () -> columnModel.setColumnVisible(0, true));
    assertThrows(IllegalStateException.class, () -> columnModel.setColumns(0));

    columnModel.getLockedState().set(false);
    columnModel.setColumns(3, 2, 1);
    columnModel.getLockedState().set(true);
    assertThrows(IllegalStateException.class, () -> columnModel.setColumns(1, 0, 2));
  }

  private static FilteredTableColumnModel<Integer> createTestModel() {
    TableColumn column = new TableColumn(0);
    column.setIdentifier(0);

    return new DefaultFilteredTableColumnModel<>(singletonList(column));
  }
}
