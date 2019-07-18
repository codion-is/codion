/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.table;

import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.common.model.table.DefaultColumnConditionModel;
import org.jminor.swing.common.model.table.AbstractFilteredTableModel;
import org.jminor.swing.common.model.table.AbstractTableSortModel;

import org.junit.jupiter.api.Test;

import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableColumn;
import java.awt.AWTException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FilteredTablePanelTest {

  private static final String[] ITEMS = {"a", "b", "c", "d", "e"};

  @Test
  public void test() {
    final FilteredTablePanel<String, Integer> panel =
            new FilteredTablePanel<>(createTestModel());
    assertNotNull(panel.getJTable());
    assertNotNull(panel.getSearchField());
  }

  @Test
  public void constructorNullTableModel() {
    assertThrows(NullPointerException.class, () -> new FilteredTablePanel<String, Integer>((AbstractFilteredTableModel) null, null));
  }

  @Test
  public void constructorNullTable() {
    assertThrows(Exception.class, () -> new FilteredTablePanel<String, Integer>((JTable) null, null));
  }

  @Test
  public void searchField() throws AWTException {
    final TestAbstractFilteredTableModel tableModel = createTestModel();
    final FilteredTablePanel<String, Integer> panel = new FilteredTablePanel<>(tableModel);

    tableModel.addItemsAt(Arrays.asList("darri", "dac", "dansinn", "dlabo"), 0);

    final JTextField searchField = panel.getSearchField();

    searchField.setText("d");
    assertEquals(0, tableModel.getSelectionModel().getSelectedIndex());
    searchField.setText("da");
    assertEquals(0, tableModel.getSelectionModel().getSelectedIndex());
    searchField.setText("dac");
    assertEquals(1, tableModel.getSelectionModel().getSelectedIndex());
    searchField.setText("dar");
    assertEquals(0, tableModel.getSelectionModel().getSelectedIndex());
    searchField.setText("dan");
    assertEquals(2, tableModel.getSelectionModel().getSelectedIndex());
    searchField.setText("dl");
    assertEquals(3, tableModel.getSelectionModel().getSelectedIndex());
    searchField.setText("darri");
    assertEquals(0, tableModel.getSelectionModel().getSelectedIndex());
    searchField.setText("dac");
    assertEquals(1, tableModel.getSelectionModel().getSelectedIndex());
    searchField.setText("dl");
    assertEquals(3, tableModel.getSelectionModel().getSelectedIndex());
    searchField.setText("dans");
    assertEquals(2, tableModel.getSelectionModel().getSelectedIndex());
    searchField.setText("dansu");
    assertTrue(tableModel.getSelectionModel().isSelectionEmpty());

    searchField.setText("");

    panel.findNextValue(false, true, "da");
    assertEquals(0, tableModel.getSelectionModel().getSelectedIndex());
    panel.findNextValue(false, true, "da");
    assertEquals(1, tableModel.getSelectionModel().getSelectedIndex());
    panel.findNextValue(false, true, "da");
    assertEquals(2, tableModel.getSelectionModel().getSelectedIndex());
    panel.findNextValue(true, false, "da");
    assertEquals(1, tableModel.getSelectionModel().getSelectedIndex());
    assertEquals(2, tableModel.getSelectionModel().getSelectionCount());
    panel.findNextValue(true, false, "da");
    assertEquals(0, tableModel.getSelectionModel().getSelectedIndex());
    assertEquals(3, tableModel.getSelectionModel().getSelectionCount());
    panel.findNextValue(false, true, "dat");
  }

  public static TestAbstractFilteredTableModel createTestModel() {
    return createTestModel(null);
  }

  public static TestAbstractFilteredTableModel createTestModel(final Comparator<String> customComparator) {
    final TableColumn column = new TableColumn(0);
    column.setIdentifier(0);
    final ColumnConditionModel<Integer> filterModel = new DefaultColumnConditionModel<>(0, String.class, "%");
    return new TestAbstractFilteredTableModel(new AbstractTableSortModel<String, Integer>(Collections.singletonList(column)) {
      @Override
      public Class getColumnClass(final Integer columnIdentifier) {
        return String.class;
      }

      @Override
      protected Comparable getComparable(final String rowObject, final Integer columnIdentifier) {
        return rowObject;
      }

      @Override
      protected Comparator initializeColumnComparator(final Integer columnIdentifier) {
        if (customComparator != null) {
          return customComparator;
        }

        return super.initializeColumnComparator(columnIdentifier);
      }
    }, Collections.singletonList(filterModel));
  }

  public static class TestAbstractFilteredTableModel extends AbstractFilteredTableModel<String, Integer> {

    private TestAbstractFilteredTableModel(final AbstractTableSortModel<String, Integer> sortModel,
                                           final List<ColumnConditionModel<Integer>> columnFilterModels) {
      super(sortModel, columnFilterModels);
    }

    @Override
    protected void doRefresh() {
      clear();
      addItems(Arrays.asList(ITEMS), true, false);
    }

    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex) {
      return getItemAt(rowIndex);
    }

    public void addItemsAt(final List<String> items, final int index) {
      addItems(items, index, false);
    }
  }
}
