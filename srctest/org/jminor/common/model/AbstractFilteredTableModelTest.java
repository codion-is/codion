/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import static org.junit.Assert.*;
import org.junit.Test;

import javax.swing.table.DefaultTableColumnModel;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * User: Björn Darri
 * Date: 25.7.2010
 * Time: 13:54:59
 */
public final class AbstractFilteredTableModelTest {

  @Test
  public void clear() {
    tableModel.clear();
    assertTrue(tableModel.getRowCount() == 0);
  }

  @Test
  public void testColumnModel() {
    assertEquals(0, tableModel.getColumnCount());
  }

  @Test
  public void testSorting() {
    tableModel.refresh();
    tableModel.setSortingDirective(0, SortingDirective.DESCENDING);
    assertEquals("e", tableModel.getItemAt(0));
    tableModel.setSortingDirective(0, SortingDirective.ASCENDING);
    assertEquals("a", tableModel.getItemAt(0));
    tableModel.clearSortingState();
  }

  @Test
  public void testSelection() {
    tableModel.refresh();
    tableModel.setSelectedItemIndex(2);
    assertEquals(2, tableModel.getSelectedIndex());
    tableModel.moveSelectionDown();
    assertEquals(3, tableModel.getSelectedIndex());
    tableModel.moveSelectionUp();
    tableModel.moveSelectionUp();
    assertEquals(1, tableModel.getSelectedIndex());
    tableModel.selectAll();
    assertEquals(5, tableModel.getSelectedItems().size());
    tableModel.clearSelection();
    assertEquals(0, tableModel.getSelectedItems().size());

    tableModel.setSelectedItem(testEntities[0]);
    assertEquals("selected item should fit", testEntities[0], tableModel.getSelectedItem());
    assertEquals("current index should fit", 0, tableModel.getSelectedIndex());
    assertEquals(1, tableModel.getSelectionCount());
    assertFalse(tableModel.isSelectionEmpty());
    tableModel.addSelectedItemIndex(1);
    assertEquals("selected item should fit", testEntities[0], tableModel.getSelectedItem());
    assertEquals("selected indexes should fit", Arrays.asList(0, 1), tableModel.getSelectedIndexes());
    assertEquals("current index should fit", 0, tableModel.getSelectedIndex());
    tableModel.addSelectedItemIndex(4);
    assertEquals("selected indexes should fit", Arrays.asList(0, 1, 4), tableModel.getSelectedIndexes());
    tableModel.getSelectionModel().removeIndexInterval(1, 4);
    assertEquals("selected indexes should fit", Arrays.asList(0), tableModel.getSelectedIndexes());
    assertEquals("current index should fit", 0, tableModel.getSelectedIndex());
    tableModel.getSelectionModel().clearSelection();
    assertEquals("selected indexes should fit", new ArrayList<Integer>(), tableModel.getSelectedIndexes());
    assertEquals("current index should fit", -1, tableModel.getSelectionModel().getMinSelectionIndex());
    tableModel.addSelectedItemIndexes(Arrays.asList(0, 3, 4));
    assertEquals("selected indexes should fit", Arrays.asList(0, 3, 4), tableModel.getSelectedIndexes());
    assertEquals("current index should fit", 0, tableModel.getSelectionModel().getMinSelectionIndex());
    assertEquals(3, tableModel.getSelectionCount());
    tableModel.getSelectionModel().removeSelectionInterval(0, 0);
    assertEquals("current index should fit", 3, tableModel.getSelectionModel().getMinSelectionIndex());
    tableModel.getSelectionModel().removeSelectionInterval(3, 3);
    assertEquals("current index should fit", 4, tableModel.getSelectionModel().getMinSelectionIndex());

    tableModel.addSelectedItemIndexes(Arrays.asList(0, 3, 4));
    assertEquals("current index should fit", 0, tableModel.getSelectionModel().getMinSelectionIndex());
    tableModel.getSelectionModel().removeSelectionInterval(3, 3);
    assertEquals("current index should fit", 0, tableModel.getSelectionModel().getMinSelectionIndex());
    tableModel.getSelectionModel().clearSelection();
    assertEquals("current index should fit", -1, tableModel.getSelectionModel().getMinSelectionIndex());

    tableModel.addSelectedItemIndexes(Arrays.asList(0, 1, 2, 3, 4));
    assertEquals("current index should fit", 0, tableModel.getSelectionModel().getMinSelectionIndex());
    tableModel.getSelectionModel().removeSelectionInterval(0, 0);
    assertEquals("current index should fit", 1, tableModel.getSelectionModel().getMinSelectionIndex());
    tableModel.getSelectionModel().removeSelectionInterval(1, 1);
    assertEquals("current index should fit", 2, tableModel.getSelectionModel().getMinSelectionIndex());
    tableModel.getSelectionModel().removeSelectionInterval(2, 2);
    assertEquals("current index should fit", 3, tableModel.getSelectionModel().getMinSelectionIndex());
    tableModel.getSelectionModel().removeSelectionInterval(3, 3);
    assertEquals("current index should fit", 4, tableModel.getSelectionModel().getMinSelectionIndex());
    tableModel.getSelectionModel().removeSelectionInterval(4, 4);
    assertEquals("current index should fit", -1, tableModel.getSelectionModel().getMinSelectionIndex());
  }

