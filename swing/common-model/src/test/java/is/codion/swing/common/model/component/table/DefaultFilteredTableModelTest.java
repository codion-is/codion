/*
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.component.table;

import is.codion.common.Separators;
import is.codion.common.event.Event;
import is.codion.common.state.State;
import is.codion.swing.common.model.component.table.DefaultFilteredTableSearchModel.DefaultRowColumn;
import is.codion.swing.common.model.component.table.FilteredTableSearchModel.RowColumn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.SortOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static org.junit.jupiter.api.Assertions.*;

/**
 * User: Björn Darri
 * Date: 25.7.2010
 * Time: 13:54:59
 */
public final class DefaultFilteredTableModelTest {

  private static final TestRow A = new TestRow("a");
  private static final TestRow B = new TestRow("b");
  private static final TestRow C = new TestRow("c");
  private static final TestRow D = new TestRow("d");
  private static final TestRow E = new TestRow("e");
  private static final TestRow F = new TestRow("f");
  private static final TestRow G = new TestRow("g");
  private static final TestRow NULL = new TestRow(null);
  private static final List<TestRow> ITEMS = unmodifiableList(asList(A, B, C, D, E));

  private FilteredTableModel<TestRow, Integer> tableModel;

  private static final class TestRow {
    private final String value;

    private TestRow(String value) {
      this.value = value;
    }
  }

  private static FilteredTableModel<TestRow, Integer> createTestModel(Comparator<String> customComparator) {
    return FilteredTableModel.<TestRow, Integer>builder(() -> createColumns(customComparator), (row, columnIdentifier) -> row.value)
            .itemSupplier(() -> ITEMS)
            .build();
  }

  private static List<FilteredTableColumn<Integer>> createColumns(Comparator<String> customComparator) {
    Comparator<?> comparator = customComparator;
    if (comparator == null) {
      comparator = Comparator.comparing(Object::toString);
    }

    return singletonList(FilteredTableColumn.builder(0)
            .comparator(comparator)
            .columnClass(String.class)
            .build());
  }

  @BeforeEach
  void setUp() {
    tableModel = createTestModel(null);
  }

  @Test
  void getColumnCount() {
    assertEquals(1, tableModel.getColumnCount());
  }

  @Test
  void filterItems() {
    tableModel.refresh();
    tableModel.includeCondition().set(item -> !item.equals(B) && !item.equals(F));
    assertFalse(tableModel.visible(B));
    assertTrue(tableModel.containsItem(B));
    tableModel.addItemsAt(0, Collections.singletonList(F));
    tableModel.sortModel().setSortOrder(0, SortOrder.DESCENDING);
    assertFalse(tableModel.visible(F));
    assertTrue(tableModel.containsItem(F));
    tableModel.includeCondition().set(null);
    assertTrue(tableModel.visible(B));
    assertTrue(tableModel.visible(F));
  }

  @Test
  void filterModel() {
    tableModel.refresh();
    assertEquals(5, tableModel.visibleCount());
    tableModel.filterModel().conditionModel(0).setEqualValue("a");
    assertEquals(1, tableModel.visibleCount());
    tableModel.filterModel().conditionModel(0).setEqualValue("b");
    assertEquals(1, tableModel.visibleCount());
    tableModel.filterModel().conditionModel(0).clear();
  }

  @Test
  void addItemsAt() {
    tableModel.refresh();
    tableModel.addItemsAt(2, asList(F, G));
    assertEquals(2, tableModel.indexOf(F));
    assertEquals(3, tableModel.indexOf(G));
    assertEquals(4, tableModel.indexOf(C));
  }

  @Test
  void nullColumnFactory() {
    assertThrows(NullPointerException.class, () ->
            FilteredTableModel.<String, Integer>builder(null, (row, columnIdentifier) -> null));
  }

  @Test
  void nullColumnValueProvider() {
    assertThrows(NullPointerException.class, () -> FilteredTableModel.<String, Integer>builder(() -> null, null));
  }

