/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.model.table;

import org.jminor.common.event.Event;
import org.jminor.common.event.EventDataListener;
import org.jminor.common.event.EventListener;
import org.jminor.common.event.Events;
import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.common.model.table.ColumnSummaryModel;
import org.jminor.common.model.table.DefaultColumnSummaryModel;
import org.jminor.common.model.table.FilteredTableModel;
import org.jminor.common.model.table.RowColumn;
import org.jminor.common.model.table.TableSortModel;

import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * A TableModel implementation that supports filtering, searching and sorting.
 * <pre>
 * AbstractFilteredTableModel tableModel = ...;
 * JTable table = new JTable(tableModel, tableModel.getColumnModel(), tableModel.getSelectionModel());
 * </pre><br>
 * User: Björn Darri<br>
 * Sorting functionality originally based on TableSorter by Philip Milne, Brendon McLean, Dan van Enckevort and Parwinder Sekhon<br>
 * Date: 18.4.2010<br>
 * Time: 09:48:07<br>
 * @param <R> the type representing the rows in this table model
 * @param <C> type type used to identify columns in this table model, Integer for simple indexed identification for example
 */
public abstract class AbstractFilteredTableModel<R, C> extends AbstractTableModel implements FilteredTableModel<R, C, TableColumn> {

  private final Event filterEvent = Events.event();
  private final Event sortEvent = Events.event();
  private final Event refreshStartedEvent = Events.event();
  private final Event refreshDoneEvent = Events.event();
  private final Event tableDataChangedEvent = Events.event();
  private final Event tableModelClearedEvent = Events.event();
  private final Event<List<Integer>> rowsDeletedEvent = Events.event();

  /**
   * Holds visible items
   */
  private final List<R> visibleItems = new ArrayList<>();

  /**
   * Holds items that are filtered
   */
  private final List<R> filteredItems = new ArrayList<>();

  /**
   * The selection model
   */
  private final SwingTableSelectionModel<R> selectionModel;

  /**
   * The TableColumnModel
   */
  private final SwingFilteredTableColumnModel<R, C> columnModel;

  /**
   * The sort model
   */
  private final TableSortModel<R, C, TableColumn> sortModel;

  /**
   * Maps PropertySummaryModels to their respective properties
   */
  private final Map<C, ColumnSummaryModel> columnSummaryModels = new HashMap<>();

  /**
   * the include condition used by this model
   */
  private Predicate<R> includeCondition;

  /**
   * true if searching the table model should be done via regular expressions
   */
  private boolean regularExpressionSearch = false;

  /**
   * Instantiates a new table model.
   * @param sortModel the sort model to use
   * @throws NullPointerException in case {@code sortModel} is null
   */
  public AbstractFilteredTableModel(final TableSortModel<R, C, TableColumn> sortModel) {
    this(sortModel, null);
  }

  /**
   * Instantiates a new table model.
   * @param sortModel the sort model to use
   * @param columnFilterModels the column filter models
   * @throws NullPointerException in case {@code sortModel} is null
   */
  public AbstractFilteredTableModel(final TableSortModel<R, C, TableColumn> sortModel,
                                    final Collection<? extends ColumnConditionModel<R, C>> columnFilterModels) {
    this.sortModel = requireNonNull(sortModel, "sortModel");
    this.columnModel = new SwingFilteredTableColumnModel<>(sortModel.getColumns(), columnFilterModels);
    this.selectionModel = new SwingTableSelectionModel<>(this);
    this.includeCondition = new DefaultIncludeCondition<>(this.columnModel.getColumnFilterModels());
    bindEventsInternal();
  }

  @Override
  public final List<R> getItems() {
    final List<R> items = new ArrayList<>(visibleItems);
    items.addAll(filteredItems);

    return unmodifiableList(items);
  }

  @Override
  public final List<R> getVisibleItems() {
    return unmodifiableList(visibleItems);
  }

  @Override
  public final List<R> getFilteredItems() {
    return unmodifiableList(filteredItems);
  }

  @Override
  public final int getVisibleItemCount() {
    return getRowCount();
  }

  @Override
  public final int getFilteredItemCount() {
    return filteredItems.size();
  }

  @Override
  public final int getColumnCount() {
    return columnModel.getColumnCount();
  }

  @Override
  public final int getRowCount() {
    return visibleItems.size();
  }

  @Override
  public final boolean containsItem(final R item) {
    return visibleItems.contains(item) || filteredItems.contains(item);
  }

  @Override
  public final boolean isVisible(final R item) {
    return visibleItems.contains(item);
  }

  @Override
  public final boolean isFiltered(final R item) {
    return filteredItems.contains(item);
  }

