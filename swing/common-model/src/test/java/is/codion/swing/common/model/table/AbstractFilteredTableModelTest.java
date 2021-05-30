/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.table;

import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.ColumnFilterModel;
import is.codion.common.model.table.DefaultColumnFilterModel;
import is.codion.common.model.table.FilteredTableModel.RowColumn;
import is.codion.common.model.table.TableSortModel;
import is.codion.common.model.table.TableSortModel.SortingDirective;
import is.codion.common.state.State;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.table.TableColumn;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

/**
 * User: Björn Darri
 * Date: 25.7.2010
 * Time: 13:54:59
 */
public final class AbstractFilteredTableModelTest {

  private static final List<String> A = singletonList("a");
  private static final List<String> B = singletonList("b");
  private static final List<String> C = singletonList("c");
  private static final List<String> D = singletonList("d");
  private static final List<String> E = singletonList("e");
  private static final List<String> F = singletonList("f");
  private static final List<String> G = singletonList("g");
  private static final List<String> NULL = singletonList(null);
  private static final List<List<String>> ITEMS = asList(A, B, C, D, E);

  private TestAbstractFilteredTableModel tableModel;

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
      return getItemAt(rowIndex).get(columnIndex);
    }

    void addItemsAt(final List<List<String>> items, final int index) {
      addItemsAt(index, items);
    }
  }

  private static TestAbstractFilteredTableModel createTestModel() {
    return createTestModel(null);
  }

  private static TestAbstractFilteredTableModel createTestModel(final Comparator<String> customComparator) {
    final TableColumn column = new TableColumn(0);
    column.setIdentifier(0);
    final ColumnFilterModel<List<String>, Integer, String> filterModel = new DefaultColumnFilterModel<>(0, String.class, "%");
    filterModel.setComparableFunction(row -> row.get(0));
    return new TestAbstractFilteredTableModel(singletonList(column), new AbstractTableSortModel<List<String>, Integer>() {
      @Override
      public Class<?> getColumnClass(final Integer columnIdentifier) {
        return String.class;
      }

      @Override
      protected Comparable<String> getComparable(final List<String> row, final Integer columnIdentifier) {
        return row.get(columnIdentifier);
      }

      @Override
      protected Comparator<String> initializeColumnComparator(final Integer columnIdentifier) {
        if (customComparator != null) {
          return customComparator;
        }

        return (Comparator<String>) super.initializeColumnComparator(columnIdentifier);
      }
    }, singletonList(filterModel));
  }

  @BeforeEach
  void setUp() {
    tableModel = createTestModel();
  }

  @Test
  void getColumnCount() {
    assertEquals(1, tableModel.getColumnCount());
  }

  @Test
  void filterContents() {
    tableModel.refresh();
    tableModel.setIncludeCondition(item -> !item.equals(B) && !item.equals(F));
    assertFalse(tableModel.isVisible(B));
    assertTrue(tableModel.containsItem(B));
    tableModel.addItemsAt(Collections.singletonList(F), 0);
    tableModel.getSortModel().setSortingDirective(0, SortingDirective.DESCENDING);
    assertFalse(tableModel.isVisible(F));
    assertTrue(tableModel.containsItem(F));
    tableModel.setIncludeCondition(null);
    assertTrue(tableModel.isVisible(B));
    assertTrue(tableModel.isVisible(F));
  }

  @Test
  void addItemsAt() {
    tableModel.refresh();
    tableModel.addItemsAt(asList(F, G), 2);
    assertEquals(2, tableModel.indexOf(F));
    assertEquals(3, tableModel.indexOf(G));
    assertEquals(4, tableModel.indexOf(C));
  }

  @Test
  void nullSortModel() {
    assertThrows(NullPointerException.class, () -> new AbstractFilteredTableModel<String, Integer>(new SwingFilteredTableColumnModel<>(singletonList(new TableColumn())), null) {
      @Override
      public Object getValueAt(final int rowIndex, final int columnIndex) {
        return null;
      }
    });
  }

  @Test
  void noColumns() {
    assertThrows(IllegalArgumentException.class, () -> new AbstractFilteredTableModel<String, Integer>(new SwingFilteredTableColumnModel<>(emptyList()),
            new AbstractTableSortModel<String, Integer>() {
      @Override
      protected Comparable<?> getComparable(final String row, final Integer columnIdentifier) {
        return null;
      }

      @Override
      public Class<?> getColumnClass(final Integer columnIdentifier) {
        return null;
      }
    }) {

      @Override
      public Object getValueAt(final int rowIndex, final int columnIndex) {
        return null;
      }
    });
  }

  @Test
  void refreshEvents() {
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

    started.set(0);
    done.set(0);
    cleared.set(0);
    tableModel.setMergeOnRefresh(true);
    tableModel.refresh();
    assertEquals(1, started.get());
    assertEquals(1, done.get());
    assertEquals(0, cleared.get());

    tableModel.removeRefreshStartedListener(startListener);
    tableModel.removeRefreshDoneListener(doneListener);
    tableModel.removeTableModelClearedListener(clearedListener);
  }

  @Test
  void removeItems() {
    final AtomicInteger events = new AtomicInteger();
    final EventListener listener = events::incrementAndGet;
    tableModel.addTableDataChangedListener(listener);
    tableModel.refresh();
    assertEquals(1, events.get());
    final ColumnConditionModel<Integer, String> columnFilterModel =
            tableModel.getColumnFilterModel(0);
    columnFilterModel.setEqualValue("a");
    tableModel.removeItem(B);
    assertEquals(3, events.get());
    assertFalse(tableModel.isVisible(B));
    assertTrue(tableModel.containsItem(A));
    tableModel.removeItem(A);
    assertEquals(4, events.get());
    assertFalse(tableModel.containsItem(A));
    tableModel.removeItems(asList(D, E));
    assertEquals(4, events.get());//no change when removing filtered items
    assertFalse(tableModel.isVisible(D));
    assertFalse(tableModel.isVisible(E));
    tableModel.removeTableDataChangedListener(listener);
  }

  @Test
  void setItemAt() {
    final State selectionChangedState = State.state();
    tableModel.getSelectionModel().addSelectedItemListener((item) -> selectionChangedState.set(true));
    tableModel.refresh();
    tableModel.getSelectionModel().setSelectedItem(B);
    final List<String> h = singletonList("h");
    tableModel.setItemAt(tableModel.indexOf(B), h);
    assertEquals(h, tableModel.getSelectionModel().getSelectedItem());
    assertTrue(selectionChangedState.get());
    tableModel.setItemAt(tableModel.indexOf(h), B);

    selectionChangedState.set(false);
    final List<String> newB = singletonList("b");
    tableModel.setItemAt(tableModel.indexOf(B), newB);
    assertFalse(selectionChangedState.get());
    assertEquals(newB, tableModel.getSelectionModel().getSelectedItem());
  }

  @Test
  void removeItemsRange() {
    final AtomicInteger events = new AtomicInteger();
    final EventListener listener = events::incrementAndGet;
    tableModel.addTableDataChangedListener(listener);
    tableModel.refresh();
    assertEquals(1, events.get());
    tableModel.removeItems(1, 3);
    assertEquals(2, events.get());
    assertTrue(tableModel.containsItem(A));
    assertFalse(tableModel.containsItem(B));
    assertFalse(tableModel.containsItem(C));
    assertTrue(tableModel.containsItem(D));
    assertTrue(tableModel.containsItem(E));

    tableModel.setMergeOnRefresh(true);
    events.set(0);
    tableModel.refresh();
    assertEquals(5, events.get());

    tableModel.removeTableDataChangedListener(listener);
  }

  @Test
  void find() {
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

    final List<Row> items = asList(new Row(0, "a"), new Row(1, "b"),
            new Row(2, "c"), new Row(3, "d"), new Row(4, "e"));

    final AbstractFilteredTableModel<Row, Integer> testModel = new AbstractFilteredTableModel<Row, Integer>(new SwingFilteredTableColumnModel<>(asList(columnId, columnValue)),
            new AbstractTableSortModel<Row, Integer>() {
              @Override
              public Class getColumnClass(final Integer columnIdentifier) {
                if (columnIdentifier == 0) {
                  return Integer.class;
                }

                return String.class;
              }

              @Override
              protected Comparable getComparable(final Row row, final Integer columnIdentifier) {
                if (columnIdentifier == 0) {
                  return row.id;
                }

                return row.value;
              }
            }) {
      @Override
      protected Collection<Row> refreshItems() {
        return items;
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
    RowColumn coordinate = testModel.findNext(0, "b");
    assertEquals(RowColumn.rowColumn(1, 1), coordinate);
    coordinate = testModel.findNext(coordinate.getRow(), "e");
    assertEquals(RowColumn.rowColumn(4, 1), coordinate);
    coordinate = testModel.findPrevious(coordinate.getRow(), "c");
    assertEquals(RowColumn.rowColumn(2, 1), coordinate);
    coordinate = testModel.findNext(0, "x");
    assertNull(coordinate);

    testModel.getSortModel().setSortingDirective(1, SortingDirective.DESCENDING);

    coordinate = testModel.findNext(0, "b");
    assertEquals(RowColumn.rowColumn(3, 1), coordinate);
    coordinate = testModel.findPrevious(coordinate.getRow(), "e");
    assertEquals(RowColumn.rowColumn(0, 1), coordinate);

    testModel.setRegularExpressionSearch(true);
    assertTrue(testModel.isRegularExpressionSearch());
    coordinate = testModel.findNext(0, "(?i)B");
    assertEquals(RowColumn.rowColumn(3, 1), coordinate);

    Predicate<String> condition = item -> item.equals("b") || item.equals("e");

    coordinate = testModel.findPrevious(4, condition);
    assertEquals(RowColumn.rowColumn(3, 1), coordinate);
    coordinate = testModel.findPrevious(coordinate.getRow() - 1, condition);
    assertEquals(RowColumn.rowColumn(0, 1), coordinate);

    testModel.getSortModel().setSortingDirective(1, SortingDirective.ASCENDING);
    testModel.getColumnModel().moveColumn(1, 0);

    testModel.refresh();
    coordinate = testModel.findNext(0, "b");
    assertEquals(RowColumn.rowColumn(1, 0), coordinate);
    coordinate = testModel.findNext(coordinate.getRow(), "e");
    assertEquals(RowColumn.rowColumn(4, 0), coordinate);
    coordinate = testModel.findPrevious(coordinate.getRow(), "c");
    assertEquals(RowColumn.rowColumn(2, 0), coordinate);
    coordinate = testModel.findNext(0, "x");
    assertNull(coordinate);

    testModel.getSortModel().setSortingDirective(0, SortingDirective.DESCENDING);

    coordinate = testModel.findNext(0, "b");
    assertEquals(RowColumn.rowColumn(3, 0), coordinate);
    coordinate = testModel.findPrevious(coordinate.getRow(), "e");
    assertEquals(RowColumn.rowColumn(0, 0), coordinate);

    testModel.setRegularExpressionSearch(true);
    assertTrue(testModel.isRegularExpressionSearch());
    coordinate = testModel.findNext(0, "(?i)B");
    assertEquals(RowColumn.rowColumn(3, 0), coordinate);

    condition = item -> item.equals("b") || item.equals("e");

    coordinate = testModel.findPrevious(4, condition);
    assertEquals(RowColumn.rowColumn(3, 0), coordinate);
    coordinate = testModel.findPrevious(coordinate.getRow() - 1, condition);
    assertEquals(RowColumn.rowColumn(0, 0), coordinate);
  }

  @Test
  void clear() {
    tableModel.refresh();
    assertTrue(tableModel.getRowCount() > 0);
    tableModel.clear();
    assertEquals(0, tableModel.getRowCount());
  }

  @Test
  void customSorting() {
    final AbstractFilteredTableModel<List<String>, Integer> tableModel = createTestModel(Comparator.reverseOrder());
    tableModel.refresh();
    final TableSortModel<List<String>, Integer> sortModel = tableModel.getSortModel();
    sortModel.setSortingDirective(0, SortingDirective.ASCENDING);
    assertEquals(E, tableModel.getItemAt(0));
    sortModel.setSortingDirective(0, SortingDirective.DESCENDING);
    assertEquals(A, tableModel.getItemAt(0));
  }

  @Test
  void sorting() {
    final AtomicInteger actionsPerformed = new AtomicInteger();
    final EventListener listener = actionsPerformed::incrementAndGet;
    tableModel.addSortListener(listener);

    tableModel.refresh();
    final TableSortModel<List<String>, Integer> sortModel = tableModel.getSortModel();
    sortModel.setSortingDirective(0, SortingDirective.DESCENDING);
    assertEquals(SortingDirective.DESCENDING, sortModel.getSortingState(0).getDirective());
    assertEquals(E, tableModel.getItemAt(0));
    assertEquals(1, actionsPerformed.get());
    sortModel.setSortingDirective(0, SortingDirective.ASCENDING);
    assertEquals(SortingDirective.ASCENDING, sortModel.getSortingState(0).getDirective());
    assertEquals(A, tableModel.getItemAt(0));
    assertEquals(0, sortModel.getSortingState(0).getPriority());
    assertEquals(2, actionsPerformed.get());

    sortModel.setSortingDirective(0, SortingDirective.DESCENDING);
    tableModel.refresh();
    assertEquals(A, tableModel.getItemAt(4));
    assertEquals(E, tableModel.getItemAt(0));
    sortModel.setSortingDirective(0, SortingDirective.ASCENDING);

    final List<List<String>> items = new ArrayList<>();
    items.add(NULL);
    tableModel.addItemsAt(0, items);
    sortModel.setSortingDirective(0, SortingDirective.ASCENDING);
    assertEquals(0, tableModel.indexOf(NULL));
    sortModel.setSortingDirective(0, SortingDirective.DESCENDING);
    assertEquals(tableModel.getRowCount() - 1, tableModel.indexOf(NULL));

    tableModel.refresh();
    items.add(NULL);
    tableModel.addItemsAt(0, items);
    sortModel.setSortingDirective(0, SortingDirective.ASCENDING);
    assertEquals(0, tableModel.indexOf(NULL));
    sortModel.setSortingDirective(0, SortingDirective.DESCENDING);
    assertEquals(tableModel.getRowCount() - 2, tableModel.indexOf(NULL));
    sortModel.setSortingDirective(0, SortingDirective.UNSORTED);
    tableModel.removeSortListener(listener);
  }

  @Test
  void addSelectedIndexesNegative() {
    final Collection<Integer> indexes = new ArrayList<>();
    indexes.add(1);
    indexes.add(-1);
    assertThrows(IndexOutOfBoundsException.class, () -> tableModel.getSelectionModel().addSelectedIndexes(indexes));
  }

  @Test
  void addSelectedIndexesOutOfBounds() {
    final Collection<Integer> indexes = new ArrayList<>();
    indexes.add(1);
    indexes.add(10);
    assertThrows(IndexOutOfBoundsException.class, () -> tableModel.getSelectionModel().addSelectedIndexes(indexes));
  }

  @Test
  void setSelectedIndexesNegative() {
    final Collection<Integer> indexes = new ArrayList<>();
    indexes.add(1);
    indexes.add(-1);
    assertThrows(IndexOutOfBoundsException.class, () -> tableModel.getSelectionModel().setSelectedIndexes(indexes));
  }

  @Test
  void setSelectedIndexesOutOfBounds() {
    final Collection<Integer> indexes = new ArrayList<>();
    indexes.add(1);
    indexes.add(10);
    assertThrows(IndexOutOfBoundsException.class, () -> tableModel.getSelectionModel().setSelectedIndexes(indexes));
  }

  @Test
  void setSelectedIndexNegative() {
    assertThrows(IndexOutOfBoundsException.class, () -> tableModel.getSelectionModel().setSelectedIndex(-1));
  }

  @Test
  void setSelectedIndexOutOfBounds() {
    assertThrows(IndexOutOfBoundsException.class, () -> tableModel.getSelectionModel().setSelectedIndex(10));
  }

  @Test
  void addSelectedIndexNegative() {
    assertThrows(IndexOutOfBoundsException.class, () -> tableModel.getSelectionModel().addSelectedIndex(-1));
  }

  @Test
  void addSelectedIndexOutOfBounds() {
    assertThrows(IndexOutOfBoundsException.class, () -> tableModel.getSelectionModel().addSelectedIndex(10));
  }

  @Test
  void selection() {
    final AtomicInteger events = new AtomicInteger();
    final EventListener listener = events::incrementAndGet;
    final EventDataListener dataListener = Event.dataListener(listener);
    final SwingTableSelectionModel<List<String>> selectionModel = tableModel.getSelectionModel();
    selectionModel.addSelectedIndexListener(dataListener);
    selectionModel.addSelectionChangedListener(listener);
    selectionModel.addSelectedItemListener(dataListener);
    selectionModel.addSelectedItemsListener(dataListener);

    assertFalse(selectionModel.getSingleSelectionObserver().get());
    assertTrue(selectionModel.getSelectionEmptyObserver().get());
    assertFalse(selectionModel.getSelectionNotEmptyObserver().get());
    assertFalse(selectionModel.getMultipleSelectionObserver().get());

    tableModel.refresh();
    selectionModel.setSelectedIndex(2);
    assertEquals(4, events.get());
    assertTrue(selectionModel.getSingleSelectionObserver().get());
    assertFalse(selectionModel.getSelectionEmptyObserver().get());
    assertFalse(selectionModel.getMultipleSelectionObserver().get());
    assertEquals(2, selectionModel.getSelectedIndex());
    selectionModel.moveSelectionDown();
    assertEquals(8, events.get());
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
    assertFalse(selectionModel.getSingleSelectionObserver().get());
    assertTrue(selectionModel.getSelectionEmptyObserver().get());
    assertFalse(selectionModel.getMultipleSelectionObserver().get());
    assertEquals(0, selectionModel.getSelectedItems().size());

    selectionModel.setSelectedItem(ITEMS.get(0));
    assertFalse(selectionModel.getMultipleSelectionObserver().get());
    assertEquals(ITEMS.get(0), selectionModel.getSelectedItem());
    assertEquals(0, selectionModel.getSelectedIndex());
    assertEquals(1, selectionModel.getSelectionCount());
    assertFalse(selectionModel.isSelectionEmpty());
    selectionModel.addSelectedIndex(1);
    assertTrue(selectionModel.getMultipleSelectionObserver().get());
    assertEquals(ITEMS.get(0), selectionModel.getSelectedItem());
    assertEquals(asList(0, 1), selectionModel.getSelectedIndexes());
    assertEquals(0, selectionModel.getSelectedIndex());
    selectionModel.addSelectedIndex(4);
    assertTrue(selectionModel.getMultipleSelectionObserver().get());
    assertEquals(asList(0, 1, 4), selectionModel.getSelectedIndexes());
    selectionModel.removeIndexInterval(1, 4);
    assertEquals(singletonList(0), selectionModel.getSelectedIndexes());
    assertEquals(0, selectionModel.getSelectedIndex());
    selectionModel.clearSelection();
    assertEquals(new ArrayList<Integer>(), selectionModel.getSelectedIndexes());
    assertEquals(-1, selectionModel.getMinSelectionIndex());
    selectionModel.addSelectedIndexes(asList(0, 3, 4));
    assertEquals(asList(0, 3, 4), selectionModel.getSelectedIndexes());
    assertEquals(0, selectionModel.getMinSelectionIndex());
    assertEquals(3, selectionModel.getSelectionCount());
    selectionModel.removeSelectionInterval(0, 0);
    assertEquals(3, selectionModel.getMinSelectionIndex());
    selectionModel.removeSelectionInterval(3, 3);
    assertEquals(4, selectionModel.getMinSelectionIndex());

    selectionModel.addSelectedIndexes(asList(0, 3, 4));
    assertEquals(0, selectionModel.getMinSelectionIndex());
    selectionModel.removeSelectionInterval(3, 3);
    assertEquals(0, selectionModel.getMinSelectionIndex());
    selectionModel.clearSelection();
    assertEquals(-1, selectionModel.getMinSelectionIndex());

    selectionModel.addSelectedIndexes(asList(0, 1, 2, 3, 4));
    assertEquals(0, selectionModel.getMinSelectionIndex());
    selectionModel.removeSelectionInterval(0, 0);
    assertEquals(1, selectionModel.getMinSelectionIndex());
    selectionModel.removeSelectedIndex(1);
    assertEquals(2, selectionModel.getMinSelectionIndex());
    selectionModel.removeSelectedIndexes(singletonList(2));
    assertEquals(3, selectionModel.getMinSelectionIndex());
    selectionModel.removeSelectionInterval(3, 3);
    assertEquals(4, selectionModel.getMinSelectionIndex());
    selectionModel.removeSelectedIndex(4);
    assertEquals(-1, selectionModel.getMinSelectionIndex());

    selectionModel.addSelectedItem(ITEMS.get(0));
    assertEquals(1, selectionModel.getSelectionCount());
    assertEquals(0, selectionModel.getMinSelectionIndex());
    selectionModel.addSelectedItems(asList(ITEMS.get(1), ITEMS.get(2)));
    assertEquals(3, selectionModel.getSelectionCount());
    assertEquals(0, selectionModel.getMinSelectionIndex());
    selectionModel.removeSelectedItem(ITEMS.get(1));
    assertEquals(2, selectionModel.getSelectionCount());
    assertEquals(0, selectionModel.getMinSelectionIndex());
    selectionModel.removeSelectedItem(ITEMS.get(2));
    assertEquals(1, selectionModel.getSelectionCount());
    assertEquals(0, selectionModel.getMinSelectionIndex());
    selectionModel.addSelectedItems(asList(ITEMS.get(1), ITEMS.get(2)));
    assertEquals(3, selectionModel.getSelectionCount());
    assertEquals(0, selectionModel.getMinSelectionIndex());
    selectionModel.addSelectedItem(ITEMS.get(4));
    assertEquals(4, selectionModel.getSelectionCount());
    assertEquals(0, selectionModel.getMinSelectionIndex());
    tableModel.removeItem(ITEMS.get(0));
    assertEquals(3, selectionModel.getSelectionCount());
    assertEquals(0, selectionModel.getMinSelectionIndex());

    tableModel.clear();
    assertTrue(selectionModel.getSelectionEmptyObserver().get());
    assertFalse(selectionModel.getSelectionNotEmptyObserver().get());
    assertNull(selectionModel.getSelectedItem());

    selectionModel.clearSelection();
    selectionModel.removeSelectedIndexListener(dataListener);
    selectionModel.removeSelectionChangedListener(listener);
    selectionModel.removeSelectedItemListener(dataListener);
    selectionModel.removeSelectedItemsListener(dataListener);
  }

  @Test
  void selectionAndSorting() {
    tableModel.refresh();
    assertTrue(tableModelContainsAll(ITEMS, false, tableModel));

    //test selection and filtering together
    final SwingTableSelectionModel<List<String>> selectionModel = tableModel.getSelectionModel();
    tableModel.getSelectionModel().addSelectedIndexes(singletonList(3));
    assertEquals(3, selectionModel.getMinSelectionIndex());

    tableModel.getColumnFilterModel(0).setEqualValue("d");
    tableModel.getColumnFilterModel(0).setEnabled(false);

    selectionModel.setSelectedIndexes(singletonList(3));
    assertEquals(3, selectionModel.getMinSelectionIndex());
    assertEquals(ITEMS.get(2), selectionModel.getSelectedItem());

    tableModel.getSortModel().setSortingDirective(0, SortingDirective.ASCENDING);
    assertEquals(ITEMS.get(2), selectionModel.getSelectedItem());
    assertEquals(2, selectionModel.getMinSelectionIndex());

    tableModel.getSelectionModel().setSelectedIndexes(singletonList(0));
    assertEquals(ITEMS.get(0), selectionModel.getSelectedItem());
    tableModel.getSortModel().setSortingDirective(0, SortingDirective.DESCENDING);
    assertEquals(4, selectionModel.getMinSelectionIndex());

    assertEquals(singletonList(4), selectionModel.getSelectedIndexes());
    assertEquals(ITEMS.get(0), selectionModel.getSelectedItem());
    assertEquals(4, selectionModel.getMinSelectionIndex());
    assertEquals(ITEMS.get(0), selectionModel.getSelectedItem());
  }

  @Test
  void selectionAndFiltering() {
    tableModel.refresh();
    tableModel.getSelectionModel().addSelectedIndexes(singletonList(3));
    assertEquals(3, tableModel.getSelectionModel().getMinSelectionIndex());

    tableModel.getColumnFilterModel(0).setEqualValue("d");
    assertEquals(0, tableModel.getSelectionModel().getMinSelectionIndex());
    assertEquals(singletonList(0), tableModel.getSelectionModel().getSelectedIndexes());
    tableModel.getColumnFilterModel(0).setEnabled(false);
    assertEquals(0, tableModel.getSelectionModel().getMinSelectionIndex());
    assertEquals(ITEMS.get(3), tableModel.getSelectionModel().getSelectedItem());
  }

  @Test
  void setIncludeCondition() {
    tableModel.refresh();
    tableModel.setIncludeCondition(item -> false);
    assertEquals(0, tableModel.getRowCount());
  }

  @Test
  void columns() {
    assertEquals(1, tableModel.getColumnCount());
  }

  @Test
  void filterAndRemove() {
    tableModel.refresh();
    tableModel.getColumnFilterModel(0).setEqualValue("a");
    assertTrue(tableModel.containsItem(B));
    tableModel.removeItem(B);
    assertFalse(tableModel.containsItem(B));
    tableModel.removeItem(A);
    assertFalse(tableModel.containsItem(A));
  }

  @Test
  void filtering() throws Exception {
    final AtomicInteger done = new AtomicInteger();
    final EventListener listener = done::incrementAndGet;
    tableModel.addFilterListener(listener);

    tableModel.refresh();
    assertTrue(tableModelContainsAll(ITEMS, false, tableModel));
    assertNotNull(tableModel.getIncludeCondition());

    //test filters
    assertTrue(tableModel.isVisible(B));
    tableModel.getColumnFilterModel(0).setEqualValue("a");
    assertEquals(2, done.get());
    assertTrue(tableModel.isVisible(A));
    assertFalse(tableModel.isVisible(B));
    assertTrue(tableModel.isFiltered(D));
    assertFalse(tableModel.isVisible(B));
    assertTrue(tableModel.containsItem(B));
    assertTrue(tableModel.getColumnFilterModel(0).isEnabled());
    assertEquals(4, tableModel.getFilteredItemCount());
    assertFalse(tableModelContainsAll(ITEMS, false, tableModel));
    assertTrue(tableModelContainsAll(ITEMS, true, tableModel));

    assertTrue(tableModel.getVisibleItems().size() > 0);
    assertTrue(tableModel.getFilteredItems().size() > 0);
    assertTrue(tableModel.getItems().size() > 0);

    tableModel.getColumnFilterModel(0).setEnabled(false);
    assertEquals(3, done.get());
    assertFalse(tableModel.getColumnFilterModel(0).isEnabled());

    assertTrue(tableModelContainsAll(ITEMS, false, tableModel));

    tableModel.getColumnFilterModel(0).setEqualValue("t"); // ekki til
    assertTrue(tableModel.getColumnFilterModel(0).isEnabled());
    assertEquals(5, tableModel.getFilteredItemCount());
    assertFalse(tableModelContainsAll(ITEMS, false, tableModel));
    assertTrue(tableModelContainsAll(ITEMS, true, tableModel));
    tableModel.getColumnFilterModel(0).setEnabled(false);
    assertTrue(tableModelContainsAll(ITEMS, false, tableModel));
    assertFalse(tableModel.getColumnFilterModel(0).isEnabled());

    tableModel.getColumnFilterModel(0).setEqualValue("b");
    final int rowCount = tableModel.getRowCount();
    tableModel.addItemsAt(0, singletonList(singletonList("x")));
    assertEquals(rowCount, tableModel.getRowCount());

    tableModel.removeFilterListener(listener);
  }

  @Test
  void getValues() {
    tableModel.refresh();
    tableModel.getSelectionModel().setSelectedIndexes(asList(0, 2));
    Collection<String> values = tableModel.getSelectedValues(0);
    assertEquals(2, values.size());
    assertTrue(values.contains("a"));
    assertTrue(values.contains("c"));

    values = tableModel.getValues(0);
    assertEquals(5, values.size());
    assertTrue(values.contains("a"));
    assertTrue(values.contains("b"));
    assertTrue(values.contains("c"));
    assertTrue(values.contains("d"));
    assertTrue(values.contains("e"));
    assertFalse(values.contains("zz"));
  }

  @Test
  void columnModel() {
    final TableColumn column = tableModel.getColumnModel().getTableColumn(0);
    assertEquals(0, column.getIdentifier());
  }

  @Test
  void getColumnClass() {
    assertEquals(String.class, tableModel.getColumnClass(0));
  }

  private static boolean tableModelContainsAll(final List<List<String>> rows, final boolean includeFiltered,
                                               final AbstractFilteredTableModel<List<String>, Integer> model) {
    for (final List<String> row : rows) {
      if (includeFiltered) {
        if (!model.containsItem(row)) {
          return false;
        }
      }
      else if (!model.isVisible(row)) {
        return false;
      }
    }

    return true;
  }
}