  @Test
  void noColumns() {
    assertThrows(IllegalArgumentException.class, () ->
            FilteredTableModel.<String, Integer>builder(Collections::emptyList, (row, columnIdentifier) -> null)
                    .build());
  }

  @Test
  void refreshEvents() {
    AtomicInteger done = new AtomicInteger();
    AtomicInteger cleared = new AtomicInteger();
    Runnable successfulListener = done::incrementAndGet;
    Consumer<Throwable> failedListener = exception -> {};
    Runnable clearedListener = cleared::incrementAndGet;
    tableModel.refresher().addRefreshListener(successfulListener);
    tableModel.refresher().addRefreshFailedListener(failedListener);
    tableModel.addClearListener(clearedListener);
    tableModel.refresh();
    assertTrue(tableModel.getRowCount() > 0);
    assertEquals(1, done.get());
    assertEquals(1, cleared.get());

    done.set(0);
    cleared.set(0);
    tableModel.mergeOnRefresh().set(true);
    tableModel.refresh();
    assertEquals(1, done.get());
    assertEquals(0, cleared.get());

    tableModel.refresher().removeRefreshListener(successfulListener);
    tableModel.refresher().removeRefreshFailedListener(failedListener);
    tableModel.removeClearListener(clearedListener);
  }

  @Test
  void mergeOnRefresh() {
    AtomicInteger selectionEvents = new AtomicInteger();
    List<TestRow> items = new ArrayList<>(ITEMS);
    FilteredTableModel<TestRow, Integer> testModel =
            FilteredTableModel.<TestRow, Integer>builder(() -> createColumns(null), (row, columnIdentifier) -> row.value)
                    .itemSupplier(() -> items)
                    .build();
    testModel.selectionModel().addSelectionListener(selectionEvents::incrementAndGet);
    testModel.mergeOnRefresh().set(true);
    testModel.refresh();
    testModel.sortModel().setSortOrder(0, SortOrder.ASCENDING);
    testModel.selectionModel().setSelectedIndex(1);//b

    assertEquals(1, selectionEvents.get());
    assertSame(B, testModel.selectionModel().getSelectedItem());

    items.remove(C);
    testModel.refresh();
    assertEquals(1, selectionEvents.get());

    items.remove(B);
    testModel.refresh();
    assertTrue(testModel.selectionModel().isSelectionEmpty());
    assertEquals(2, selectionEvents.get());

    testModel.selectionModel().setSelectedItem(E);
    assertEquals(3, selectionEvents.get());

    testModel.includeCondition().set(item -> !item.equals(E));
    assertEquals(4, selectionEvents.get());

    items.add(B);

    testModel.refresh();
    //merge does not sort new items
    testModel.sortItems();

    testModel.selectionModel().setSelectedIndex(1);//b

    assertEquals(5, selectionEvents.get());
    assertSame(B, testModel.selectionModel().getSelectedItem());
  }

  @Test
  void removeItems() {
    AtomicInteger events = new AtomicInteger();
    Runnable listener = events::incrementAndGet;
    tableModel.addDataChangedListener(listener);
    tableModel.refresh();
    assertEquals(1, events.get());
    tableModel.filterModel().conditionModel(0).setEqualValue("a");
    tableModel.removeItem(B);
    assertEquals(3, events.get());
    assertFalse(tableModel.visible(B));
    assertTrue(tableModel.containsItem(A));
    tableModel.removeItem(A);
    assertEquals(4, events.get());
    assertFalse(tableModel.containsItem(A));
    tableModel.removeItems(asList(D, E));
    assertEquals(4, events.get());//no change event when removing filtered items
    assertFalse(tableModel.visible(D));
    assertFalse(tableModel.visible(E));
    assertFalse(tableModel.filtered(D));
    assertFalse(tableModel.filtered(E));
    tableModel.filterModel().conditionModel(0).setEqualValue(null);
    tableModel.refresh();
    assertEquals(7, events.get());
    tableModel.removeItems(0, 2);
    assertEquals(8, events.get());//just a single event when removing multiple items
    tableModel.removeItemAt(0);
    assertEquals(9, events.get());
    tableModel.removeDataChangedListener(listener);
  }