  @Override
  public final RowColumn findNext(final int fromRowIndex, final String searchText) {
    return findNext(fromRowIndex, getSearchCondition(searchText));
  }

  @Override
  public final RowColumn findPrevious(final int fromRowIndex, final String searchText) {
    return findPrevious(fromRowIndex, getSearchCondition(searchText));
  }

  @Override
  public final RowColumn findNext(final int fromRowIndex, final Predicate<String> condition) {
    for (int row = fromRowIndex >= getVisibleItemCount() ? 0 : fromRowIndex; row < getVisibleItemCount(); row++) {
      final RowColumn coordinate = findColumnValue(row, condition);
      if (coordinate != null) {
        return coordinate;
      }
    }

    return null;
  }

  @Override
  public final RowColumn findPrevious(final int fromRowIndex, final Predicate<String> condition) {
    for (int row = fromRowIndex < 0 ? getVisibleItemCount() - 1 : fromRowIndex; row >= 0; row--) {
      final RowColumn coordinate = findColumnValue(row, condition);
      if (coordinate != null) {
        return coordinate;
      }
    }

    return null;
  }

  /**
   * Refreshes the data in this table model, respecting the selection, filtering as well
   * as sorting states.
   */
  @Override
  public final void refresh() {
    try {
      refreshStartedEvent.onEvent();
      final List<R> selectedItems = new ArrayList<>(selectionModel.getSelectedItems());
      doRefresh();
      selectionModel.setSelectedItems(selectedItems);
    }
    finally {
      refreshDoneEvent.onEvent();
    }
  }

  @Override
  public final void clear() {
    filteredItems.clear();
    final int size = visibleItems.size();
    if (size > 0) {
      visibleItems.clear();
      fireTableRowsDeleted(0, size - 1);
    }
    tableModelClearedEvent.onEvent();
  }

  @Override
  public final SwingTableSelectionModel<R> getSelectionModel() {
    return selectionModel;
  }

  @Override
  public final TableSortModel<R, C, TableColumn> getSortModel() {
    return sortModel;
  }

  @Override
  public final ColumnSummaryModel getColumnSummaryModel(final C columnIdentifier) {
    if (!columnSummaryModels.containsKey(columnIdentifier)) {
      columnSummaryModels.put(columnIdentifier, new DefaultColumnSummaryModel(createColumnValueProvider(columnIdentifier)));
    }

    return columnSummaryModels.get(columnIdentifier);
  }

  @Override
  public final Collection getValues(final C columnIdentifier) {
    return getColumnValues(IntStream.range(0, getVisibleItemCount()).boxed(),
            columnModel.getTableColumn(columnIdentifier).getModelIndex());
  }

  @Override
  public final Collection getSelectedValues(final C columnIdentifier) {
    return getColumnValues(getSelectionModel().getSelectedIndexes().stream(),
            columnModel.getTableColumn(columnIdentifier).getModelIndex());
  }

  @Override
  public final boolean isRegularExpressionSearch() {
    return regularExpressionSearch;
  }

  @Override
  public final void setRegularExpressionSearch(final boolean regularExpressionSearch) {
    this.regularExpressionSearch = regularExpressionSearch;
  }

  @Override
  public final R getItemAt(final int index) {
    return visibleItems.get(index);
  }

  @Override
  public final int indexOf(final R item) {
    return visibleItems.indexOf(item);
  }

  @Override
  public final void sort() {
    final List<R> selectedItems = new ArrayList<>(selectionModel.getSelectedItems());
    sortModel.sort(visibleItems);
    fireTableRowsUpdated(0, visibleItems.size());
    selectionModel.setSelectedItems(selectedItems);
    sortEvent.onEvent();
  }

  @Override
  public final void filterContents() {
    final List<R> selectedItems = selectionModel.getSelectedItems();
    visibleItems.addAll(filteredItems);
    filteredItems.clear();
    if (includeCondition != null) {
      for (final ListIterator<R> iterator = visibleItems.listIterator(); iterator.hasNext(); ) {
        final R item = iterator.next();
        if (!includeCondition.test(item)) {
          filteredItems.add(item);
          iterator.remove();
        }
      }
    }
    sortModel.sort(visibleItems);
    fireTableDataChanged();
    selectionModel.setSelectedItems(selectedItems);
    filterEvent.onEvent();
  }

  @Override
  public final Predicate<R> getIncludeCondition() {
    return includeCondition;
  }

  @Override
  public final void setIncludeCondition(final Predicate<R> includeCondition) {
    this.includeCondition = includeCondition;
    filterContents();
  }

  @Override
  public final void removeItem(final R item) {
    final int visibleItemIndex = visibleItems.indexOf(item);
    if (visibleItemIndex >= 0) {
      visibleItems.remove(visibleItemIndex);
      fireTableRowsDeleted(visibleItemIndex, visibleItemIndex);
    }
    else {
      final int filteredIndex = filteredItems.indexOf(item);
      if (filteredIndex >= 0) {
        filteredItems.remove(item);
      }
    }
  }

