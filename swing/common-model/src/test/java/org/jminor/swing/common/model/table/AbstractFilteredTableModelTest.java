/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.model.table;

import org.jminor.common.EventDataListener;
import org.jminor.common.EventListener;
import org.jminor.common.Events;
import org.jminor.common.model.FilterCondition;
import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.common.model.table.DefaultColumnConditionModel;
import org.jminor.common.model.table.RowColumn;
import org.jminor.common.model.table.SortingDirective;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.table.TableColumn;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * User: Björn Darri
 * Date: 25.7.2010
 * Time: 13:54:59
 */
public final class AbstractFilteredTableModelTest {

  private static final String[] ITEMS = {"a", "b", "c", "d", "e"};

  private TestAbstractFilteredTableModel tableModel;

  public static class TestAbstractFilteredTableModel extends AbstractFilteredTableModel<String, Integer> {

    private TestAbstractFilteredTableModel(final AbstractTableSortModel<String, Integer> sortModel,
                                           final List<ColumnConditionModel<Integer>> columnFilterModels) {
      super(sortModel, columnFilterModels);
    }

    @Override
    protected void doRefresh() {
      clear();
      addItems(Arrays.asList(ITEMS), false);
    }

    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex) {
      return getItemAt(rowIndex);
    }

    public void addItemsAt(final List<String> items, final int index) {
      addItems(items, index);
    }
  }

  public static TestAbstractFilteredTableModel createTestModel() {
    return createTestModel(null);
  }

  public static TestAbstractFilteredTableModel createTestModel(final Comparator<String> customComparator) {
    final TableColumn column = new TableColumn(0);
    column.setIdentifier(0);
    final ColumnConditionModel<Integer> filterModel = new DefaultColumnConditionModel<>(0, Types.VARCHAR, "%");
    return new TestAbstractFilteredTableModel(new AbstractTableSortModel<String, Integer>(Collections.singletonList(column)) {
      @Override
      public Class getColumnClass(final Integer columnIdentifier) {
        return String.class;
      }

      @Override
      protected Comparable getComparable(final String rowObject, final Integer columnIdentifier) {
        return rowObject;
      }

      @Override
      protected Comparator initializeColumnComparator(final Integer columnIdentifier) {
        if (customComparator != null) {
          return customComparator;
        }

        return super.initializeColumnComparator(columnIdentifier);
      }
    }, Collections.singletonList(filterModel));
  }

  @BeforeEach
  public void setUp() {
    tableModel = createTestModel();
  }

  @Test
  public void getColumnCount() {
    assertEquals(1, tableModel.getColumnCount());
  }

  @Test
  public void filterContents() {
    tableModel.refresh();
    tableModel.setFilterCondition(item -> !item.equals("b") && !item.equals("f"));
    assertFalse(tableModel.contains("b", false));
    assertTrue(tableModel.contains("b", true));
    tableModel.addItemsAt(Collections.singletonList("f"), 0);
    tableModel.getSortModel().setSortingDirective(0, SortingDirective.DESCENDING, false);
    assertFalse(tableModel.contains("f", false));
    assertTrue(tableModel.contains("f", true));
    tableModel.setFilterCondition(null);
    assertTrue(tableModel.contains("b", false));
    assertTrue(tableModel.contains("f", false));
  }

  @Test
  public void addItemsAt() {
    tableModel.refresh();
    tableModel.addItemsAt(Arrays.asList("f", "g"), 2);
    assertEquals(2, tableModel.indexOf("f"));
    assertEquals(3, tableModel.indexOf("g"));
    assertEquals(4, tableModel.indexOf("c"));
  }

  @Test
  public void nullColumnModel() {
    assertThrows(NullPointerException.class, () -> new AbstractFilteredTableModel<String, Integer>(null, null) {
      @Override
      protected void doRefresh() {}

      @Override
      public Object getValueAt(final int rowIndex, final int columnIndex) {
        return null;
      }
    });
  }

  @Test
  public void refreshEvents() {
    final AtomicInteger started = new AtomicInteger();
    final AtomicInteger done = new AtomicInteger();
    final AtomicInteger cleared = new AtomicInteger();
    final EventListener startListener = started::incrementAndGet;
    final EventListener doneListener = done::incrementAndGet;
    final EventListener clearedListener = cleared::incrementAndGet;
    tableModel.addRefreshStartedListener(startListener);
    tableModel.addRefreshDoneListener(doneListener);
    tableModel.addTableModelClearedListener(clearedListener);
    tableModel.refresh();
    assertTrue(tableModel.getRowCount() > 0);
    assertEquals(1, started.get());
    assertEquals(1, done.get());
    assertEquals(1, cleared.get());
    tableModel.removeRefreshStartedListener(startListener);
    tableModel.removeRefreshDoneListener(doneListener);
    tableModel.removeTableModelClearedListener(clearedListener);
  }

  @Test
  public void removeItems() {
    final AtomicInteger events = new AtomicInteger();
    final EventListener listener = events::incrementAndGet;
    tableModel.addTableDataChangedListener(listener);
    tableModel.refresh();
    assertEquals(1, events.get());
    tableModel.getColumnModel().getColumnFilterModel(0).setLikeValue("a");
    tableModel.removeItem("b");
    assertEquals(3, events.get());
    assertFalse(tableModel.contains("b", false));
    assertTrue(tableModel.contains("a", true));
    tableModel.removeItem("a");
    assertEquals(4, events.get());
    assertFalse(tableModel.contains("a", true));
    tableModel.removeItems(Arrays.asList("d", "e"));
    assertEquals(4, events.get());//no change when removing filtered items
    assertFalse(tableModel.contains("d", false));
    assertFalse(tableModel.contains("e", false));
    tableModel.removeTableDataChangedListener(listener);
  }

  @Test
  public void removeItemsRange() {
    final AtomicInteger events = new AtomicInteger();
    final EventListener listener = events::incrementAndGet;
    tableModel.addTableDataChangedListener(listener);
    tableModel.refresh();
    assertEquals(1, events.get());
    tableModel.removeItems(1, 3);
    assertEquals(2, events.get());
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

    final TableColumn columnId = new TableColumn(0);
    columnId.setIdentifier(0);
    final TableColumn columnValue = new TableColumn(1);
    columnValue.setIdentifier(1);

    final List<Row> items = Arrays.asList(new Row(0, "a"), new Row(1, "b"),
            new Row(2, "c"), new Row(3, "d"), new Row(4, "e"));

    final AbstractFilteredTableModel<Row, Integer> testModel = new AbstractFilteredTableModel<Row, Integer>(
            new AbstractTableSortModel<Row, Integer>(Arrays.asList(columnId, columnValue)) {
              @Override
              public Class getColumnClass(final Integer columnIdentifier) {
                if (columnIdentifier == 0) {
                  return Integer.class;
                }

                return String.class;
              }

              @Override
              protected Comparable getComparable(final Row rowObject, final Integer columnIdentifier) {
                if (columnIdentifier == 0) {
                  return rowObject.id;
                }

                return rowObject.value;
              }
            }, null) {
      @Override
      protected void doRefresh() {
        clear();
        addItems(items, false);
      }

      @Override
      public Object getValueAt(final int rowIndex, final int columnIndex) {
        final Row row = getItemAt(rowIndex);
        if (columnIndex == 0) {
          return row.id;
        }

        return row.value;
      }
    };

    testModel.refresh();
    RowColumn point = testModel.findNextItemCoordinate(0, true, "b");
    assertEquals(RowColumn.rowColumn(1, 1), point);
    point = testModel.findNextItemCoordinate(point.getRow(), true, "e");
    assertEquals(RowColumn.rowColumn(4, 1), point);
    point = testModel.findNextItemCoordinate(point.getRow(), false, "c");
    assertEquals(RowColumn.rowColumn(2, 1), point);
    point = testModel.findNextItemCoordinate(0, true, "x");
    assertNull(point);

    testModel.getSortModel().setSortingDirective(1, SortingDirective.DESCENDING, false);

    point = testModel.findNextItemCoordinate(0, true, "b");
    assertEquals(RowColumn.rowColumn(3, 1), point);
    point = testModel.findNextItemCoordinate(point.getRow(), false, "e");
    assertEquals(RowColumn.rowColumn(0, 1), point);

    testModel.setRegularExpressionSearch(true);
    assertTrue(testModel.isRegularExpressionSearch());
    point = testModel.findNextItemCoordinate(0, true, "(?i)B");
    assertEquals(RowColumn.rowColumn(3, 1), point);

    FilterCondition<Object> condition = item -> item.equals("b") || item.equals("e");

    point = testModel.findNextItemCoordinate(4, false, condition);
    assertEquals(RowColumn.rowColumn(3, 1), point);
    point = testModel.findNextItemCoordinate(point.getRow() - 1, false, condition);
    assertEquals(RowColumn.rowColumn(0, 1), point);

    testModel.getSortModel().setSortingDirective(1, SortingDirective.ASCENDING, false);
    testModel.getColumnModel().moveColumn(1, 0);

    testModel.refresh();
    point = testModel.findNextItemCoordinate(0, true, "b");
    assertEquals(RowColumn.rowColumn(1, 0), point);
    point = testModel.findNextItemCoordinate(point.getRow(), true, "e");
    assertEquals(RowColumn.rowColumn(4, 0), point);
    point = testModel.findNextItemCoordinate(point.getRow(), false, "c");
    assertEquals(RowColumn.rowColumn(2, 0), point);
    point = testModel.findNextItemCoordinate(0, true, "x");
    assertNull(point);

    testModel.getSortModel().setSortingDirective(0, SortingDirective.DESCENDING, false);

    point = testModel.findNextItemCoordinate(0, true, "b");
    assertEquals(RowColumn.rowColumn(3, 0), point);
    point = testModel.findNextItemCoordinate(point.getRow(), false, "e");
    assertEquals(RowColumn.rowColumn(0, 0), point);

    testModel.setRegularExpressionSearch(true);
    assertTrue(testModel.isRegularExpressionSearch());
    point = testModel.findNextItemCoordinate(0, true, "(?i)B");
    assertEquals(RowColumn.rowColumn(3, 0), point);

    condition = item -> item.equals("b") || item.equals("e");

    point = testModel.findNextItemCoordinate(4, false, condition);
    assertEquals(RowColumn.rowColumn(3, 0), point);
    point = testModel.findNextItemCoordinate(point.getRow() - 1, false, condition);
    assertEquals(RowColumn.rowColumn(0, 0), point);
  }

  @Test
  public void clear() {
    tableModel.refresh();
    assertTrue(tableModel.getRowCount() > 0);
    tableModel.clear();
    assertEquals(0, tableModel.getRowCount());
  }

  @Test
  public void customSorting() {
    final AbstractFilteredTableModel<String, Integer> tableModel = createTestModel(Comparator.reverseOrder());
    tableModel.refresh();
    tableModel.getSortModel().setSortingDirective(0, SortingDirective.ASCENDING, false);
    assertEquals("e", tableModel.getItemAt(0));
    tableModel.getSortModel().setSortingDirective(0, SortingDirective.DESCENDING, false);
    assertEquals("a", tableModel.getItemAt(0));
  }

  @Test
  public void sorting() {
    final AtomicInteger actionsPerformed = new AtomicInteger();
    final EventListener listener = actionsPerformed::incrementAndGet;
    tableModel.addSortingListener(listener);

    tableModel.refresh();
    tableModel.getSortModel().setSortingDirective(0, SortingDirective.DESCENDING, false);
    assertEquals(SortingDirective.DESCENDING, tableModel.getSortModel().getSortingState(0).getDirective());
    assertEquals("e", tableModel.getItemAt(0));
    assertEquals(1, actionsPerformed.get());
    tableModel.getSortModel().setSortingDirective(0, SortingDirective.ASCENDING, false);
    assertEquals(SortingDirective.ASCENDING, tableModel.getSortModel().getSortingState(0).getDirective());
    assertEquals("a", tableModel.getItemAt(0));
    assertEquals(0, tableModel.getSortModel().getSortingState(0).getPriority());
    assertEquals(2, actionsPerformed.get());

    tableModel.getSortModel().setSortingDirective(0, SortingDirective.DESCENDING, false);
    tableModel.refresh();
    assertEquals("a", tableModel.getItemAt(4));
    assertEquals("e", tableModel.getItemAt(0));
    tableModel.getSortModel().setSortingDirective(0, SortingDirective.ASCENDING, false);

    final List<String> items = new ArrayList<>();
    items.add(null);
    tableModel.addItems(items, true);
    tableModel.getSortModel().setSortingDirective(0, SortingDirective.ASCENDING, false);
    assertEquals(0, tableModel.indexOf(null));
    tableModel.getSortModel().setSortingDirective(0, SortingDirective.DESCENDING, false);
    assertEquals(tableModel.getRowCount() - 1, tableModel.indexOf(null));

    tableModel.refresh();
    items.add(null);
    tableModel.addItems(items, true);
    tableModel.getSortModel().setSortingDirective(0, SortingDirective.ASCENDING, false);
    assertEquals(0, tableModel.indexOf(null));
    tableModel.getSortModel().setSortingDirective(0, SortingDirective.DESCENDING, false);
    assertEquals(tableModel.getRowCount() - 2, tableModel.indexOf(null));
    tableModel.getSortModel().setSortingDirective(0, SortingDirective.UNSORTED, false);
    tableModel.removeSortingListener(listener);
  }

  @Test
  public void getSortingDirectiveInvalidColumn() {
    assertThrows(IllegalArgumentException.class, () -> tableModel.getSortModel().getSortingState(1).getDirective());
  }

  @Test
  public void getSortingPriorityInvalidColumn() {
    assertThrows(IllegalArgumentException.class, () -> tableModel.getSortModel().getSortingState(1).getPriority());
  }

  @Test
  public void addSelectedIndexesNegative() {
    final Collection<Integer> indexes = new ArrayList<>();
    indexes.add(1);
    indexes.add(-1);
    assertThrows(IndexOutOfBoundsException.class, () -> tableModel.getSelectionModel().addSelectedIndexes(indexes));
  }

  @Test
  public void addSelectedIndexesOutOfBounds() {
    final Collection<Integer> indexes = new ArrayList<>();
    indexes.add(1);
    indexes.add(10);
    assertThrows(IndexOutOfBoundsException.class, () -> tableModel.getSelectionModel().addSelectedIndexes(indexes));
  }

  @Test
  public void setSelectedIndexesNegative() {
    final Collection<Integer> indexes = new ArrayList<>();
    indexes.add(1);
    indexes.add(-1);
    assertThrows(IndexOutOfBoundsException.class, () -> tableModel.getSelectionModel().setSelectedIndexes(indexes));
  }

  @Test
  public void setSelectedIndexesOutOfBounds() {
    final Collection<Integer> indexes = new ArrayList<>();
    indexes.add(1);
    indexes.add(10);
    assertThrows(IndexOutOfBoundsException.class, () -> tableModel.getSelectionModel().setSelectedIndexes(indexes));
  }

  @Test
  public void setSelectedIndexNegative() {
    assertThrows(IndexOutOfBoundsException.class, () -> tableModel.getSelectionModel().setSelectedIndex(-1));
  }

  @Test
  public void setSelectedIndexOutOfBounds() {
    assertThrows(IndexOutOfBoundsException.class, () -> tableModel.getSelectionModel().setSelectedIndex(10));
  }

  @Test
  public void addSelectedIndexNegative() {
    assertThrows(IndexOutOfBoundsException.class, () -> tableModel.getSelectionModel().addSelectedIndex(-1));
  }

  @Test
  public void addSelectedIndexOutOfBounds() {
    assertThrows(IndexOutOfBoundsException.class, () -> tableModel.getSelectionModel().addSelectedIndex(10));
  }

  @Test
  public void selection() {
    final AtomicInteger events = new AtomicInteger();
    final EventListener listener = events::incrementAndGet;
    final EventDataListener dataListener = Events.dataListener(listener);
    final SwingTableSelectionModel<String> selectionModel = (SwingTableSelectionModel<String>) tableModel.getSelectionModel();
    selectionModel.addSelectedIndexListener(dataListener);
    selectionModel.addSelectionChangedListener(listener);
    selectionModel.addSelectedItemListener(dataListener);
    selectionModel.addSelectedItemsListener(dataListener);

    assertFalse(selectionModel.getSingleSelectionObserver().isActive());
    assertTrue(selectionModel.getSelectionEmptyObserver().isActive());
    assertFalse(selectionModel.getMultipleSelectionObserver().isActive());

    tableModel.refresh();
    selectionModel.setSelectedIndex(2);
    assertEquals(4, events.get());
    assertTrue(selectionModel.getSingleSelectionObserver().isActive());
    assertFalse(selectionModel.getSelectionEmptyObserver().isActive());
    assertFalse(selectionModel.getMultipleSelectionObserver().isActive());
    assertEquals(2, selectionModel.getSelectedIndex());
    selectionModel.moveSelectionDown();
    assertEquals(12, events.get());
    assertEquals(3, selectionModel.getSelectedIndex());
    selectionModel.moveSelectionUp();
    selectionModel.moveSelectionUp();
    assertEquals(1, selectionModel.getSelectedIndex());

    selectionModel.moveSelectionDown();
    selectionModel.moveSelectionDown();

    assertEquals(3, selectionModel.getSelectedIndex());

    selectionModel.setSelectedIndex(0);
    selectionModel.moveSelectionUp();
    assertEquals(tableModel.getRowCount() - 1, selectionModel.getSelectedIndex());

    selectionModel.setSelectedIndex(tableModel.getRowCount() - 1);
    selectionModel.moveSelectionDown();
    assertEquals(0, selectionModel.getSelectedIndex());

    selectionModel.clearSelection();
    selectionModel.moveSelectionUp();
    assertEquals(tableModel.getRowCount() - 1, selectionModel.getSelectedIndex());

    selectionModel.clearSelection();
    selectionModel.moveSelectionDown();
    assertEquals(0, selectionModel.getSelectedIndex());

    selectionModel.selectAll();
    assertEquals(5, selectionModel.getSelectedItems().size());
    selectionModel.clearSelection();
    assertFalse(selectionModel.getSingleSelectionObserver().isActive());
    assertTrue(selectionModel.getSelectionEmptyObserver().isActive());
    assertFalse(selectionModel.getMultipleSelectionObserver().isActive());
    assertEquals(0, selectionModel.getSelectedItems().size());

    selectionModel.setSelectedItem(ITEMS[0]);
    assertFalse(selectionModel.getMultipleSelectionObserver().isActive());
    assertEquals(ITEMS[0], selectionModel.getSelectedItem());
    assertEquals(0, selectionModel.getSelectedIndex());
    assertEquals(1, selectionModel.getSelectionCount());
    assertFalse(selectionModel.isSelectionEmpty());
    selectionModel.addSelectedIndex(1);
    assertTrue(selectionModel.getMultipleSelectionObserver().isActive());
    assertEquals(ITEMS[0], selectionModel.getSelectedItem());
    assertEquals(Arrays.asList(0, 1), selectionModel.getSelectedIndexes());
    assertEquals(0, selectionModel.getSelectedIndex());
    selectionModel.addSelectedIndex(4);
    assertTrue(selectionModel.getMultipleSelectionObserver().isActive());
    assertEquals(Arrays.asList(0, 1, 4), selectionModel.getSelectedIndexes());
    selectionModel.removeIndexInterval(1, 4);
    assertEquals(Collections.singletonList(0), selectionModel.getSelectedIndexes());
    assertEquals(0, selectionModel.getSelectedIndex());
    selectionModel.clearSelection();
    assertEquals(new ArrayList<Integer>(), selectionModel.getSelectedIndexes());
    assertEquals(-1, selectionModel.getMinSelectionIndex());
    selectionModel.addSelectedIndexes(Arrays.asList(0, 3, 4));
    assertEquals(Arrays.asList(0, 3, 4), selectionModel.getSelectedIndexes());
    assertEquals(0, selectionModel.getMinSelectionIndex());
    assertEquals(3, selectionModel.getSelectionCount());
    selectionModel.removeSelectionInterval(0, 0);
    assertEquals(3, selectionModel.getMinSelectionIndex());
    selectionModel.removeSelectionInterval(3, 3);
    assertEquals(4, selectionModel.getMinSelectionIndex());

    selectionModel.addSelectedIndexes(Arrays.asList(0, 3, 4));
    assertEquals(0, selectionModel.getMinSelectionIndex());
    selectionModel.removeSelectionInterval(3, 3);
    assertEquals(0, selectionModel.getMinSelectionIndex());
    selectionModel.clearSelection();
    assertEquals(-1, selectionModel.getMinSelectionIndex());

    selectionModel.addSelectedIndexes(Arrays.asList(0, 1, 2, 3, 4));
    assertEquals(0, selectionModel.getMinSelectionIndex());
    selectionModel.removeSelectionInterval(0, 0);
    assertEquals(1, selectionModel.getMinSelectionIndex());
    selectionModel.removeSelectionInterval(1, 1);
    assertEquals(2, selectionModel.getMinSelectionIndex());
    selectionModel.removeSelectionInterval(2, 2);
    assertEquals(3, selectionModel.getMinSelectionIndex());
    selectionModel.removeSelectionInterval(3, 3);
    assertEquals(4, selectionModel.getMinSelectionIndex());
    selectionModel.removeSelectionInterval(4, 4);
    assertEquals(-1, selectionModel.getMinSelectionIndex());

    selectionModel.addSelectedItem(ITEMS[0]);
    assertEquals(1, selectionModel.getSelectionCount());
    assertEquals(0, selectionModel.getMinSelectionIndex());
    selectionModel.addSelectedItems(Arrays.asList(ITEMS[1], ITEMS[2]));
    assertEquals(3, selectionModel.getSelectionCount());
    assertEquals(0, selectionModel.getMinSelectionIndex());
    selectionModel.addSelectedItem(ITEMS[4]);
    assertEquals(4, selectionModel.getSelectionCount());
    assertEquals(0, selectionModel.getMinSelectionIndex());
    tableModel.removeItem(ITEMS[0]);
    assertEquals(3, selectionModel.getSelectionCount());
    assertEquals(0, selectionModel.getMinSelectionIndex());

    tableModel.clear();
    assertTrue(selectionModel.getSelectionEmptyObserver().isActive());
    assertNull(selectionModel.getSelectedItem());

    selectionModel.clearSelection();
    selectionModel.removeSelectedIndexListener(dataListener);
    selectionModel.removeSelectionChangedListener(listener);
    selectionModel.removeSelectedItemListener(dataListener);
    selectionModel.removeSelectedItemsListener(dataListener);
  }

  @Test
  public void selectionAndSorting() {
    tableModel.refresh();
    assertTrue(tableModelContainsAll(ITEMS, false, tableModel));

    //test selection and filtering together
    final SwingTableSelectionModel<String> selectionModel = (SwingTableSelectionModel<String>) tableModel.getSelectionModel();
    tableModel.getSelectionModel().addSelectedIndexes(Collections.singletonList(3));
    assertEquals(3, selectionModel.getMinSelectionIndex());

    tableModel.getColumnModel().getColumnFilterModel(0).setLikeValue("d");
    tableModel.getColumnModel().getColumnFilterModel(0).setEnabled(false);

    selectionModel.setSelectedIndexes(Collections.singletonList(3));
    assertEquals(3, selectionModel.getMinSelectionIndex());
    assertEquals(ITEMS[2], selectionModel.getSelectedItem());

    tableModel.getSortModel().setSortingDirective(0, SortingDirective.ASCENDING, false);
    assertEquals(ITEMS[2], selectionModel.getSelectedItem());
    assertEquals(2, selectionModel.getMinSelectionIndex());

    tableModel.getSelectionModel().setSelectedIndexes(Collections.singletonList(0));
    assertEquals(ITEMS[0], selectionModel.getSelectedItem());
    tableModel.getSortModel().setSortingDirective(0, SortingDirective.DESCENDING, false);
    assertEquals(4, selectionModel.getMinSelectionIndex());

    assertEquals(Collections.singletonList(4), selectionModel.getSelectedIndexes());
    assertEquals(ITEMS[0], selectionModel.getSelectedItem());
    assertEquals(4, selectionModel.getMinSelectionIndex());
    assertEquals(ITEMS[0], selectionModel.getSelectedItem());
  }

  @Test
  public void selectionAndFiltering() {
    tableModel.refresh();
    tableModel.getSelectionModel().addSelectedIndexes(Collections.singletonList(3));
    assertEquals(3, ((SwingTableSelectionModel) tableModel.getSelectionModel()).getMinSelectionIndex());

    tableModel.getColumnModel().getColumnFilterModel(0).setLikeValue("d");
    assertEquals(0, ((SwingTableSelectionModel) tableModel.getSelectionModel()).getMinSelectionIndex());
    assertEquals(Collections.singletonList(0), tableModel.getSelectionModel().getSelectedIndexes());
    tableModel.getColumnModel().getColumnFilterModel(0).setEnabled(false);
    assertEquals(0, ((SwingTableSelectionModel) tableModel.getSelectionModel()).getMinSelectionIndex());
    assertEquals(ITEMS[3], tableModel.getSelectionModel().getSelectedItem());
  }

  @Test
  public void setFilterCondition() {
    tableModel.refresh();
    tableModel.setFilterCondition(item -> false);
    assertEquals(0, tableModel.getRowCount());
  }

  @Test
  public void columns() {
    assertEquals(1, tableModel.getColumnCount());
  }

  @Test
  public void filterAndRemove() {
    tableModel.refresh();
    tableModel.getColumnModel().getColumnFilterModel(0).setLikeValue("a");
    assertTrue(tableModel.contains("b", true));
    tableModel.removeItem("b");
    assertFalse(tableModel.contains("b", true));
    tableModel.removeItem("a");
    assertFalse(tableModel.contains("a", true));
  }

  @Test
  public void filtering() throws Exception {
    final AtomicInteger done = new AtomicInteger();
    final EventListener listener = done::incrementAndGet;
    tableModel.addFilteringListener(listener);

    tableModel.refresh();
    assertTrue(tableModelContainsAll(ITEMS, false, tableModel));
    assertNotNull(tableModel.getFilterCondition());

    //test filters
    assertTrue(tableModel.contains("b", false));
    tableModel.getColumnModel().getColumnFilterModel(0).setLikeValue("a");
    assertEquals(2, done.get());
    assertTrue(tableModel.isVisible("a"));
    assertFalse(tableModel.isVisible("b"));
    assertTrue(tableModel.isFiltered("d"));
    assertFalse(tableModel.contains("b", false));
    assertTrue(tableModel.contains("b", true));
    assertTrue(tableModel.getColumnModel().getColumnFilterModel(0).isEnabled());
    assertEquals(4, tableModel.getFilteredItemCount());
    assertFalse(tableModelContainsAll(ITEMS, false, tableModel));
    assertTrue(tableModelContainsAll(ITEMS, true, tableModel));

    assertTrue(tableModel.getVisibleItems().size() > 0);
    assertTrue(tableModel.getFilteredItems().size() > 0);
    assertTrue(tableModel.getAllItems().size() > 0);

    tableModel.getColumnModel().getColumnFilterModel(0).setEnabled(false);
    assertEquals(3, done.get());
    assertFalse(tableModel.getColumnModel().getColumnFilterModel(0).isEnabled());

    assertTrue(tableModelContainsAll(ITEMS, false, tableModel));

    tableModel.getColumnModel().getColumnFilterModel(0).setLikeValue("t"); // ekki til
    assertTrue(tableModel.getColumnModel().getColumnFilterModel(0).isEnabled());
    assertEquals(5, tableModel.getFilteredItemCount());
    assertFalse(tableModelContainsAll(ITEMS, false, tableModel));
    assertTrue(tableModelContainsAll(ITEMS, true, tableModel));
    tableModel.getColumnModel().getColumnFilterModel(0).setEnabled(false);
    assertTrue(tableModelContainsAll(ITEMS, false, tableModel));
    assertFalse(tableModel.getColumnModel().getColumnFilterModel(0).isEnabled());

    tableModel.getColumnModel().getColumnFilterModel(0).setLikeValue("b");
    final int rowCount = tableModel.getRowCount();
    tableModel.addItems(Collections.singletonList("x"), true);
    assertEquals(rowCount, tableModel.getRowCount());

    tableModel.removeFilteringListener(listener);
  }

  @Test
  public void getValues() {
    tableModel.refresh();
    tableModel.getSelectionModel().setSelectedIndexes(Arrays.asList(0, 2));
    Collection values = tableModel.getValues(0, true);
    assertEquals(2, values.size());
    assertTrue(values.contains("a"));
    assertTrue(values.contains("c"));

    values = tableModel.getValues(0, false);
    assertEquals(5, values.size());
    assertTrue(values.contains("a"));
    assertTrue(values.contains("b"));
    assertTrue(values.contains("c"));
    assertTrue(values.contains("d"));
    assertTrue(values.contains("e"));
    assertFalse(values.contains("zz"));
  }

  @Test
  public void columnModel() {
    final TableColumn column = tableModel.getColumnModel().getTableColumn(0);
    assertEquals(0, column.getIdentifier());
  }

  @Test
  public void getColumnClass() {
    assertEquals(String.class, tableModel.getColumnClass(0));
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