  @Test
  void setItemAt() {
    AtomicInteger dataChangedEvents = new AtomicInteger();
    Runnable listener = dataChangedEvents::incrementAndGet;
    tableModel.addDataChangedListener(listener);
    State selectionChangedState = State.state();
    tableModel.selectionModel().addSelectedItemListener((item) -> selectionChangedState.set(true));
    tableModel.refresh();
    assertEquals(1, dataChangedEvents.get());
    tableModel.selectionModel().setSelectedItem(B);
    TestRow h = new TestRow("h");
    tableModel.setItemAt(tableModel.indexOf(B), h);
    assertEquals(2, dataChangedEvents.get());
    assertEquals(h, tableModel.selectionModel().getSelectedItem());
    assertTrue(selectionChangedState.get());
    tableModel.setItemAt(tableModel.indexOf(h), B);
    assertEquals(3, dataChangedEvents.get());

    selectionChangedState.set(false);
    TestRow newB = new TestRow("b");
    tableModel.setItemAt(tableModel.indexOf(B), newB);
    assertFalse(selectionChangedState.get());
    assertEquals(newB, tableModel.selectionModel().getSelectedItem());
    tableModel.removeDataChangedListener(listener);
  }

  @Test
  void removeItemsRange() {
    AtomicInteger events = new AtomicInteger();
    Runnable listener = events::incrementAndGet;
    tableModel.addDataChangedListener(listener);
    tableModel.refresh();
    assertEquals(1, events.get());
    List<TestRow> removed = tableModel.removeItems(1, 3);
    assertEquals(2, events.get());
    assertTrue(tableModel.containsItem(A));
    assertFalse(tableModel.containsItem(B));
    assertTrue(removed.contains(B));
    assertFalse(tableModel.containsItem(C));
    assertTrue(removed.contains(C));
    assertTrue(tableModel.containsItem(D));
    assertTrue(tableModel.containsItem(E));

    tableModel.mergeOnRefresh().set(true);
    events.set(0);
    tableModel.refresh();
    assertEquals(5, events.get());

    tableModel.removeDataChangedListener(listener);
  }

