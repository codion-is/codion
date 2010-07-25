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
public class AbstractFilteredTableModelTest {

  @Test
  public void testSorting() {
    testModel.refresh();
    testModel.setSortingDirective(0, SortingDirective.DESCENDING);
    assertEquals("e", testModel.getItemAt(0));
    testModel.setSortingDirective(0, SortingDirective.ASCENDING);
    assertEquals("a", testModel.getItemAt(0));
  }

  @Test
  public void testSelection() {
    testModel.refresh();
    testModel.setSelectedItemIndex(2);
    assertEquals(2, testModel.getSelectedIndex());
    testModel.moveSelectionDown();
    assertEquals(3, testModel.getSelectedIndex());
    testModel.moveSelectionUp();
    testModel.moveSelectionUp();
    assertEquals(1, testModel.getSelectedIndex());
    testModel.selectAll();
    assertEquals(5, testModel.getSelectedItems().size());
    testModel.clearSelection();
    assertEquals(0, testModel.getSelectedItems().size());

    testModel.setSelectedItem(testEntities[0]);
    assertEquals("selected item should fit", testEntities[0], testModel.getSelectedItem());
    assertEquals("current index should fit", 0, testModel.getSelectedIndex());
    testModel.addSelectedItemIndex(1);
    assertEquals("selected item should fit", testEntities[0], testModel.getSelectedItem());
    assertEquals("selected indexes should fit", Arrays.asList(0, 1), testModel.getSelectedIndexes());
    assertEquals("current index should fit", 0, testModel.getSelectedIndex());
    testModel.addSelectedItemIndex(4);
    assertEquals("selected indexes should fit", Arrays.asList(0, 1, 4), testModel.getSelectedIndexes());
    testModel.getSelectionModel().removeIndexInterval(1, 4);
    assertEquals("selected indexes should fit", Arrays.asList(0), testModel.getSelectedIndexes());
    assertEquals("current index should fit", 0, testModel.getSelectedIndex());
    testModel.getSelectionModel().clearSelection();
    assertEquals("selected indexes should fit", new ArrayList<Integer>(), testModel.getSelectedIndexes());
    assertEquals("current index should fit", -1, testModel.getSelectionModel().getMinSelectionIndex());
    testModel.addSelectedItemIndexes(Arrays.asList(0, 3, 4));
    assertEquals("selected indexes should fit", Arrays.asList(0, 3, 4), testModel.getSelectedIndexes());
    assertEquals("current index should fit", 0, testModel.getSelectionModel().getMinSelectionIndex());
    testModel.getSelectionModel().removeSelectionInterval(0, 0);
    assertEquals("current index should fit", 3, testModel.getSelectionModel().getMinSelectionIndex());
    testModel.getSelectionModel().removeSelectionInterval(3, 3);
    assertEquals("current index should fit", 4, testModel.getSelectionModel().getMinSelectionIndex());

    testModel.addSelectedItemIndexes(Arrays.asList(0, 3, 4));
    assertEquals("current index should fit", 0, testModel.getSelectionModel().getMinSelectionIndex());
    testModel.getSelectionModel().removeSelectionInterval(3, 3);
    assertEquals("current index should fit", 0, testModel.getSelectionModel().getMinSelectionIndex());
    testModel.getSelectionModel().clearSelection();
    assertEquals("current index should fit", -1, testModel.getSelectionModel().getMinSelectionIndex());

    testModel.addSelectedItemIndexes(Arrays.asList(0, 1, 2, 3, 4));
    assertEquals("current index should fit", 0, testModel.getSelectionModel().getMinSelectionIndex());
    testModel.getSelectionModel().removeSelectionInterval(0, 0);
    assertEquals("current index should fit", 1, testModel.getSelectionModel().getMinSelectionIndex());
    testModel.getSelectionModel().removeSelectionInterval(1, 1);
    assertEquals("current index should fit", 2, testModel.getSelectionModel().getMinSelectionIndex());
    testModel.getSelectionModel().removeSelectionInterval(2, 2);
    assertEquals("current index should fit", 3, testModel.getSelectionModel().getMinSelectionIndex());
    testModel.getSelectionModel().removeSelectionInterval(3, 3);
    assertEquals("current index should fit", 4, testModel.getSelectionModel().getMinSelectionIndex());
    testModel.getSelectionModel().removeSelectionInterval(4, 4);
    assertEquals("current index should fit", -1, testModel.getSelectionModel().getMinSelectionIndex());
  }

