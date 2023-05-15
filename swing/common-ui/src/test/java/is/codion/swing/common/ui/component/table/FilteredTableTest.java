/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.common.model.table.ColumnConditionModel;
import is.codion.swing.common.model.component.table.DefaultFilteredTableModel;
import is.codion.swing.common.model.component.table.FilteredTableColumn;

import org.junit.jupiter.api.Test;

import javax.swing.JScrollPane;
import javax.swing.JTextField;
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
  void builderNullTableModel() {
    assertThrows(Exception.class, () -> FilteredTable.builder(null));
  }

  @Test
  void searchField() throws AWTException {
    FilteredTableColumn<Integer> column = FilteredTableColumn.builder(0)
            .columnClass(String.class)
            .build();
    ColumnConditionModel<Integer, String> filterModel = ColumnConditionModel.builder(0, String.class).build();

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
    FilteredTable<TestAbstractFilteredTableModel, List<String>, Integer> filteredTable = FilteredTable.builder(tableModel).build();
    tableModel.refresh();

    new JScrollPane(filteredTable);

    JTextField searchField = filteredTable.searchField();

    searchField.setText("d");
    assertEquals(0, tableModel.selectionModel().getSelectedIndex());
    searchField.setText("da");
    assertEquals(0, tableModel.selectionModel().getSelectedIndex());
    searchField.setText("dac");
    assertEquals(1, tableModel.selectionModel().getSelectedIndex());
    searchField.setText("dar");
    assertEquals(0, tableModel.selectionModel().getSelectedIndex());
    searchField.setText("dan");
    assertEquals(2, tableModel.selectionModel().getSelectedIndex());
    searchField.setText("dl");
    assertEquals(3, tableModel.selectionModel().getSelectedIndex());
    searchField.setText("darri");
    assertEquals(0, tableModel.selectionModel().getSelectedIndex());
    searchField.setText("dac");
    assertEquals(1, tableModel.selectionModel().getSelectedIndex());
    searchField.setText("dl");
    assertEquals(3, tableModel.selectionModel().getSelectedIndex());
    searchField.setText("dans");
    assertEquals(2, tableModel.selectionModel().getSelectedIndex());
    searchField.setText("dansu");
    assertTrue(tableModel.selectionModel().isSelectionEmpty());

    searchField.setText("");
  }

  private static class TestAbstractFilteredTableModel extends DefaultFilteredTableModel<List<String>, Integer> {

    private TestAbstractFilteredTableModel(List<FilteredTableColumn<Integer>> columns,
                                           List<ColumnConditionModel<Integer, ?>> columnFilterModels) {
      super(columns, List::get, columnFilterModels);
    }

    @Override
    protected Collection<List<String>> refreshItems() {
      return ITEMS;
    }
  }
}