  @Test
  void searchModel() {
    final class Row implements Comparable<Row> {

      private final int id;
      private final String value;

      Row(int id, String value) {
        this.id = id;
        this.value = value;
      }

      @Override
      public int compareTo(Row o) {
        return value.compareTo(o.value);
      }
    }

    FilteredTableColumn<Integer> columnId = FilteredTableColumn.builder(0)
            .columnClass(Integer.class)
            .build();
    FilteredTableColumn<Integer> columnValue = FilteredTableColumn.builder(1)
            .columnClass(String.class)
            .build();

    List<Row> items = asList(
            new Row(0, "a"),
            new Row(1, "b"),
            new Row(2, "c"),
            new Row(3, "d"),
            new Row(4, "e")
    );

    FilteredTableModel<Row, Integer> testModel =
            FilteredTableModel.<Row, Integer>builder(() -> asList(columnId, columnValue), (row, columnIdentifier) -> {
              if (columnIdentifier == 0) {
                return row.id;
              }

              return row.value;
            }).itemSupplier(() -> items).build();

    testModel.refresh();
    FilteredTableSearchModel searchModel = testModel.searchModel();
    searchModel.searchString().set("b");
    RowColumn rowColumn = searchModel.nextResult().orElse(null);
    assertEquals(new DefaultRowColumn(1, 1), rowColumn);
    searchModel.searchString().set("e");
    rowColumn = searchModel.nextResult().orElse(null);
    assertEquals(new DefaultRowColumn(4, 1), rowColumn);
    searchModel.searchString().set("c");
    rowColumn = searchModel.previousResult().orElse(null);
    assertEquals(new DefaultRowColumn(2, 1), rowColumn);
    searchModel.searchString().set("x");
    rowColumn = searchModel.nextResult().orElse(null);
    assertNull(rowColumn);

    testModel.sortModel().setSortOrder(1, SortOrder.DESCENDING);

    searchModel.searchString().set("b");
    rowColumn = searchModel.nextResult().orElse(null);
    assertEquals(new DefaultRowColumn(3, 1), rowColumn);
    searchModel.searchString().set("e");
    rowColumn = searchModel.previousResult().orElse(null);
    assertEquals(new DefaultRowColumn(0, 1), rowColumn);

    searchModel.regularExpression().set(true);
    searchModel.searchString().set("(?i)B");
    rowColumn = searchModel.nextResult().orElse(null);
    assertEquals(new DefaultRowColumn(3, 1), rowColumn);

    Predicate<String> predicate = item -> item.equals("b") || item.equals("e");

    searchModel.searchPredicate().set(predicate);
    rowColumn = searchModel.selectPreviousResult().orElse(null);
    assertEquals(new DefaultRowColumn(3, 1), rowColumn);
    rowColumn = searchModel.selectPreviousResult().orElse(null);
    assertEquals(new DefaultRowColumn(0, 1), rowColumn);

    assertEquals(Arrays.asList(
            new DefaultRowColumn(0, 1),
            new DefaultRowColumn(3, 1)
    ), searchModel.searchResults());

    testModel.sortModel().setSortOrder(1, SortOrder.ASCENDING);
    testModel.columnModel().moveColumn(1, 0);

    testModel.refresh();
    searchModel.searchString().set("b");
    rowColumn = searchModel.nextResult().orElse(null);
    assertEquals(new DefaultRowColumn(1, 0), rowColumn);
    searchModel.searchString().set("e");
    rowColumn = searchModel.nextResult().orElse(null);
    assertEquals(new DefaultRowColumn(4, 0), rowColumn);
    searchModel.searchString().set("c");
    rowColumn = searchModel.previousResult().orElse(null);
    assertEquals(new DefaultRowColumn(2, 0), rowColumn);
    searchModel.searchString().set("x");
    rowColumn = searchModel.nextResult().orElse(null);
    assertNull(rowColumn);

    testModel.sortModel().setSortOrder(0, SortOrder.DESCENDING);

    searchModel.searchString().set("b");
    rowColumn = searchModel.nextResult().orElse(null);
    assertEquals(new DefaultRowColumn(3, 0), rowColumn);
    searchModel.searchString().set("e");
    rowColumn = searchModel.previousResult().orElse(null);
    assertEquals(new DefaultRowColumn(0, 0), rowColumn);

    searchModel.regularExpression().set(true);
    searchModel.searchString().set("(?i)B");
    rowColumn = searchModel.nextResult().orElse(null);
    assertEquals(new DefaultRowColumn(3, 0), rowColumn);

    predicate = item -> item.equals("b") || item.equals("e");

    searchModel.searchPredicate().set(predicate);
    rowColumn = searchModel.selectPreviousResult().orElse(null);
    assertEquals(new DefaultRowColumn(3, 0), rowColumn);
    rowColumn = searchModel.selectPreviousResult().orElse(null);
    assertEquals(new DefaultRowColumn(0, 0), rowColumn);

    assertEquals(2, testModel.selectionModel().selectionCount());

    searchModel.selectPreviousResult();
    searchModel.selectNextResult();
    searchModel.selectNextResult();
    searchModel.selectNextResult();

    rowColumn = searchModel.selectPreviousResult().orElse(null);
    rowColumn = searchModel.selectNextResult().orElse(null);
    rowColumn = searchModel.selectNextResult().orElse(null);

    assertEquals(Arrays.asList(
            new DefaultRowColumn(0, 0),
            new DefaultRowColumn(3, 0)
    ), searchModel.searchResults());
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
    FilteredTableModel<TestRow, Integer> tableModel = createTestModel(Comparator.reverseOrder());
    tableModel.refresh();
    FilteredTableSortModel<TestRow, Integer> sortModel = tableModel.sortModel();
    sortModel.setSortOrder(0, SortOrder.ASCENDING);
    assertEquals(E, tableModel.itemAt(0));
    sortModel.setSortOrder(0, SortOrder.DESCENDING);
    assertEquals(A, tableModel.itemAt(0));
  }

