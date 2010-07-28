/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * User: Björn Darri
 * Date: 25.7.2010
 * Time: 13:54:59
 */
public final class AbstractFilteredTableModelTest {

  private String[] testEntities = {"a", "b", "c", "d", "e"};

  private AbstractFilteredTableModel<String, Integer> tableModel;

  @Before
  public void setUp() {
    final TableColumnModel columnModel = new DefaultTableColumnModel();
    final TableColumn column = new TableColumn(0);
    column.setIdentifier(0);
    columnModel.addColumn(column);
    tableModel = new AbstractFilteredTableModel<String, Integer>(columnModel,
            Arrays.asList(new DefaultSearchModel<Integer>(0, Types.VARCHAR, "%"))) {
      @Override
      protected void doRefresh() {
        clear();
        addItems(Arrays.asList(testEntities), false);
      }

      public Object getValueAt(final int rowIndex, final int columnIndex) {
        return getItemAt(rowIndex);
      }
    };
  }

  @Test
  public void refresh() {
    final Collection<Object> started = new ArrayList<Object>();
    final Collection<Object> done = new ArrayList<Object>();
    tableModel.eventRefreshStarted().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        started.add(new Object());
      }
    });
    tableModel.eventRefreshDone().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        done.add(new Object());
      }
    });
    tableModel.refresh();
    assertTrue(tableModel.getRowCount() > 0);
    assertEquals(1, started.size());
    assertEquals(1, done.size());
  }

  @Test
  public void removeItems() {
    final Collection<Object> events = new ArrayList<Object>();
    tableModel.eventTableDataChanged().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        events.add(new Object());
      }
    });
    tableModel.refresh();
    tableModel.getFilterModel(0).setLikeValue("a");
    tableModel.removeItem("b");
    assertEquals(3, events.size());
    assertFalse(tableModel.contains("b", false));
    assertTrue(tableModel.contains("a", true));
    tableModel.removeItem("a");
    assertEquals(4, events.size());
    assertFalse(tableModel.contains("a", true));
    tableModel.removeItems(Arrays.asList("d", "e"));
    assertEquals(4, events.size());//no change when removing filtered items
    assertFalse(tableModel.contains("d", false));
    assertFalse(tableModel.contains("e", false));
  }

  @Test
  public void findNextItemCoordinate() {
    tableModel.refresh();
    Point point = tableModel.findNextItemCoordinate(0, true, "b");
    assertEquals(new Point(0, 1), point);
    point = tableModel.findNextItemCoordinate(point.y, true, "e");
    assertEquals(new Point(0, 4), point);
    point = tableModel.findNextItemCoordinate(point.y, false, "c");
    assertEquals(new Point(0, 2), point);
    point = tableModel.findNextItemCoordinate(0, true, "x");
    assertNull(point);

    tableModel.setSortingDirective(0, SortingDirective.DESCENDING);

    point = tableModel.findNextItemCoordinate(0, true, "b");
    assertEquals(new Point(0, 3), point);
    point = tableModel.findNextItemCoordinate(point.y, false, "e");
    assertEquals(new Point(0, 0), point);

    tableModel.setRegularExpressionSearch(true);
    assertTrue(tableModel.isRegularExpressionSearch());
    point = tableModel.findNextItemCoordinate(0, true, "(?i)B");
    assertEquals(new Point(0, 3), point);

    final FilterCriteria<Object> criteria = new FilterCriteria<Object>() {
      public boolean include(final Object item) {
        return item.equals("a") || item.equals("c");
      }
    };

    tableModel.clearSortingState();
    point = tableModel.findNextItemCoordinate(4, false, criteria);
    assertEquals(new Point(0, 2), point);
    point = tableModel.findNextItemCoordinate(point.y - 1, false, criteria);
    assertEquals(new Point(0, 0), point);
  }

  @Test
  public void clear() {
    tableModel.refresh();
    assertTrue(tableModel.getRowCount() > 0);
    tableModel.clear();
    assertTrue(tableModel.getRowCount() == 0);
  }

  @Test
  public void testColumnModel() {
    final Collection<Object> hidden = new ArrayList<Object>();
    final Collection<Object> shown = new ArrayList<Object>();
    tableModel.eventColumnHidden().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        hidden.add(new Object());
      }
    });
    tableModel.eventColumnShown().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        shown.add(new Object());
      }
    });

    assertNotNull(tableModel.getColumnModel());
    assertEquals(1, tableModel.getColumnCount());
    assertNotNull(tableModel.getTableColumn(0));

    tableModel.setColumnVisible(0, false);
    assertFalse(tableModel.isColumnVisible(0));
    assertEquals(1, hidden.size());
    assertEquals(1, tableModel.getHiddenColumns().size());
    tableModel.setColumnVisible(0, true);
    assertTrue(tableModel.isColumnVisible(0));
    assertEquals(1, shown.size());
  }

  @Test
  public void testSorting() {
    final Collection<Object> started = new ArrayList<Object>();
    final Collection<Object> done = new ArrayList<Object>();
    tableModel.eventSortingStarted().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        started.add(new Object());
      }
    });
    tableModel.eventSortingDone().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        done.add(new Object());
      }
    });

    tableModel.refresh();
    tableModel.setSortingDirective(0, SortingDirective.DESCENDING);
    assertEquals(SortingDirective.DESCENDING, tableModel.getSortingDirective(0));
    assertEquals("e", tableModel.getItemAt(0));
    assertEquals(1, started.size());
    assertEquals(1, done.size());
    tableModel.setSortingDirective(0, SortingDirective.ASCENDING);
    assertEquals(SortingDirective.ASCENDING, tableModel.getSortingDirective(0));
    assertEquals("a", tableModel.getItemAt(0));
    assertEquals(0, tableModel.getSortPriority(0));
    assertEquals(-1, tableModel.getSortPriority(1));
    assertEquals(2, started.size());
    assertEquals(2, done.size());
    tableModel.clearSortingState();

    final List<String> items = new ArrayList<String>();
    items.add(null);
    tableModel.addItems(items, true);
    tableModel.setSortingDirective(0, SortingDirective.ASCENDING);
    assertEquals(0, tableModel.indexOf(null));
    tableModel.setSortingDirective(0, SortingDirective.DESCENDING);
    assertEquals(tableModel.getRowCount() - 1, tableModel.indexOf(null));

    tableModel.refresh();
    items.add(null);
    tableModel.addItems(items, true);
    tableModel.setSortingDirective(0, SortingDirective.ASCENDING);
    assertEquals(0, tableModel.indexOf(null));
    tableModel.setSortingDirective(0, SortingDirective.DESCENDING);
    assertEquals(tableModel.getRowCount() - 2, tableModel.indexOf(null));
  }

  @Test
  public void testSelection() {
    final Collection<Object> events = new ArrayList<Object>();
    tableModel.eventSelectedIndexChanged().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        events.add(new Object());
      }
    });
    tableModel.eventSelectionChanged().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        events.add(new Object());
      }
    });
    tableModel.eventSelectionChangedAdjusting().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        events.add(new Object());
      }
    });

    tableModel.refresh();
    tableModel.setSelectedItemIndex(2);
    assertEquals(2, events.size());
    assertFalse(tableModel.stateSelectionEmpty().isActive());
    assertFalse(tableModel.stateMultipleSelection().isActive());
    assertEquals(2, tableModel.getSelectedIndex());
    tableModel.moveSelectionDown();
    assertEquals(6, events.size());
    assertEquals(3, tableModel.getSelectedIndex());
    tableModel.moveSelectionUp();
    tableModel.moveSelectionUp();
    assertEquals(1, tableModel.getSelectedIndex());

    tableModel.moveSelectionDown();
    tableModel.moveSelectionDown();

    assertEquals(3, tableModel.getSelectedIndex());

    tableModel.setSelectedItemIndex(0);
    tableModel.moveSelectionUp();
    assertEquals(tableModel.getRowCount() - 1, tableModel.getSelectedIndex());

    tableModel.setSelectedItemIndex(tableModel.getRowCount() - 1);
    tableModel.moveSelectionDown();
    assertEquals(0, tableModel.getSelectedIndex());

    tableModel.clearSelection();
    tableModel.moveSelectionUp();
    assertEquals(tableModel.getRowCount() - 1, tableModel.getSelectedIndex());

    tableModel.clearSelection();
    tableModel.moveSelectionDown();
    assertEquals(0, tableModel.getSelectedIndex());

    tableModel.selectAll();
    assertEquals(5, tableModel.getSelectedItems().size());
    tableModel.clearSelection();
    assertTrue(tableModel.stateSelectionEmpty().isActive());
    assertFalse(tableModel.stateMultipleSelection().isActive());
    assertEquals(0, tableModel.getSelectedItems().size());

    tableModel.setSelectedItem(testEntities[0]);
    assertFalse(tableModel.stateMultipleSelection().isActive());
    assertEquals("selected item should fit", testEntities[0], tableModel.getSelectedItem());
    assertEquals("current index should fit", 0, tableModel.getSelectedIndex());
    assertEquals(1, tableModel.getSelectionCount());
    assertFalse(tableModel.isSelectionEmpty());
    tableModel.addSelectedItemIndex(1);
    assertTrue(tableModel.stateMultipleSelection().isActive());
    assertEquals("selected item should fit", testEntities[0], tableModel.getSelectedItem());
    assertEquals("selected indexes should fit", Arrays.asList(0, 1), tableModel.getSelectedIndexes());
    assertEquals("current index should fit", 0, tableModel.getSelectedIndex());
    tableModel.addSelectedItemIndex(4);
    assertTrue(tableModel.stateMultipleSelection().isActive());
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
    final Collection<Object> started = new ArrayList<Object>();
    final Collection<Object> done = new ArrayList<Object>();
    tableModel.eventFilteringStarted().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        started.add(new Object());
      }
    });
    tableModel.eventFilteringDone().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        done.add(new Object());
      }
    });

    tableModel.refresh();
    assertTrue("Model should contain all entities", tableModelContainsAll(testEntities, false, tableModel));
    assertNotNull(tableModel.getFilterCriteria());
    assertEquals(1, tableModel.getFilterModels().size());

    try {
      tableModel.setFilterCriteria(new FilterCriteria<String>() {
        public boolean include(final String item) {
          return false;
        }
      });
      fail();
    }
    catch (UnsupportedOperationException e) {}

    //test filters
    tableModel.getFilterModel(0).setLikeValue("a");
    assertEquals(2, started.size());
    assertEquals(2, done.size());
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
    assertEquals(3, started.size());
    assertEquals(3, done.size());
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

    tableModel.getFilterModel(0).setLikeValue("b");
    final int rowCount = tableModel.getRowCount();
    tableModel.addItems(Arrays.asList("x"), true);
    assertEquals(rowCount, tableModel.getRowCount());
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
}