  @Override
  public final void removeItems(final Collection<R> items) {
    for (final R item : items) {
      removeItem(item);
    }
  }

  @Override
  public final void removeItems(final int fromIndex, final int toIndex) {
    visibleItems.subList(fromIndex, toIndex).clear();
    fireTableRowsDeleted(fromIndex, toIndex);
  }

  /**
   * A default implementation returning true
   * @return true
   */
  @Override
  public boolean allowSelectionChange() {
    return true;
  }

  @Override
  public final void addRowsDeletedListener(final EventDataListener<List<Integer>> listener) {
    rowsDeletedEvent.addDataListener(listener);
  }

  @Override
  public final void removeRowsDeletedListener(final EventDataListener<List<Integer>> listener) {
    rowsDeletedEvent.removeDataListener(listener);
  }

  @Override
  public final SwingFilteredTableColumnModel<R, C> getColumnModel() {
    return columnModel;
  }

  @Override
  public final Class getColumnClass(final int columnIndex) {
    return sortModel.getColumnClass(getColumnModel().getColumnIdentifier(columnIndex));
  }

  @Override
  public final void addRefreshStartedListener(final EventListener listener) {
    refreshStartedEvent.addListener(listener);
  }

  @Override
  public final void removeRefreshStartedListener(final EventListener listener) {
    refreshStartedEvent.removeListener(listener);
  }

  @Override
  public final void addRefreshDoneListener(final EventListener listener) {
    refreshDoneEvent.addListener(listener);
  }

  @Override
  public final void removeRefreshDoneListener(final EventListener listener) {
    refreshDoneEvent.removeListener(listener);
  }

  @Override
  public final void addFilteringListener(final EventListener listener) {
    filterEvent.addListener(listener);
  }

  @Override
  public final void removeFilteringListener(final EventListener listener) {
    filterEvent.removeListener(listener);
  }

  @Override
  public final void addSortListener(final EventListener listener) {
    sortEvent.addListener(listener);
  }

  @Override
  public final void removeSortListener(final EventListener listener) {
    sortEvent.removeListener(listener);
  }

  @Override
  public final void addTableDataChangedListener(final EventListener listener) {
    tableDataChangedEvent.addListener(listener);
  }

  @Override
  public final void removeTableDataChangedListener(final EventListener listener) {
    tableDataChangedEvent.removeListener(listener);
  }

  @Override
  public final void addTableModelClearedListener(final EventListener listener) {
    tableModelClearedEvent.addListener(listener);
  }

  @Override
  public final void removeTableModelClearedListener(final EventListener listener) {
    tableModelClearedEvent.removeListener(listener);
  }

  /**
   * Refreshes the data in this table model.
   * @see #clear()
   * @see #addItems(List, boolean, boolean)
   * @see #addItems(List, int, boolean)
   */
  protected abstract void doRefresh();

  /**
   * Creates a ColumnValueProvider for the given column
   * @param columnIdentifier the column identifier
   * @return a ColumnValueProvider for the column identified by {@code columnIdentifier}
   */
  protected ColumnSummaryModel.ColumnValueProvider createColumnValueProvider(final C columnIdentifier) {
    return new DefaultColumnValueProvider(columnIdentifier, this, null);
  }

  /**
   * Adds the given items to this table model, non-filtered items are added at top or bottom. If {@code sortAfterAdding}
   * is true and sorting is enabled this model is sorted after the items have been added
   * @param items the items to add
   * @param atTop if true then items are added at the top of the table model, else at the bottom
   * @param sortAfterAdding if true and sorting is enabled the model contents are sorted after adding
   * @see TableSortModel#isSortingEnabled()
   */
  protected final void addItems(final List<R> items, final boolean atTop, final boolean sortAfterAdding) {
    addItems(items, atTop ? 0 : visibleItems.size(), sortAfterAdding);
  }

  /**
   * Adds the given items to this table model, non-filtered items are added at the given index. If {@code sortAfterAdding}
   * is true and sorting is enabled this model is sorted after the items have been added
   * @param items the items to add
   * @param index the index at which to add the items
   * @param sortAfterAdding if true and sorting is enabled then the model is sorted after adding
   * @see TableSortModel#isSortingEnabled()
   */
  protected final void addItems(final List<R> items, final int index, final boolean sortAfterAdding) {
    int counter = 0;
    for (final R item : items) {
      if (includeCondition == null || includeCondition.test(item)) {
        visibleItems.add(index + counter++, item);
      }
      else {
        filteredItems.add(item);
      }
    }
    if (sortAfterAdding && sortModel.isSortingEnabled()) {
      sortModel.sort(visibleItems);
    }
    fireTableDataChanged();
  }