  @Test
  void sorting() {
    AtomicInteger actionsPerformed = new AtomicInteger();
    Consumer<Integer> listener = columnIdentifier -> actionsPerformed.incrementAndGet();
    tableModel.sortModel().addSortingChangedListener(listener);

    tableModel.refresh();
    FilteredTableSortModel<TestRow, Integer> sortModel = tableModel.sortModel();
    sortModel.setSortOrder(0, SortOrder.DESCENDING);
    assertEquals(SortOrder.DESCENDING, sortModel.sortOrder(0));
    assertEquals(E, tableModel.itemAt(0));
    assertEquals(1, actionsPerformed.get());
    sortModel.setSortOrder(0, SortOrder.ASCENDING);
    assertEquals(SortOrder.ASCENDING, sortModel.sortOrder(0));
    assertEquals(A, tableModel.itemAt(0));
    assertEquals(0, sortModel.columnSortOrder().get(0).columnIdentifier());
    assertEquals(2, actionsPerformed.get());

    sortModel.setSortOrder(0, SortOrder.DESCENDING);
    tableModel.refresh();
    assertEquals(A, tableModel.itemAt(4));
    assertEquals(E, tableModel.itemAt(0));
    sortModel.setSortOrder(0, SortOrder.ASCENDING);

    List<TestRow> items = new ArrayList<>();
    items.add(NULL);
    tableModel.addItemsAt(0, items);
    sortModel.setSortOrder(0, SortOrder.ASCENDING);
    assertEquals(0, tableModel.indexOf(NULL));
    sortModel.setSortOrder(0, SortOrder.DESCENDING);
    assertEquals(tableModel.getRowCount() - 1, tableModel.indexOf(NULL));

    tableModel.refresh();
    items.add(NULL);
    tableModel.addItemsAt(0, items);
    sortModel.setSortOrder(0, SortOrder.ASCENDING);
    assertEquals(0, tableModel.indexOf(NULL));
    sortModel.setSortOrder(0, SortOrder.DESCENDING);
    assertEquals(tableModel.getRowCount() - 2, tableModel.indexOf(NULL));
    sortModel.setSortOrder(0, SortOrder.UNSORTED);
    tableModel.sortModel().removeSortingChangedListener(listener);
  }

  @Test
  void addSelectedIndexesNegative() {
    Collection<Integer> indexes = new ArrayList<>();
    indexes.add(1);
    indexes.add(-1);
    assertThrows(IndexOutOfBoundsException.class, () -> tableModel.selectionModel().addSelectedIndexes(indexes));
  }

  @Test
  void addSelectedIndexesOutOfBounds() {
    Collection<Integer> indexes = new ArrayList<>();
    indexes.add(1);
    indexes.add(10);
    assertThrows(IndexOutOfBoundsException.class, () -> tableModel.selectionModel().addSelectedIndexes(indexes));
  }

  @Test
  void setSelectedIndexesNegative() {
    Collection<Integer> indexes = new ArrayList<>();
    indexes.add(1);
    indexes.add(-1);
    assertThrows(IndexOutOfBoundsException.class, () -> tableModel.selectionModel().setSelectedIndexes(indexes));
  }

  @Test
  void setSelectedIndexesOutOfBounds() {
    Collection<Integer> indexes = new ArrayList<>();
    indexes.add(1);
    indexes.add(10);
    assertThrows(IndexOutOfBoundsException.class, () -> tableModel.selectionModel().setSelectedIndexes(indexes));
  }

  @Test
  void setSelectedIndexNegative() {
    assertThrows(IndexOutOfBoundsException.class, () -> tableModel.selectionModel().setSelectedIndex(-1));
  }

