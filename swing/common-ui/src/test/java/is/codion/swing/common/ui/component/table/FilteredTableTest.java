/*
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.swing.common.model.component.table.FilteredTableColumn;
import is.codion.swing.common.model.component.table.FilteredTableModel;

import org.junit.jupiter.api.Test;

import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public class FilteredTableTest {

  @Test
  void builderNullTableModel() {
    assertThrows(Exception.class, () -> FilteredTable.builder(null));
  }

  @Test
  void searchField() {
    FilteredTableColumn<Integer> column = FilteredTableColumn.builder(0)
            .columnClass(String.class)
            .build();

    FilteredTableModel<List<String>, Integer> tableModel =
            FilteredTableModel.<List<String>, Integer>builder(() -> singletonList(column), List::get)
                    .itemSupplier(() -> asList(
                            singletonList("darri"),
                            singletonList("dac"),
                            singletonList("dansinn"),
                            singletonList("dlabo")))
                    .build();

    FilteredTable<List<String>, Integer> filteredTable = FilteredTable.builder(tableModel).build();
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
}