  /**
   * Returns the value to use when searching through the table.
   * @param rowIndex the row index
   * @param column the column
   * @return the search value
   * @see #findNext(int, String)
   * @see #findPrevious(int, String)
   */
  protected String getSearchValueAt(final int rowIndex, final TableColumn column) {
    final Object value = getValueAt(rowIndex, column.getModelIndex());

    return value == null ? "" : value.toString();
  }

  /**
   * @param searchText the search text
   * @return a Predicate based on the given search text
   */
  private Predicate<String> getSearchCondition(final String searchText) {
    if (regularExpressionSearch) {
      return new RegexSearchCondition(searchText);
    }

    return item -> !(item == null || searchText == null) && item.toLowerCase().contains(searchText.toLowerCase());
  }

  private void bindEventsInternal() {
    addTableModelListener(e -> tableDataChangedEvent.onEvent());
    for (final ColumnConditionModel conditionModel : columnModel.getColumnFilterModels()) {
      conditionModel.addConditionChangedListener(this::filterContents);
    }
    sortModel.addSortingChangedListener(this::sort);
    addTableModelListener(e -> {
      if (e.getType() == TableModelEvent.DELETE) {
        rowsDeletedEvent.onEvent(asList(e.getFirstRow(), e.getLastRow()));
      }
    });
  }

  private List getColumnValues(final Stream<Integer> rowIndexStream, final int columnModelIndex) {
    return rowIndexStream.map(rowIndex -> getValueAt(rowIndex, columnModelIndex)).collect(toList());
  }

  private RowColumn findColumnValue(final int row, final Predicate<String> condition) {
    final Enumeration<TableColumn> columnsToSearch = columnModel.getColumns();
    while (columnsToSearch.hasMoreElements()) {
      final TableColumn column = columnsToSearch.nextElement();
      if (condition.test(getSearchValueAt(row, column))) {
        return RowColumn.rowColumn(row, columnModel.getColumnIndex(column.getIdentifier()));
      }
    }

    return null;
  }

  private static final class RegexSearchCondition implements Predicate<String> {

    private final Pattern pattern;

    /**
     * Instantiates a new RegexSearchCondition.
     * @param patternString the regex pattern
     */
    private RegexSearchCondition(final String patternString) {
      this.pattern = Pattern.compile(patternString);
    }

    /**
     * Returns true if the regex pattern is valid and the given item passes the condition.
     * @param item the item
     * @return true if the item should be included
     */
    @Override
    public boolean test(final String item) {
      return item != null && pattern.matcher(item).find();
    }
  }

  private static final class DefaultIncludeCondition<R, C> implements Predicate<R> {

    private final Collection<? extends ColumnConditionModel<R, C>> columnFilters;

    private DefaultIncludeCondition(final Collection<? extends ColumnConditionModel<R, C>> columnFilters) {
      this.columnFilters = columnFilters;
    }

    @Override
    public boolean test(final R item) {
      return columnFilters.stream().allMatch(columnFilter -> columnFilter.include(item));
    }
  }

  /**
   * A default ColumnValueProvider implementation
   */
  protected static final class DefaultColumnValueProvider implements ColumnSummaryModel.ColumnValueProvider {

    private final Object columnIdentifier;
    private final FilteredTableModel tableModel;
    private final Format format;
    private final boolean numerical;

    /**
     * @param columnIdentifier the identifier of the column which values are provided
     * @param tableModel the table model
     * @param format the format to use for presenting the summary value
     */
    public DefaultColumnValueProvider(final Object columnIdentifier, final FilteredTableModel tableModel,
                                      final Format format) {
      this.columnIdentifier = columnIdentifier;
      this.tableModel = tableModel;
      this.format = format;
      final Class columnClass = tableModel.getSortModel().getColumnClass(columnIdentifier);
      this.numerical = Number.class.isAssignableFrom(columnClass);
    }

    @Override
    public String format(final Object value) {
      return format == null ? value.toString() : format.format(value);
    }

    @Override
    public void addValuesChangedListener(final EventListener listener) {
      tableModel.addTableDataChangedListener(listener);
      tableModel.getSelectionModel().addSelectionChangedListener(listener);
    }

    @Override
    public boolean isNumerical() {
      return numerical;
    }

    @Override
    public Collection getValues() {
      return isValueSubset() ? tableModel.getSelectedValues(columnIdentifier) : tableModel.getValues(columnIdentifier);
    }

    @Override
    public boolean isValueSubset() {
      return tableModel.getSelectionModel().isSelectionNotEmpty();
    }
  }
}
