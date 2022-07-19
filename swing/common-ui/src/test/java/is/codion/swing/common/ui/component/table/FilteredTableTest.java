/*
 * Copyright (c) 2010 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.common.model.table.ColumnFilterModel;
import is.codion.common.model.table.DefaultColumnFilterModel;
import is.codion.swing.common.model.component.table.DefaultFilteredTableModel;

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
    TableColumn column = new TableColumn(0);
    column.setIdentifier(0);
    ColumnFilterModel<List<String>, Integer, String> filterModel =
            new DefaultColumnFilterModel<>(0, String.class, '%');

    TestAbstractFilteredTableModel tableModel = new TestAbstractFilteredTableModel(singletonList(column), singletonList(filterModel)) {
      @Override
      protected Collection<List<String>> refreshItems() {
        return asList(
                singletonList("darri"),
                singletonList("dac"),
                singletonList("dansinn"),
                singletonList("dlabo"));
      }
    };
    FilteredTable<List<String>, Integer, TestAbstractFilteredTableModel> filteredTable =
            new FilteredTable<>(tableModel);
    tableModel.refresh();

    new JScrollPane(filteredTable);

    JTextField searchField = filteredTable.getSearchField();

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
  }

  private static class TestAbstractFilteredTableModel extends DefaultFilteredTableModel<List<String>, Integer> {

    private TestAbstractFilteredTableModel(List<TableColumn> columns,
                                           List<ColumnFilterModel<List<String>, Integer, String>> columnFilterModels) {
      super(columns, new ColumnValueProvider<List<String>, Integer>() {
        @Override
        public Object getValue(List<String> row, Integer columnIdentifier) {
          return row.get(columnIdentifier);
        }

        @Override
        public Class<?> getColumnClass(Integer columnIdentifier) {
          return String.class;
        }
      }, columnFilterModels);
    }

    @Override
    protected Collection<List<String>> refreshItems() {
      return ITEMS;
    }
  }
}
