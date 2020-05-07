/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.table;

import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.common.model.table.DefaultColumnConditionModel;
import org.jminor.swing.common.model.table.AbstractFilteredTableModel;
import org.jminor.swing.common.model.table.AbstractTableSortModel;

import org.junit.jupiter.api.Test;

import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.table.TableColumn;
import java.awt.AWTException;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public class FilteredTableTest {

  private static final List<List<String>> ITEMS = asList(
          singletonList("a"), singletonList("b"), singletonList("c"), singletonList("d"), singletonList("e"));

  @Test
  public void constructorNullTableModel() {
    assertThrows(Exception.class, () -> new FilteredTable(null));
  }

  @Test
  public void searchField() throws AWTException {
    final TableColumn column = new TableColumn(0);
    column.setIdentifier(0);
    final ColumnConditionModel<List<String>, Integer> filterModel =
            new DefaultColumnConditionModel<>(0, String.class, "%");

    final TestAbstractFilteredTableModel tableModel = new TestAbstractFilteredTableModel(
            new TestAbstractTableSortModel(singletonList(column)), singletonList(filterModel)) {
      @Override
      protected void doRefresh() {
        clear();
        addItems(asList(singletonList("darri"), singletonList("dac"), singletonList("dansinn"), singletonList("dlabo")));
      }
    };
    final FilteredTable<List<String>, Integer, TestAbstractFilteredTableModel> filteredTable =
            new FilteredTable<>(tableModel);
    tableModel.refresh();

    new JScrollPane(filteredTable);

    final JTextField searchField = filteredTable.getSearchField();

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

    filteredTable.findNext("da");
    assertEquals(0, tableModel.getSelectionModel().getSelectedIndex());
    filteredTable.findNext("da");
    assertEquals(1, tableModel.getSelectionModel().getSelectedIndex());
    filteredTable.findNext("da");
    assertEquals(2, tableModel.getSelectionModel().getSelectedIndex());
    filteredTable.findAndSelectPrevious("da");
    assertEquals(1, tableModel.getSelectionModel().getSelectedIndex());
    assertEquals(2, tableModel.getSelectionModel().getSelectionCount());
    filteredTable.findAndSelectPrevious("da");
    assertEquals(0, tableModel.getSelectionModel().getSelectedIndex());
    assertEquals(3, tableModel.getSelectionModel().getSelectionCount());
    filteredTable.findNext("dat");
  }

  private static class TestAbstractFilteredTableModel extends AbstractFilteredTableModel<List<String>, Integer> {

    private TestAbstractFilteredTableModel(final AbstractTableSortModel<List<String>, Integer> sortModel,
                                           final List<ColumnConditionModel<List<String>, Integer>> columnFilterModels) {
      super(sortModel, columnFilterModels);
    }

    @Override
    protected void doRefresh() {
      clear();
      addItems(ITEMS);
    }

    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex) {
      return getItemAt(rowIndex);
    }
  }

  private static final class TestAbstractTableSortModel extends AbstractTableSortModel<List<String>, Integer> {

    public TestAbstractTableSortModel(final List<TableColumn> columns) {
      super(columns);
    }

    @Override
    public Class getColumnClass(final Integer columnIdentifier) {
      return String.class;
    }

    @Override
    protected Comparable getComparable(final List<String> row, final Integer columnIdentifier) {
      return row.get(columnIdentifier);
    }
  }
}
