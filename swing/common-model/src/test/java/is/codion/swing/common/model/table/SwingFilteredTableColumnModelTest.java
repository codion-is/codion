/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.table;

import is.codion.common.event.EventDataListener;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.DefaultColumnConditionModel;

import org.junit.jupiter.api.Test;

import javax.swing.table.TableColumn;
import java.util.ArrayList;
import java.util.Collection;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public class SwingFilteredTableColumnModelTest {

  @Test
  public void testModel() {
    final SwingFilteredTableColumnModel testModel = createTestModel();
    final Collection<Object> hidden = new ArrayList<>();
    final Collection<Object> shown = new ArrayList<>();
    final EventDataListener<Integer> hideListener = hidden::add;
    final EventDataListener<Integer> showListener = shown::add;
    testModel.addColumnHiddenListener(hideListener);
    testModel.addColumnShownListener(showListener);

    assertEquals(1, testModel.getColumnCount());
    assertNotNull(testModel.getTableColumn(0));

    testModel.hideColumn(0);
    assertFalse(testModel.isColumnVisible(0));
    assertEquals(1, hidden.size());
    assertEquals(1, testModel.getHiddenColumns().size());
    testModel.showColumn(0);
    assertTrue(testModel.isColumnVisible(0));
    assertEquals(1, shown.size());

    testModel.removeColumnHiddenListener(hideListener);
    testModel.removeColumnShownListener(showListener);

    assertTrue(testModel.containsColumn(0));
    assertFalse(testModel.containsColumn(1));
  }

  @Test
  public void getTableColumnNotFound() {
    final SwingFilteredTableColumnModel testModel = createTestModel();
    assertThrows(IllegalArgumentException.class, () -> testModel.getTableColumn(42));
  }

  @Test
  public void constructorNullColumns() {
    assertThrows(IllegalArgumentException.class, () -> new SwingFilteredTableColumnModel<>(null, new ArrayList<>()));
  }

  @Test
  public void constructorNoColumns() {
    assertThrows(IllegalArgumentException.class, () -> new SwingFilteredTableColumnModel<>(new ArrayList<>(), new ArrayList<>()));
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

    final SwingFilteredTableColumnModel<String, Integer> columnModel =
            new SwingFilteredTableColumnModel<>(asList(column0, column1, column2, column3), null);

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

  private SwingFilteredTableColumnModel createTestModel() {
    final TableColumn column = new TableColumn(0);
    column.setIdentifier(0);
    final ColumnConditionModel<String, Integer, String> filterModel = new DefaultColumnConditionModel<>(0, String.class, "%");

    return new SwingFilteredTableColumnModel<>(singletonList(column), singletonList(filterModel));
  }
}