  @Test
  void setSelectedIndexOutOfBounds() {
    assertThrows(IndexOutOfBoundsException.class, () -> tableModel.selectionModel().setSelectedIndex(10));
  }

  @Test
  void addSelectedIndexNegative() {
    assertThrows(IndexOutOfBoundsException.class, () -> tableModel.selectionModel().addSelectedIndex(-1));
  }

  @Test
  void addSelectedIndexOutOfBounds() {
    assertThrows(IndexOutOfBoundsException.class, () -> tableModel.selectionModel().addSelectedIndex(10));
  }

  @Test
  void selection() {
    AtomicInteger events = new AtomicInteger();
    Runnable listener = events::incrementAndGet;
    Consumer dataListener = Event.dataListener(listener);
    FilteredTableSelectionModel<TestRow> selectionModel = tableModel.selectionModel();
    selectionModel.addSelectedIndexListener(dataListener);
    selectionModel.addSelectionListener(listener);
    selectionModel.addSelectedItemListener(dataListener);
    selectionModel.addSelectedItemsListener(dataListener);

    assertFalse(selectionModel.singleSelection().get());
    assertTrue(selectionModel.selectionEmpty().get());
    assertFalse(selectionModel.selectionNotEmpty().get());
    assertFalse(selectionModel.multipleSelection().get());

    tableModel.refresh();
    selectionModel.setSelectedIndex(2);
    assertEquals(4, events.get());
    assertTrue(selectionModel.singleSelection().get());
    assertFalse(selectionModel.selectionEmpty().get());
    assertFalse(selectionModel.multipleSelection().get());
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
    assertFalse(selectionModel.singleSelection().get());
    assertTrue(selectionModel.selectionEmpty().get());
    assertFalse(selectionModel.multipleSelection().get());
    assertEquals(0, selectionModel.getSelectedItems().size());

    selectionModel.setSelectedItem(ITEMS.get(0));
    assertFalse(selectionModel.multipleSelection().get());
    assertEquals(ITEMS.get(0), selectionModel.getSelectedItem());
    assertEquals(0, selectionModel.getSelectedIndex());
    assertEquals(1, selectionModel.selectionCount());
    assertFalse(selectionModel.isSelectionEmpty());
    selectionModel.addSelectedIndex(1);
    assertTrue(selectionModel.multipleSelection().get());
    assertEquals(ITEMS.get(0), selectionModel.getSelectedItem());
    assertEquals(asList(0, 1), selectionModel.getSelectedIndexes());
    assertEquals(0, selectionModel.getSelectedIndex());
    selectionModel.addSelectedIndex(4);
    assertTrue(selectionModel.multipleSelection().get());
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
    assertEquals(3, selectionModel.selectionCount());
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
    assertEquals(1, selectionModel.selectionCount());
    assertEquals(0, selectionModel.getMinSelectionIndex());
    selectionModel.addSelectedItems(asList(ITEMS.get(1), ITEMS.get(2)));
    assertEquals(3, selectionModel.selectionCount());
    assertEquals(0, selectionModel.getMinSelectionIndex());
    selectionModel.removeSelectedItem(ITEMS.get(1));
    assertEquals(2, selectionModel.selectionCount());
    assertEquals(0, selectionModel.getMinSelectionIndex());
    selectionModel.removeSelectedItem(ITEMS.get(2));
    assertEquals(1, selectionModel.selectionCount());
    assertEquals(0, selectionModel.getMinSelectionIndex());
    selectionModel.addSelectedItems(asList(ITEMS.get(1), ITEMS.get(2)));
    assertEquals(3, selectionModel.selectionCount());
    assertEquals(0, selectionModel.getMinSelectionIndex());
    selectionModel.addSelectedItem(ITEMS.get(4));
    assertEquals(4, selectionModel.selectionCount());
    assertEquals(0, selectionModel.getMinSelectionIndex());
    tableModel.removeItem(ITEMS.get(0));
    assertEquals(3, selectionModel.selectionCount());
    assertEquals(0, selectionModel.getMinSelectionIndex());

    tableModel.clear();
    assertTrue(selectionModel.selectionEmpty().get());
    assertFalse(selectionModel.selectionNotEmpty().get());
    assertNull(selectionModel.getSelectedItem());

    selectionModel.clearSelection();
    selectionModel.removeSelectedIndexListener(dataListener);
    selectionModel.removeSelectionListener(listener);
    selectionModel.removeSelectedItemListener(dataListener);
    selectionModel.removeSelectedItemsListener(dataListener);
  }

