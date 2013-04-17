/*
 * Copyright (c) 2004 - 2013, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.table;

import org.jminor.common.model.EventAdapter;
import org.jminor.common.model.EventListener;

import org.junit.Test;

import javax.swing.table.TableColumn;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

public class DefaultFilteredTableColumnModelTest {

  @Test
  public void testModel() {
    final FilteredTableColumnModel<Integer> testModel = createTestModel();
    final Collection<Object> hidden = new ArrayList<Object>();
    final Collection<Object> shown = new ArrayList<Object>();
    final EventListener<Integer> hideListener = new EventAdapter<Integer>() {
      @Override
      public void eventOccurred() {
        hidden.add(new Object());
      }
    };
    final EventListener<Integer> showListener = new EventAdapter<Integer>() {
      @Override
      public void eventOccurred() {
        shown.add(new Object());
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
  }

  @Test(expected = IllegalArgumentException.class)
  public void getTableColumnNotFound() {
    final FilteredTableColumnModel<Integer> testModel = createTestModel();
    testModel.getTableColumn(42);
  }

  private FilteredTableColumnModel<Integer> createTestModel() {
    final TableColumn column = new TableColumn(0);
    column.setIdentifier(0);
    final ColumnSearchModel<Integer> filterModel = new DefaultColumnSearchModel<Integer>(0, Types.VARCHAR, "%");

    return new DefaultFilteredTableColumnModel<Integer>(Arrays.asList(column), Arrays.asList(filterModel));
  }
}
