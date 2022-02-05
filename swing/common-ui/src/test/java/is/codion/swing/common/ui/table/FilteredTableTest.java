/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.table;

import is.codion.common.model.table.ColumnFilterModel;
import is.codion.common.model.table.DefaultColumnFilterModel;
import is.codion.swing.common.model.table.AbstractFilteredTableModel;
import is.codion.swing.common.model.table.AbstractTableSortModel;
import is.codion.swing.common.model.table.SwingFilteredTableColumnModel;

import org.junit.jupiter.api.Test;

import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.table.TableColumn;
import java.awt.AWTException;
import java.util.Collection;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public class FilteredTableTest {

  private static final Collection<List<String>> ITEMS = asList(
          singletonList("a"), singletonList("b"), singletonList("c"), singletonList("d"), singletonList("e"));

  @Test
  void constructorNullTableModel() {
    assertThrows(Exception.class, () -> new FilteredTable<>(null));
  }

  @Test
  void searchField() throws AWTException {
    final TableColumn column = new TableColumn(0);
    column.setIdentifier(0);
    final ColumnFilterModel<List<String>, Integer, String> filterModel =
            new DefaultColumnFilterModel<>(0, String.class, "%");

    final TestAbstractFilteredTableModel tableModel = new TestAbstractFilteredTableModel(singletonList(column),
            new TestAbstractTableSortModel(), singletonList(filterModel)) {
      @Override
      protected Collection<List<String>> refreshItems() {
        return asList(singletonList("darri"), singletonList("dac"), singletonList("dansinn"), singletonList("dlabo"));
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

    private TestAbstractFilteredTableModel(final List<TableColumn> columns, final AbstractTableSortModel<List<String>, Integer> sortModel,
                                           final List<ColumnFilterModel<List<String>, Integer, String>> columnFilterModels) {
      super(new SwingFilteredTableColumnModel<>(columns), sortModel, columnFilterModels);
    }

    @Override
    protected Collection<List<String>> refreshItems() {
      return ITEMS;
    }

    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex) {
      return getItemAt(rowIndex);
    }
  }

  private static final class TestAbstractTableSortModel extends AbstractTableSortModel<List<String>, Integer> {

    @Override
    public Class<String> getColumnClass(final Integer columnIdentifier) {
      return String.class;
    }

    @Override
    protected Comparable<String> getComparable(final List<String> row, final Integer columnIdentifier) {
      return row.get(columnIdentifier);
    }
  }
}
