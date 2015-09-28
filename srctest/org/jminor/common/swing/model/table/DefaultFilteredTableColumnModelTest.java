/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.swing.model.table;

import org.jminor.common.model.EventInfoListener;

import org.junit.Test;

import javax.swing.table.TableColumn;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.*;

public class DefaultFilteredTableColumnModelTest {

  @Test
  public void testModel() {
    final FilteredTableColumnModel<Integer> testModel = createTestModel();
    final Collection<Object> hidden = new ArrayList<>();
    final Collection<Object> shown = new ArrayList<>();
    final EventInfoListener<Integer> hideListener = new EventInfoListener<Integer>() {
      @Override
      public void eventOccurred(final Integer info) {
        hidden.add(info);
      }
    };
    final EventInfoListener<Integer> showListener = new EventInfoListener<Integer>() {
      @Override
      public void eventOccurred(final Integer info) {
        shown.add(info);
      }
    };
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

  @Test(expected = IllegalArgumentException.class)
  public void getTableColumnNotFound() {
    final FilteredTableColumnModel<Integer> testModel = createTestModel();
    testModel.getTableColumn(42);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNullColumns() {
    new DefaultFilteredTableColumnModel<>(null, new ArrayList<ColumnSearchModel<Integer>>());
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNoColumns() {
    new DefaultFilteredTableColumnModel<>(new ArrayList<TableColumn>(), new ArrayList<ColumnSearchModel<Integer>>());
  }

  @Test
  public void setColumns() {
    final TableColumn column0 = new TableColumn(0);
    column0.setIdentifier(0);
    final TableColumn column1 = new TableColumn(1);
    column1.setIdentifier(1);
    final TableColumn column2 = new TableColumn(2);
    column2.setIdentifier(2);
    final TableColumn column3 = new TableColumn(3);
    column3.setIdentifier(3);

    final DefaultFilteredTableColumnModel<Integer> columnModel =
            new DefaultFilteredTableColumnModel<>(Arrays.asList(column0, column1, column2, column3), null);

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

  private FilteredTableColumnModel<Integer> createTestModel() {
    final TableColumn column = new TableColumn(0);
    column.setIdentifier(0);
    final ColumnSearchModel<Integer> filterModel = new DefaultColumnSearchModel<>(0, Types.VARCHAR, "%");

    return new DefaultFilteredTableColumnModel<>(Collections.singletonList(column), Collections.singletonList(filterModel));
  }
}
