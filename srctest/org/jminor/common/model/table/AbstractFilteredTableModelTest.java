/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.table;

import org.jminor.common.model.EventAdapter;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.FilterCriteria;

import org.junit.Before;
import org.junit.Test;

import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.Point;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.*;

/**
 * User: Björn Darri
 * Date: 25.7.2010
 * Time: 13:54:59
 */
public final class AbstractFilteredTableModelTest {

  private static final String[] ITEMS = {"a", "b", "c", "d", "e"};

  private TestAbstractFilteredTableModel tableModel;

  private static class TestAbstractFilteredTableModel extends AbstractFilteredTableModel<String, Integer> {

    private TestAbstractFilteredTableModel(final TableColumnModel columnModel, final List<? extends ColumnSearchModel<Integer>> columnFilterModels) {
      super(columnModel, columnFilterModels);
    }

    @Override
    protected void doRefresh() {
      clear();
      addItems(Arrays.asList(ITEMS), false);
    }

    @Override
    protected Class getColumnClass(final Integer columnIdentifier) {
      return String.class;
    }

    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex) {
      return getItemAt(rowIndex);
    }

    @Override
    protected Comparable getComparable(final String rowObject, final Integer columnIdentifier) {
      return rowObject;
    }

    public void addItemsAt(final List<String> items, final int index) {
      addItems(items, index);
    }
  }

  public static TestAbstractFilteredTableModel createTestModel() {
    return createTestModel(null);
  }

  public static TestAbstractFilteredTableModel createTestModel(final Comparator<String> customComparator) {
    final TableColumnModel columnModel = new DefaultTableColumnModel();
    final TableColumn column = new TableColumn(0);
    column.setIdentifier(0);
    columnModel.addColumn(column);
    final ColumnSearchModel<Integer> filterModel = new DefaultColumnSearchModel<Integer>(0, Types.VARCHAR, "%");
    return new TestAbstractFilteredTableModel(columnModel, Arrays.asList(filterModel)) {
      @Override
      protected Comparator initializeColumnComparator(final Integer columnIdentifier) {
        if (customComparator != null) {
          return customComparator;
        }

        return super.initializeColumnComparator(columnIdentifier);
      }
    };
  }

  @Before
  public void setUp() {
    tableModel = createTestModel();
  }

  @Test
  public void addItemsAt() {
    tableModel.refresh();
    tableModel.addItemsAt(Arrays.asList("f", "g"), 2);
    assertEquals(2, tableModel.indexOf("f"));
    assertEquals(3, tableModel.indexOf("g"));
    assertEquals(4, tableModel.indexOf("c"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void nullColumnModel() {
    new AbstractFilteredTableModel<String, Integer>(null, null) {
      @Override
      protected void doRefresh() {}

      @Override
      public Object getValueAt(final int rowIndex, final int columnIndex) {
        return null;
      }

      @Override
      protected Comparable getComparable(final String rowObject, final Integer columnIdentifier) {
        return null;
      }
    };
  }

  @Test
  public void refresh() {
    final Collection<Object> started = new ArrayList<Object>();
    final Collection<Object> done = new ArrayList<Object>();
    final Collection<Object> cleared = new ArrayList<Object>();
    final EventListener startListener = new EventAdapter() {
      @Override
      public void eventOccurred() {
        started.add(new Object());
      }
    };
    final EventListener doneListener = new EventAdapter() {
      @Override
      public void eventOccurred() {
        done.add(new Object());
      }
    };
    final EventListener clearedListener = new EventAdapter() {
      @Override
      public void eventOccurred() {
        cleared.add(new Object());
      }
    };
    tableModel.addRefreshStartedListener(startListener);
    tableModel.addRefreshDoneListener(doneListener);
    tableModel.addTableModelClearedListener(clearedListener);
    tableModel.refresh();
    assertTrue(tableModel.getRowCount() > 0);
    assertEquals(1, started.size());
    assertEquals(1, done.size());
    assertEquals(1, cleared.size());
    tableModel.removeRefreshStartedListener(startListener);
    tableModel.removeRefreshDoneListener(doneListener);
    tableModel.removeTableModelClearedListener(clearedListener);
  }

  @Test
  public void removeItems() {
    final Collection<Object> events = new ArrayList<Object>();
    final EventListener listener = new EventAdapter() {
      @Override
      public void eventOccurred() {
        events.add(new Object());
      }
    };
    tableModel.addTableDataChangedListener(listener);
    tableModel.refresh();
    assertEquals(1, events.size());
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
    tableModel.removeTableDataChangedListener(listener);
  }

  @Test
  public void removeItemsRange() {
    final Collection<Object> events = new ArrayList<Object>();
    final EventListener listener = new EventAdapter() {
      @Override
      public void eventOccurred() {
        events.add(new Object());
      }
    };
    tableModel.addTableDataChangedListener(listener);
    tableModel.refresh();
    assertEquals(1, events.size());
    tableModel.removeItems(1, 3);
    assertEquals(2, events.size());
    assertTrue(tableModel.contains("a", true));
    assertFalse(tableModel.contains("b", true));
    assertFalse(tableModel.contains("c", true));
    assertTrue(tableModel.contains("d", true));
    assertTrue(tableModel.contains("e", true));
    tableModel.removeTableDataChangedListener(listener);
  }

  @Test
  public void findNextItemCoordinate() {
    final class Row implements Comparable<Row> {

      private final int id;
      private final String value;

      Row(final int id, final String value) {
        this.id = id;
        this.value = value;
      }

      @Override
      public int compareTo(final Row o) {
        return value.compareTo(o.value);
      }
    }

    final TableColumnModel columnModel = new DefaultTableColumnModel();
    final TableColumn columnId = new TableColumn(0);
    columnId.setIdentifier(0);
    columnModel.addColumn(columnId);
    final TableColumn columnValue = new TableColumn(1);
    columnValue.setIdentifier(1);
    columnModel.addColumn(columnValue);

    final Row[] items = new Row[] {new Row(0, "a"), new Row(1, "b"), new Row(2, "c"), new Row(3, "d"), new Row(4, "e")};

    final AbstractFilteredTableModel<Row, Integer> testModel = new AbstractFilteredTableModel<Row, Integer>(columnModel, null) {
      @Override
      protected void doRefresh() {
        clear();
        addItems(Arrays.asList(items), false);
      }

      @Override
      protected Class getColumnClass(final Integer columnIdentifier) {
        if (columnIdentifier == 0) {
          return Integer.class;
        }

        return String.class;
      }

      @Override
      public Object getValueAt(final int rowIndex, final int columnIndex) {
        final Row row = getItemAt(rowIndex);
        if (columnIndex == 0) {
          return row.id;
        }

        return row.value;
      }

      @Override
      protected Comparable getComparable(final Row rowObject, final Integer columnIdentifier) {
        if (columnIdentifier == 0) {
          return rowObject.id;
        }

        return rowObject.value;
      }
    };

    testModel.refresh();
    Point point = testModel.findNextItemCoordinate(0, true, "b");
    assertEquals(new Point(1, 1), point);
    point = testModel.findNextItemCoordinate(point.y, true, "e");
    assertEquals(new Point(1, 4), point);
    point = testModel.findNextItemCoordinate(point.y, false, "c");
    assertEquals(new Point(1, 2), point);
    point = testModel.findNextItemCoordinate(0, true, "x");
    assertNull(point);

    testModel.setSortingDirective(1, SortingDirective.DESCENDING, false);

    point = testModel.findNextItemCoordinate(0, true, "b");
    assertEquals(new Point(1, 3), point);
    point = testModel.findNextItemCoordinate(point.y, false, "e");
    assertEquals(new Point(1, 0), point);

    testModel.setRegularExpressionSearch(true);
    assertTrue(testModel.isRegularExpressionSearch());
    point = testModel.findNextItemCoordinate(0, true, "(?i)B");
    assertEquals(new Point(1, 3), point);

    FilterCriteria<Object> criteria = new FilterCriteria<Object>() {
      @Override
      public boolean include(final Object item) {
        return item.equals("b") || item.equals("e");
      }
    };

    point = testModel.findNextItemCoordinate(4, false, criteria);
    assertEquals(new Point(1, 3), point);
    point = testModel.findNextItemCoordinate(point.y - 1, false, criteria);
    assertEquals(new Point(1, 0), point);

    testModel.setSortingDirective(1, SortingDirective.ASCENDING, false);
    columnModel.moveColumn(1, 0);

    testModel.refresh();
    point = testModel.findNextItemCoordinate(0, true, "b");
    assertEquals(new Point(0, 1), point);
    point = testModel.findNextItemCoordinate(point.y, true, "e");
    assertEquals(new Point(0, 4), point);
    point = testModel.findNextItemCoordinate(point.y, false, "c");
    assertEquals(new Point(0, 2), point);
    point = testModel.findNextItemCoordinate(0, true, "x");
    assertNull(point);

    testModel.setSortingDirective(0, SortingDirective.DESCENDING, false);

    point = testModel.findNextItemCoordinate(0, true, "b");
    assertEquals(new Point(0, 3), point);
    point = testModel.findNextItemCoordinate(point.y, false, "e");
    assertEquals(new Point(0, 0), point);

    testModel.setRegularExpressionSearch(true);
    assertTrue(testModel.isRegularExpressionSearch());
    point = testModel.findNextItemCoordinate(0, true, "(?i)B");
    assertEquals(new Point(0, 3), point);

    criteria = new FilterCriteria<Object>() {
      @Override
      public boolean include(final Object item) {
        return item.equals("b") || item.equals("e");
      }
    };

    point = testModel.findNextItemCoordinate(4, false, criteria);
    assertEquals(new Point(0, 3), point);
    point = testModel.findNextItemCoordinate(point.y - 1, false, criteria);
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
    final EventListener<Integer> hideListener = new EventAdapter<Integer>() {
      @Override
      public void eventOccurred() {
        hidden.add(new Object());
      }
    };
    final EventListener<Integer> showListener = new EventAdapter<Integer>() {
      @Override
      public void eventOccurred() {
        shown.add(new Object());
      }
    };
    tableModel.addColumnHiddenListener(hideListener);
    tableModel.addColumnShownListener(showListener);

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

    tableModel.removeColumnHiddenListener(hideListener);
    tableModel.removeColumnShownListener(showListener);
  }

  @Test
  public void customSorting() {
    final AbstractFilteredTableModel<String, Integer> tableModel = createTestModel(new Comparator<String>() {
      @Override
      public int compare(final String o1, final String o2) {
        return o2.compareTo(o1);//reverse order
      }
    });
    tableModel.refresh();
    tableModel.setSortingDirective(0, SortingDirective.ASCENDING, false);
    assertEquals("e", tableModel.getItemAt(0));
    tableModel.setSortingDirective(0, SortingDirective.DESCENDING, false);
    assertEquals("a", tableModel.getItemAt(0));
  }

  @Test
  public void testSorting() {
    final Collection<Object> actionsPerformed = new ArrayList<Object>();
    final EventListener listener = new EventAdapter() {
      @Override
      public void eventOccurred() {
        actionsPerformed.add(new Object());
      }
    };
    tableModel.addSortingListener(listener);

    tableModel.refresh();
    tableModel.setSortingDirective(0, SortingDirective.DESCENDING, false);
    assertEquals(SortingDirective.DESCENDING, tableModel.getSortingDirective(0));
    assertEquals("e", tableModel.getItemAt(0));
    assertEquals(1, actionsPerformed.size());
    tableModel.setSortingDirective(0, SortingDirective.ASCENDING, false);
    assertEquals(SortingDirective.ASCENDING, tableModel.getSortingDirective(0));
    assertEquals("a", tableModel.getItemAt(0));
    assertEquals(0, tableModel.getSortingPriority(0));
    assertEquals(2, actionsPerformed.size());

    tableModel.setSortingDirective(0, SortingDirective.DESCENDING, false);
    tableModel.refresh();
    assertEquals("a", tableModel.getItemAt(4));
    assertEquals("e", tableModel.getItemAt(0));
    tableModel.setSortingDirective(0, SortingDirective.ASCENDING, false);

    final List<String> items = new ArrayList<String>();
    items.add(null);
    tableModel.addItems(items, true);
    tableModel.setSortingDirective(0, SortingDirective.ASCENDING, false);
    assertEquals(0, tableModel.indexOf(null));
    tableModel.setSortingDirective(0, SortingDirective.DESCENDING, false);
    assertEquals(tableModel.getRowCount() - 1, tableModel.indexOf(null));

    tableModel.refresh();
    items.add(null);
    tableModel.addItems(items, true);
    tableModel.setSortingDirective(0, SortingDirective.ASCENDING, false);
    assertEquals(0, tableModel.indexOf(null));
    tableModel.setSortingDirective(0, SortingDirective.DESCENDING, false);
    assertEquals(tableModel.getRowCount() - 2, tableModel.indexOf(null));
    tableModel.setSortingDirective(0, SortingDirective.UNSORTED, false);
    tableModel.removeSortingListener(listener);
  }

  @Test(expected = IllegalArgumentException.class)
  public void getSortingDirectiveInvalidColumn() {
    tableModel.getSortingDirective(1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void getSortingPriorityInvalidColumn() {
    tableModel.getSortingPriority(1);
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void setSelectedIndexesNegative() {
    final Collection<Integer> indexes = new ArrayList<Integer>();
    indexes.add(1);
    indexes.add(-1);
    tableModel.setSelectedIndexes(indexes);
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void setSelectedIndexesOutOfBounds() {
    final Collection<Integer> indexes = new ArrayList<Integer>();
    indexes.add(1);
    indexes.add(10);
    tableModel.setSelectedIndexes(indexes);
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void setSelectedIndexNegative() {
    tableModel.setSelectedIndex(-1);
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void setSelectedIndexOutOfBounds() {
    tableModel.setSelectedIndex(10);
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void addSelectedIndexNegative() {
    tableModel.addSelectedIndex(-1);
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void addSelectedIndexOutOfBounds() {
    tableModel.addSelectedIndex(10);
  }

  @Test
  public void testSelection() {
    final Collection<Object> events = new ArrayList<Object>();
    final EventListener listener = new EventAdapter() {
      @Override
      public void eventOccurred() {
        events.add(new Object());
      }
    };
    tableModel.addSelectedIndexListener(listener);
    tableModel.addSelectionChangedListener(listener);

    assertFalse(tableModel.getSingleSelectionObserver().isActive());
    assertTrue(tableModel.getSelectionEmptyObserver().isActive());
    assertFalse(tableModel.getMultipleSelectionObserver().isActive());

    tableModel.refresh();
    tableModel.setSelectedIndex(2);
    assertEquals(2, events.size());
    assertTrue(tableModel.getSingleSelectionObserver().isActive());
    assertFalse(tableModel.getSelectionEmptyObserver().isActive());
    assertFalse(tableModel.getMultipleSelectionObserver().isActive());
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

    tableModel.setSelectedIndex(0);
    tableModel.moveSelectionUp();
    assertEquals(tableModel.getRowCount() - 1, tableModel.getSelectedIndex());

    tableModel.setSelectedIndex(tableModel.getRowCount() - 1);
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
    assertFalse(tableModel.getSingleSelectionObserver().isActive());
    assertTrue(tableModel.getSelectionEmptyObserver().isActive());
    assertFalse(tableModel.getMultipleSelectionObserver().isActive());
    assertEquals(0, tableModel.getSelectedItems().size());

    tableModel.setSelectedItem(ITEMS[0]);
    assertFalse(tableModel.getMultipleSelectionObserver().isActive());
    assertEquals("selected item should fit", ITEMS[0], tableModel.getSelectedItem());
    assertEquals("current index should fit", 0, tableModel.getSelectedIndex());
    assertEquals(1, tableModel.getSelectionCount());
    assertFalse(tableModel.isSelectionEmpty());
    tableModel.addSelectedIndex(1);
    assertTrue(tableModel.getMultipleSelectionObserver().isActive());
    assertEquals("selected item should fit", ITEMS[0], tableModel.getSelectedItem());
    assertEquals("selected indexes should fit", Arrays.asList(0, 1), tableModel.getSelectedIndexes());
    assertEquals("current index should fit", 0, tableModel.getSelectedIndex());
    tableModel.addSelectedIndex(4);
    assertTrue(tableModel.getMultipleSelectionObserver().isActive());
    assertEquals("selected indexes should fit", Arrays.asList(0, 1, 4), tableModel.getSelectedIndexes());
    tableModel.getSelectionModel().removeIndexInterval(1, 4);
    assertEquals("selected indexes should fit", Arrays.asList(0), tableModel.getSelectedIndexes());
    assertEquals("current index should fit", 0, tableModel.getSelectedIndex());
    tableModel.getSelectionModel().clearSelection();
    assertEquals("selected indexes should fit", new ArrayList<Integer>(), tableModel.getSelectedIndexes());
    assertEquals("current index should fit", -1, tableModel.getSelectionModel().getMinSelectionIndex());
    tableModel.addSelectedIndexes(Arrays.asList(0, 3, 4));
    assertEquals("selected indexes should fit", Arrays.asList(0, 3, 4), tableModel.getSelectedIndexes());
    assertEquals("current index should fit", 0, tableModel.getSelectionModel().getMinSelectionIndex());
    assertEquals(3, tableModel.getSelectionCount());
    tableModel.getSelectionModel().removeSelectionInterval(0, 0);
    assertEquals("current index should fit", 3, tableModel.getSelectionModel().getMinSelectionIndex());
    tableModel.getSelectionModel().removeSelectionInterval(3, 3);
    assertEquals("current index should fit", 4, tableModel.getSelectionModel().getMinSelectionIndex());

    tableModel.addSelectedIndexes(Arrays.asList(0, 3, 4));
    assertEquals("current index should fit", 0, tableModel.getSelectionModel().getMinSelectionIndex());
    tableModel.getSelectionModel().removeSelectionInterval(3, 3);
    assertEquals("current index should fit", 0, tableModel.getSelectionModel().getMinSelectionIndex());
    tableModel.getSelectionModel().clearSelection();
    assertEquals("current index should fit", -1, tableModel.getSelectionModel().getMinSelectionIndex());

    tableModel.addSelectedIndexes(Arrays.asList(0, 1, 2, 3, 4));
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

    tableModel.addSelectedItem(ITEMS[0]);
    assertEquals(1, tableModel.getSelectionCount());
    assertEquals("current index should fit", 0, tableModel.getSelectionModel().getMinSelectionIndex());
    tableModel.addSelectedItems(Arrays.asList(ITEMS[1], ITEMS[2]));
    assertEquals(3, tableModel.getSelectionCount());
    assertEquals("current index should fit", 0, tableModel.getSelectionModel().getMinSelectionIndex());
    tableModel.addSelectedItem(ITEMS[4]);
    assertEquals(4, tableModel.getSelectionCount());
    assertEquals("current index should fit", 0, tableModel.getSelectionModel().getMinSelectionIndex());

    tableModel.clearSelection();
    assertNull(tableModel.getSelectedItem());

    tableModel.removeSelectedIndexListener(listener);
    tableModel.removeSelectionChangedListener(listener);
  }

  @Test
  public void testSelectionAndSorting() {
    tableModel.refresh();
    assertTrue("Model should contain all entities", tableModelContainsAll(ITEMS, false, tableModel));

    //test selection and filtering together
    tableModel.addSelectedIndexes(Arrays.asList(3));
    assertEquals("current index should fit", 3, tableModel.getSelectionModel().getMinSelectionIndex());

    tableModel.getFilterModel(0).setLikeValue("d");
    tableModel.getFilterModel(0).setEnabled(false);

    tableModel.setSelectedIndexes(Arrays.asList(3));
    assertEquals("current index should fit", 3, tableModel.getSelectionModel().getMinSelectionIndex());
    assertEquals("current selected item should fit", ITEMS[2], tableModel.getSelectedItem());

    tableModel.setSortingDirective(0, SortingDirective.ASCENDING, false);
    assertEquals("current selected item should fit", ITEMS[2], tableModel.getSelectedItem());
    assertEquals("current index should fit", 2,
            tableModel.getSelectionModel().getMinSelectionIndex());

    tableModel.setSelectedIndexes(Arrays.asList(0));
    assertEquals("current selected item should fit", ITEMS[0], tableModel.getSelectedItem());
    tableModel.setSortingDirective(0, SortingDirective.DESCENDING, false);
    assertEquals("current index should fit", 4,
            tableModel.getSelectionModel().getMinSelectionIndex());

    assertEquals("selected indexes should fit", Arrays.asList(4), tableModel.getSelectedIndexes());
    assertEquals("current selected item should fit", ITEMS[0], tableModel.getSelectedItem());
    assertEquals("current index should fit", 4,
            tableModel.getSelectionModel().getMinSelectionIndex());
    assertEquals("selected item should fit", ITEMS[0], tableModel.getSelectedItem());
  }

  @Test
  public void testSelectionAndFiltering() {
    tableModel.refresh();
    tableModel.addSelectedIndexes(Arrays.asList(3));
    assertEquals("current index should fit", 3, tableModel.getSelectionModel().getMinSelectionIndex());

    tableModel.getFilterModel(0).setLikeValue("d");
    assertEquals("current index should fit", 0,
            tableModel.getSelectionModel().getMinSelectionIndex());
    assertEquals("selected indexes should fit", Arrays.asList(0), tableModel.getSelectedIndexes());
    tableModel.getFilterModel(0).setEnabled(false);
    assertEquals("current index should fit", 0,
            tableModel.getSelectionModel().getMinSelectionIndex());
    assertEquals("selected item should fit", ITEMS[3], tableModel.getSelectedItem());
  }

  @Test(expected = UnsupportedOperationException.class)
  public void setFilterCriteria() {
    tableModel.setFilterCriteria(new FilterCriteria<String>() {
      @Override
      public boolean include(final String item) {
        return false;
      }
    });
  }

  @Test
  public void testFiltering() throws Exception {
    final Collection<Object> done = new ArrayList<Object>();
    final EventListener listener = new EventAdapter() {
      @Override
      public void eventOccurred() {
        done.add(new Object());
      }
    };
    tableModel.addFilteringListener(listener);

    tableModel.refresh();
    assertTrue("Model should contain all entities", tableModelContainsAll(ITEMS, false, tableModel));
    assertNotNull(tableModel.getFilterCriteria());

    //test filters
    tableModel.getFilterModel(0).setLikeValue("a");
    assertEquals(2, done.size());
    assertTrue(tableModel.isVisible("a"));
    assertFalse(tableModel.isVisible("b"));
    assertTrue(tableModel.isFiltered("d"));
    assertTrue("filter should be enabled", tableModel.getFilterModel(0).isEnabled());
    assertEquals("4 entities should be filtered", 4, tableModel.getFilteredItemCount());
    assertFalse("Model should not contain all entities",
            tableModelContainsAll(ITEMS, false, tableModel));
    assertTrue("Model should contain all entities, including filtered",
            tableModelContainsAll(ITEMS, true, tableModel));

    assertTrue(tableModel.getVisibleItems().size() > 0);
    assertTrue(tableModel.getFilteredItems().size() > 0);
    assertTrue(tableModel.getAllItems().size() > 0);

    tableModel.getFilterModel(0).setEnabled(false);
    assertEquals(3, done.size());
    assertFalse("filter should not be enabled", tableModel.getFilterModel(0).isEnabled());

    assertTrue("Model should contain all entities", tableModelContainsAll(ITEMS, false, tableModel));

    tableModel.getFilterModel(0).setLikeValue("t"); // ekki til
    assertTrue("filter should be enabled", tableModel.getFilterModel(0).isEnabled());
    assertEquals("all 5 entities should be filtered", 5, tableModel.getFilteredItemCount());
    assertFalse("Model should not contain all entities",
            tableModelContainsAll(ITEMS, false, tableModel));
    assertTrue("Model should contain all entities, including filtered",
            tableModelContainsAll(ITEMS, true, tableModel));
    tableModel.getFilterModel(0).setEnabled(false);
    assertTrue("Model should contain all entities", tableModelContainsAll(ITEMS, false, tableModel));
    assertFalse("filter should not be enabled", tableModel.getFilterModel(0).isEnabled());

    tableModel.getFilterModel(0).setLikeValue("b");
    final int rowCount = tableModel.getRowCount();
    tableModel.addItems(Arrays.asList("x"), true);
    assertEquals(rowCount, tableModel.getRowCount());

    tableModel.removeFilteringListener(listener);
  }

  private static boolean tableModelContainsAll(final String[] strings, final boolean includeFiltered,
                                               final AbstractFilteredTableModel<String, Integer> model) {
    for (final String string : strings) {
      if (!model.contains(string, includeFiltered)) {
        return false;
      }
    }

    return true;
  }
}