  @Test
  void selectionAndSorting() {
    tableModel.refresh();
    assertTrue(tableModelContainsAll(ITEMS, false, tableModel));

    //test selection and filtering together
    FilteredTableSelectionModel<TestRow> selectionModel = tableModel.selectionModel();
    tableModel.selectionModel().addSelectedIndexes(singletonList(3));
    assertEquals(3, selectionModel.getMinSelectionIndex());

    tableModel.filterModel().conditionModel(0).setEqualValue("d");
    tableModel.filterModel().conditionModel(0).enabled().set(false);

    selectionModel.setSelectedIndexes(singletonList(3));
    assertEquals(3, selectionModel.getMinSelectionIndex());
    assertEquals(ITEMS.get(2), selectionModel.getSelectedItem());

    tableModel.sortModel().setSortOrder(0, SortOrder.ASCENDING);
    assertEquals(ITEMS.get(2), selectionModel.getSelectedItem());
    assertEquals(2, selectionModel.getMinSelectionIndex());

    tableModel.selectionModel().setSelectedIndexes(singletonList(0));
    assertEquals(ITEMS.get(0), selectionModel.getSelectedItem());
    tableModel.sortModel().setSortOrder(0, SortOrder.DESCENDING);
    assertEquals(4, selectionModel.getMinSelectionIndex());

    assertEquals(singletonList(4), selectionModel.getSelectedIndexes());
    assertEquals(ITEMS.get(0), selectionModel.getSelectedItem());
    assertEquals(4, selectionModel.getMinSelectionIndex());
    assertEquals(ITEMS.get(0), selectionModel.getSelectedItem());
  }

  @Test
  void selectionAndFiltering() {
    tableModel.refresh();
    tableModel.selectionModel().addSelectedIndexes(singletonList(3));
    assertEquals(3, tableModel.selectionModel().getMinSelectionIndex());

    tableModel.filterModel().conditionModel(0).setEqualValue("d");
    assertEquals(0, tableModel.selectionModel().getMinSelectionIndex());
    assertEquals(singletonList(0), tableModel.selectionModel().getSelectedIndexes());
    tableModel.filterModel().conditionModel(0).enabled().set(false);
    assertEquals(0, tableModel.selectionModel().getMinSelectionIndex());
    assertEquals(ITEMS.get(3), tableModel.selectionModel().getSelectedItem());
  }

  @Test
  void includeCondition() {
    tableModel.refresh();
    tableModel.includeCondition().set(item -> false);
    assertEquals(0, tableModel.getRowCount());
  }

  @Test
  void columns() {
    assertEquals(1, tableModel.getColumnCount());
  }

  @Test
  void filterAndRemove() {
    tableModel.refresh();
    tableModel.filterModel().conditionModel(0).setEqualValue("a");
    assertTrue(tableModel.containsItem(B));
    tableModel.removeItem(B);
    assertFalse(tableModel.containsItem(B));
    tableModel.removeItem(A);
    assertFalse(tableModel.containsItem(A));
  }