  @Test
  public void testSelectionAndSorting() {
    tableModel.refresh();
    assertTrue("Model should contain all entities", tableModelContainsAll(testEntities, false, tableModel));

    //test selection and filtering together
    tableModel.addSelectedItemIndexes(Arrays.asList(3));
    assertEquals("current index should fit", 3, tableModel.getSelectionModel().getMinSelectionIndex());

    tableModel.getFilterModel(0).setLikeValue("d");
    tableModel.getFilterModel(0).setSearchEnabled(false);

    tableModel.setSelectedItemIndexes(Arrays.asList(3));
    assertEquals("current index should fit", 3, tableModel.getSelectionModel().getMinSelectionIndex());
    assertEquals("current selected item should fit", testEntities[2], tableModel.getSelectedItem());

    tableModel.setSortingDirective(2, SortingDirective.ASCENDING);
    assertEquals("current selected item should fit", testEntities[2], tableModel.getSelectedItem());
    assertEquals("current index should fit", 2,
            tableModel.getSelectionModel().getMinSelectionIndex());

    tableModel.setSelectedItemIndexes(Arrays.asList(0));
    assertEquals("current selected item should fit", testEntities[0], tableModel.getSelectedItem());
    tableModel.setSortingDirective(2, SortingDirective.DESCENDING);
    assertEquals("current index should fit", 4,
            tableModel.getSelectionModel().getMinSelectionIndex());

    assertEquals("selected indexes should fit", Arrays.asList(4), tableModel.getSelectedIndexes());
    assertEquals("current selected item should fit", testEntities[0], tableModel.getSelectedItem());
    assertEquals("current index should fit", 4,
            tableModel.getSelectionModel().getMinSelectionIndex());
    assertEquals("selected item should fit", testEntities[0], tableModel.getSelectedItem());
  }

  @Test
  public void testSelectionAndFiltering() {
    tableModel.refresh();
    tableModel.addSelectedItemIndexes(Arrays.asList(3));
    assertEquals("current index should fit", 3, tableModel.getSelectionModel().getMinSelectionIndex());

    tableModel.getFilterModel(0).setLikeValue("d");
    assertEquals("current index should fit", 0,
            tableModel.getSelectionModel().getMinSelectionIndex());
    assertEquals("selected indexes should fit", Arrays.asList(0), tableModel.getSelectedIndexes());
    tableModel.getFilterModel(0).setSearchEnabled(false);
    assertEquals("current index should fit", 0,
            tableModel.getSelectionModel().getMinSelectionIndex());
    assertEquals("selected item should fit", testEntities[3], tableModel.getSelectedItem());
  }

  @Test
  public void testFiltering() throws Exception {
    tableModel.refresh();
    assertTrue("Model should contain all entities", tableModelContainsAll(testEntities, false, tableModel));

    //test filters
    tableModel.getFilterModel(0).setLikeValue("a");
    assertTrue(tableModel.isVisible("a"));
    assertFalse(tableModel.isVisible("b"));
    assertTrue(tableModel.isFiltered("d"));
    assertTrue("filter should be enabled", tableModel.getFilterModel(0).isSearchEnabled());
    assertEquals("4 entities should be filtered", 4, tableModel.getFilteredItemCount());
    assertFalse("Model should not contain all entities",
            tableModelContainsAll(testEntities, false, tableModel));
    assertTrue("Model should contain all entities, including filtered",
            tableModelContainsAll(testEntities, true, tableModel));

    assertTrue(tableModel.getVisibleItems().size() > 0);
    assertTrue(tableModel.getFilteredItems().size() > 0);
    assertTrue(tableModel.getAllItems().size() > 0);

    tableModel.getFilterModel(0).setSearchEnabled(false);
    assertFalse("filter should not be enabled", tableModel.getFilterModel(0).isSearchEnabled());

    assertTrue("Model should contain all entities", tableModelContainsAll(testEntities, false, tableModel));

    tableModel.getFilterModel(0).setLikeValue("t"); // ekki til
    assertTrue("filter should be enabled", tableModel.getFilterModel(0).isSearchEnabled());
    assertEquals("all 5 entities should be filtered", 5, tableModel.getFilteredItemCount());
    assertFalse("Model should not contain all entities",
            tableModelContainsAll(testEntities, false, tableModel));
    assertTrue("Model should contain all entities, including filtered",
            tableModelContainsAll(testEntities, true, tableModel));
    tableModel.getFilterModel(0).setSearchEnabled(false);
    assertTrue("Model should contain all entities", tableModelContainsAll(testEntities, false, tableModel));
    assertFalse("filter should not be enabled", tableModel.getFilterModel(0).isSearchEnabled());
  }

  private boolean tableModelContainsAll(final String[] strings, final boolean includeFiltered,
                                        final AbstractFilteredTableModel<String, Integer> model) {
    for (final String string : strings) {
      if (!model.contains(string, includeFiltered)) {
        return false;
      }
    }

    return true;
  }

  private String[] testEntities = {"a", "b", "c", "d", "e"};

  private AbstractFilteredTableModel<String, Integer> tableModel =
          new AbstractFilteredTableModel<String, Integer>(new DefaultTableColumnModel(),
                  Arrays.asList(new DefaultSearchModel<Integer>(0, Types.VARCHAR, "%"))) {
    protected void doRefresh() {
      addItems(Arrays.asList(testEntities), false);
    }

    public Object getValueAt(final int rowIndex, final int columnIndex) {
      return testEntities[rowIndex];
    }
  };
}
