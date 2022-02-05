/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.table;

import is.codion.common.event.EventDataListener;

import org.junit.jupiter.api.Test;

import javax.swing.table.TableColumn;
import java.util.ArrayList;
import java.util.Collection;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public class SwingFilteredTableColumnModelTest {

  @Test
  void testModel() {
    final SwingFilteredTableColumnModel<Integer> testModel = createTestModel();
    final Collection<Object> hidden = new ArrayList<>();
    final Collection<Object> shown = new ArrayList<>();
    final EventDataListener<Integer> hideListener = hidden::add;
    final EventDataListener<Integer> showListener = shown::add;
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
    final SwingFilteredTableColumnModel<Integer> testModel = createTestModel();
    assertThrows(IllegalArgumentException.class, () -> testModel.getTableColumn(42));
  }

  @Test
  void constructorNullColumns() {
    assertThrows(NullPointerException.class, () -> new SwingFilteredTableColumnModel<>(null));
  }

  @Test
  void constructorNoColumns() {
    assertThrows(IllegalArgumentException.class, () -> new SwingFilteredTableColumnModel<>(new ArrayList<>()));
  }

  @Test
  void setColumns() {
    final TableColumn column0 = new TableColumn(0);
    column0.setIdentifier(0);
    final TableColumn column1 = new TableColumn(1);
    column1.setIdentifier(1);
    final TableColumn column2 = new TableColumn(2);
    column2.setIdentifier(2);
    final TableColumn column3 = new TableColumn(3);
    column3.setIdentifier(3);

    final SwingFilteredTableColumnModel<Integer> columnModel =
            new SwingFilteredTableColumnModel<>(asList(column0, column1, column2, column3));

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
    final TableColumn column0 = new TableColumn(0);
    column0.setIdentifier(0);
    final TableColumn column1 = new TableColumn(1);
    column1.setIdentifier(1);
    final TableColumn column2 = new TableColumn(2);
    column2.setIdentifier(2);
    final TableColumn column3 = new TableColumn(3);
    column3.setIdentifier(3);

    final SwingFilteredTableColumnModel<Integer> columnModel =
            new SwingFilteredTableColumnModel<>(asList(column0, column1, column2, column3));

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

  private static SwingFilteredTableColumnModel<Integer> createTestModel() {
    final TableColumn column = new TableColumn(0);
    column.setIdentifier(0);

    return new SwingFilteredTableColumnModel<>(singletonList(column));
  }
}
