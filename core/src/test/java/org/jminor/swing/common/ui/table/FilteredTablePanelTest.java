/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.table;

import org.jminor.swing.common.model.table.AbstractFilteredTableModelTest;

import org.junit.Test;

import javax.swing.JTextField;
import java.awt.AWTException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FilteredTablePanelTest {

  @Test
  public void test() {
    final FilteredTablePanel<String, Integer> panel =
            new FilteredTablePanel<>(AbstractFilteredTableModelTest.createTestModel());
    assertNotNull(panel.getJTable());
    assertNotNull(panel.getSearchField());
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNullTableModel() {
    new FilteredTablePanel<String, Integer>(null, null);
  }

  @Test
  public void searchField() throws AWTException {
    final AbstractFilteredTableModelTest.TestAbstractFilteredTableModel tableModel = AbstractFilteredTableModelTest.createTestModel();
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

    searchField.setText("");
    tableModel.getSelectionModel().clearSelection();

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
}