  @Test
  void filtering() {
    tableModel.refresh();
    assertTrue(tableModelContainsAll(ITEMS, false, tableModel));
    assertTrue(tableModel.includeCondition().isNull());

    //test filters
    assertNotNull(tableModel.filterModel().conditionModel(0));
    assertTrue(tableModel.visible(B));
    tableModel.filterModel().conditionModel(0).setEqualValue("a");
    assertTrue(tableModel.visible(A));
    assertFalse(tableModel.visible(B));
    assertTrue(tableModel.filtered(D));

    tableModel.includeCondition().set(strings -> !strings.equals(A));
    assertTrue(tableModel.includeCondition().isNotNull());
    assertFalse(tableModel.visible(A));
    tableModel.includeCondition().set(null);
    assertTrue(tableModel.visible(A));

    assertFalse(tableModel.visible(B));
    assertTrue(tableModel.containsItem(B));
    assertTrue(tableModel.filterModel().conditionModel(0).enabled().get());
    assertEquals(4, tableModel.filteredCount());
    assertFalse(tableModelContainsAll(ITEMS, false, tableModel));
    assertTrue(tableModelContainsAll(ITEMS, true, tableModel));

    assertFalse(tableModel.visibleItems().isEmpty());
    assertFalse(tableModel.filteredItems().isEmpty());
    assertFalse(tableModel.items().isEmpty());

    tableModel.filterModel().conditionModel(0).enabled().set(false);
    assertFalse(tableModel.filterModel().conditionModel(0).enabled().get());

    assertTrue(tableModelContainsAll(ITEMS, false, tableModel));

    tableModel.filterModel().conditionModel(0).setEqualValue("t"); // ekki til
    assertTrue(tableModel.filterModel().conditionModel(0).enabled().get());
    assertEquals(5, tableModel.filteredCount());
    assertFalse(tableModelContainsAll(ITEMS, false, tableModel));
    assertTrue(tableModelContainsAll(ITEMS, true, tableModel));
    tableModel.filterModel().conditionModel(0).enabled().set(false);
    assertTrue(tableModelContainsAll(ITEMS, false, tableModel));
    assertFalse(tableModel.filterModel().conditionModel(0).enabled().get());

    tableModel.filterModel().conditionModel(0).setEqualValue("b");
    int rowCount = tableModel.getRowCount();
    tableModel.addItemsAt(0, singletonList(new TestRow("x")));
    assertEquals(rowCount, tableModel.getRowCount());

    assertThrows(IllegalArgumentException.class, () -> tableModel.filterModel().conditionModel(1));
  }

  @Test
  void clearFilterModels() {
    assertFalse(tableModel.filterModel().enabled(0));
    tableModel.filterModel().conditionModel(0).setEqualValue("SCOTT");
    assertTrue(tableModel.filterModel().enabled(0));
    tableModel.filterModel().clear();
    assertFalse(tableModel.filterModel().enabled(0));
  }

  @Test
  void values() {
    tableModel.refresh();
    tableModel.selectionModel().setSelectedIndexes(asList(0, 2));
    Collection<String> values = tableModel.selectedValues(0);
    assertEquals(2, values.size());
    assertTrue(values.contains("a"));
    assertTrue(values.contains("c"));

    values = tableModel.values(0);
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
    FilteredTableColumn<Integer> column = tableModel.columnModel().getColumn(0);
    assertEquals(0, column.getIdentifier());
  }

  @Test
  void getColumnClass() {
    assertEquals(String.class, tableModel.getColumnClass(0));
  }

  @Test
  void tableDataAsDelimitedString() {
    tableModel.refresh();

    String expected = "0" + Separators.LINE_SEPARATOR +
            "a" + Separators.LINE_SEPARATOR +
            "b" + Separators.LINE_SEPARATOR +
            "c" + Separators.LINE_SEPARATOR +
            "d" + Separators.LINE_SEPARATOR +
            "e";
    assertEquals(expected, tableModel.rowsAsDelimitedString('\t'));
  }

  private static boolean tableModelContainsAll(List<TestRow> rows, boolean includeFiltered,
                                               FilteredTableModel<TestRow, Integer> model) {
    for (TestRow row : rows) {
      if (includeFiltered) {
        if (!model.containsItem(row)) {
          return false;
        }
      }
      else if (!model.visible(row)) {
        return false;
      }
    }

    return true;
  }
}