  @Test
  public void testSelectionAndSorting() {
    testModel.refresh();
    assertTrue("Model should contain all entities", tableModelContainsAll(testEntities, false, testModel));

    //test selection and filtering together
    testModel.addSelectedItemIndexes(Arrays.asList(3));
    assertEquals("current index should fit", 3, testModel.getSelectionModel().getMinSelectionIndex());

    testModel.getFilterModel(0).setLikeValue("d");
    testModel.getFilterModel(0).setSearchEnabled(false);

    testModel.setSelectedItemIndexes(Arrays.asList(3));
    assertEquals("current index should fit", 3, testModel.getSelectionModel().getMinSelectionIndex());
    assertEquals("current selected item should fit", testEntities[2], testModel.getSelectedItem());

    testModel.setSortingDirective(2, SortingDirective.ASCENDING);
    assertEquals("current selected item should fit", testEntities[2], testModel.getSelectedItem());
    assertEquals("current index should fit", 2,
            testModel.getSelectionModel().getMinSelectionIndex());

    testModel.setSelectedItemIndexes(Arrays.asList(0));
    assertEquals("current selected item should fit", testEntities[0], testModel.getSelectedItem());
    testModel.setSortingDirective(2, SortingDirective.DESCENDING);
    assertEquals("current index should fit", 4,
            testModel.getSelectionModel().getMinSelectionIndex());

    assertEquals("selected indexes should fit", Arrays.asList(4), testModel.getSelectedIndexes());
    assertEquals("current selected item should fit", testEntities[0], testModel.getSelectedItem());
    assertEquals("current index should fit", 4,
            testModel.getSelectionModel().getMinSelectionIndex());
    assertEquals("selected item should fit", testEntities[0], testModel.getSelectedItem());
  }

  @Test
  public void testSelectionAndFiltering() {
    testModel.refresh();
    testModel.addSelectedItemIndexes(Arrays.asList(3));
    assertEquals("current index should fit", 3, testModel.getSelectionModel().getMinSelectionIndex());

    testModel.getFilterModel(0).setLikeValue("d");
    assertEquals("current index should fit", 0,
            testModel.getSelectionModel().getMinSelectionIndex());
    assertEquals("selected indexes should fit", Arrays.asList(0), testModel.getSelectedIndexes());
    testModel.getFilterModel(0).setSearchEnabled(false);
    assertEquals("current index should fit", 0,
            testModel.getSelectionModel().getMinSelectionIndex());
    assertEquals("selected item should fit", testEntities[3], testModel.getSelectedItem());
  }

  @Test
  public void testFiltering() throws Exception {
    testModel.refresh();
    assertTrue("Model should contain all entities", tableModelContainsAll(testEntities, false, testModel));

    //test filters
    testModel.getFilterModel(0).setLikeValue("a");
    assertTrue("filter should be enabled", testModel.getFilterModel(0).isSearchEnabled());
    assertEquals("4 entities should be filtered", 4, testModel.getFilteredItemCount());
    assertFalse("Model should not contain all entities",
            tableModelContainsAll(testEntities, false, testModel));
    assertTrue("Model should contain all entities, including filtered",
            tableModelContainsAll(testEntities, true, testModel));
    testModel.getFilterModel(0).setSearchEnabled(false);
    assertFalse("filter should not be enabled", testModel.getFilterModel(0).isSearchEnabled());

    assertTrue("Model should contain all entities", tableModelContainsAll(testEntities, false, testModel));

    testModel.getFilterModel(0).setLikeValue("t"); // ekki til
    assertTrue("filter should be enabled", testModel.getFilterModel(0).isSearchEnabled());
    assertEquals("all 5 entities should be filtered", 5, testModel.getFilteredItemCount());
    assertFalse("Model should not contain all entities",
            tableModelContainsAll(testEntities, false, testModel));
    assertTrue("Model should contain all entities, including filtered",
            tableModelContainsAll(testEntities, true, testModel));
    testModel.getFilterModel(0).setSearchEnabled(false);
    assertTrue("Model should contain all entities", tableModelContainsAll(testEntities, false, testModel));
    assertFalse("filter should not be enabled", testModel.getFilterModel(0).isSearchEnabled());
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

  private AbstractFilteredTableModel<String, Integer> testModel =
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
