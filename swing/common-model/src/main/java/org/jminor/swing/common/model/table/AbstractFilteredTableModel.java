/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.model.table;

import org.jminor.common.Event;
import org.jminor.common.EventListener;
import org.jminor.common.Events;
import org.jminor.common.model.FilterCondition;
import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.common.model.table.ColumnSummaryModel;
import org.jminor.common.model.table.DefaultColumnSummaryModel;
import org.jminor.common.model.table.SelectionModel;

import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import java.awt.Point;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

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
public abstract class AbstractFilteredTableModel<R, C> extends AbstractTableModel implements FilteredTableModel<R, C> {

  private final Event filteringDoneEvent = Events.event();
  private final Event sortingStartedEvent = Events.event();
  private final Event sortingDoneEvent = Events.event();
  private final Event refreshStartedEvent = Events.event();
  private final Event refreshDoneEvent = Events.event();
  private final Event tableDataChangedEvent = Events.event();
  private final Event tableModelClearedEvent = Events.event();

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
  private final SelectionModel<R> selectionModel;

  /**
   * The TableColumnModel
   */
  private final FilteredTableColumnModel<C> columnModel;

  /**
   * The sort model
   */
  private final TableSortModel<R, C> sortModel;

  /**
   * Maps PropertySummaryModels to their respective properties
   */
  private final Map<C, ColumnSummaryModel> columnSummaryModels = new HashMap<>();

  /**
   * the filter condition used by this model
   */
  private FilterCondition<R> filterCondition;

  /**
   * true if searching the table model should be done via regular expressions
   */
  private boolean regularExpressionSearch = false;

  /**
   * Instantiates a new table model.
   * @param sortModel the sort model to use
   * @param columnFilterModels the column filter models
   * @throws NullPointerException in case {@code columnModel} is null
   */
  public AbstractFilteredTableModel(final TableSortModel<R, C> sortModel, final Collection<? extends ColumnConditionModel<C>> columnFilterModels) {
    Objects.requireNonNull(sortModel, "sortModel");
    this.sortModel = sortModel;
    this.columnModel = new SwingFilteredTableColumnModel<>(sortModel.getColumns(), columnFilterModels);
    this.selectionModel = new SwingTableSelectionModel<>(this);
    this.filterCondition = new DefaultFilterCondition<>(this.columnModel.getColumnFilterModels());
    bindEventsInternal();
  }

  /** {@inheritDoc} */
  @Override
  public final List<R> getVisibleItems() {
    return Collections.unmodifiableList(visibleItems);
  }

  /** {@inheritDoc} */
  @Override
  public final List<R> getFilteredItems() {
    return Collections.unmodifiableList(filteredItems);
  }

  /** {@inheritDoc} */
  @Override
  public final int getVisibleItemCount() {
    return getRowCount();
  }

  /** {@inheritDoc} */
  @Override
  public final int getFilteredItemCount() {
    return filteredItems.size();
  }

  /** {@inheritDoc} */
  @Override
  public final int getColumnCount() {
    return columnModel.getColumnCount();
  }

  /** {@inheritDoc} */
  @Override
  public final int getRowCount() {
    return visibleItems.size();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean contains(final R item, final boolean includeFiltered) {
    final boolean ret = visibleItems.contains(item);
    if (!ret && includeFiltered) {
      return filteredItems.contains(item);
    }

    return ret;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isVisible(final R item) {
    return visibleItems.contains(item);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isFiltered(final R item) {
    return filteredItems.contains(item);
  }

  /** {@inheritDoc} */
  @Override
  public final Point findNextItemCoordinate(final int fromIndex, final boolean forward, final String searchText) {
    return findNextItemCoordinate(fromIndex, forward, getSearchCondition(searchText));
  }

  /** {@inheritDoc} */
  @Override
  public final Point findNextItemCoordinate(final int fromIndex, final boolean forward, final FilterCondition<Object> condition) {
    if (forward) {
      for (int row = fromIndex >= getVisibleItemCount() ? 0 : fromIndex; row < getVisibleItemCount(); row++) {
        final Point point = findColumnValue(columnModel.getColumns(), row, condition);
        if (point != null) {
          return point;
        }
      }
    }
    else {
      for (int row = fromIndex < 0 ? getVisibleItemCount() - 1 : fromIndex; row >= 0; row--) {
        final Point point = findColumnValue(columnModel.getColumns(), row, condition);
        if (point != null) {
          return point;
        }
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
      refreshStartedEvent.fire();
      final List<R> selectedItems = new ArrayList<>(selectionModel.getSelectedItems());
      doRefresh();
      selectionModel.setSelectedItems(selectedItems);
    }
    finally {
      refreshDoneEvent.fire();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void clear() {
    filteredItems.clear();
    final int size = visibleItems.size();
    if (size > 0) {
      visibleItems.clear();
      fireTableRowsDeleted(0, size - 1);
    }
    tableModelClearedEvent.fire();
  }

  /** {@inheritDoc} */
  @Override
  public final SelectionModel<R> getSelectionModel() {
    return selectionModel;
  }

  /** {@inheritDoc} */
  @Override
  public final TableSortModel<R, C> getSortModel() {
    return sortModel;
  }

  @Override
  public final ColumnSummaryModel getColumnSummaryModel(final C columnIdentifier) {
    if (!columnSummaryModels.containsKey(columnIdentifier)) {
      columnSummaryModels.put(columnIdentifier, new DefaultColumnSummaryModel(createColumnValueProvider(columnIdentifier)));
    }

    return columnSummaryModels.get(columnIdentifier);
  }

  /** {@inheritDoc} */
  @Override
  public final Collection getValues(final C columnIdentifier, final boolean selectedOnly) {
    final int columnModelIndex = columnModel.getTableColumn(columnIdentifier).getModelIndex();
    final ListSelectionModel listSelectionModel = (ListSelectionModel) getSelectionModel();
    final Collection values = new ArrayList();
    for (int row = 0; row < getVisibleItemCount(); row++) {
      if (!selectedOnly || listSelectionModel.isSelectedIndex(row)) {
        final Object value = getValueAt(row, columnModelIndex);
        if (value != null) {
          values.add(value);
        }
      }
    }

    return values;
  }

  /**
   * Creates a ColumnValueProvider for the given column
   * @param columnIdentifier the column identifier
   * @return a ColumnValueProvider for the column identified by {@code columnIdentifier}
   */
  protected ColumnSummaryModel.ColumnValueProvider createColumnValueProvider(final C columnIdentifier) {
    return new DefaultColumnValueProvider(columnIdentifier, this, null);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isRegularExpressionSearch() {
    return regularExpressionSearch;
  }

  /** {@inheritDoc} */
  @Override
  public final void setRegularExpressionSearch(final boolean value) {
    this.regularExpressionSearch = value;
  }

  /** {@inheritDoc} */
  @Override
  public final R getItemAt(final int index) {
    return visibleItems.get(index);
  }

  /** {@inheritDoc} */
  @Override
  public final int indexOf(final R item) {
    return visibleItems.indexOf(item);
  }

  /** {@inheritDoc} */
  @Override
  public final void sortContents() {
    try {
      sortingStartedEvent.fire();
      final List<R> selectedItems = new ArrayList<>(selectionModel.getSelectedItems());
      sortModel.sort(visibleItems);
      fireTableDataChanged();
      selectionModel.setSelectedItems(selectedItems);
    }
    finally {
      sortingDoneEvent.fire();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void filterContents() {
    try {
      final List<R> selectedItems = selectionModel.getSelectedItems();
      visibleItems.addAll(filteredItems);
      filteredItems.clear();
      if (filterCondition != null) {
        for (final ListIterator<R> iterator = visibleItems.listIterator(); iterator.hasNext();) {
          final R item = iterator.next();
          if (!filterCondition.include(item)) {
            filteredItems.add(item);
            iterator.remove();
          }
        }
      }
      sortModel.sort(visibleItems);
      fireTableDataChanged();
      selectionModel.setSelectedItems(selectedItems);
    }
    finally {
      filteringDoneEvent.fire();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final FilterCondition<R> getFilterCondition() {
    return filterCondition;
  }

  /** {@inheritDoc} */
  @Override
  public final void setFilterCondition(final FilterCondition<R> filterCondition) {
    this.filterCondition = filterCondition;
    filterContents();
  }

  /** {@inheritDoc} */
  @Override
  public final List<R> getAllItems() {
    final List<R> entities = new ArrayList<>(visibleItems);
    entities.addAll(filteredItems);

    return entities;
  }

  /** {@inheritDoc} */
  @Override
  public final void removeItem(final R item) {
    final int index = visibleItems.indexOf(item);
    if (index >= 0) {
      visibleItems.remove(index);
      fireTableRowsDeleted(index, index);
    }
    else {
      final int filteredIndex = filteredItems.indexOf(item);
      if (filteredIndex >= 0) {
        filteredItems.remove(item);
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void removeItems(final Collection<R> items) {
    boolean removed = false;
    for (final R item : items) {
      final int index = visibleItems.indexOf(item);
      if (index >= 0) {
        visibleItems.remove(index);
        removed = true;
      }
      else {
        final int filteredIndex = filteredItems.indexOf(item);
        if (filteredIndex >= 0) {
          filteredItems.remove(item);
        }
      }
    }
    if (removed) {
      fireTableDataChanged();
    }
  }

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
  @Override
  public final FilteredTableColumnModel<C> getColumnModel() {
    return columnModel;
  }

  /** {@inheritDoc} */
  @Override
  public final Class<?> getColumnClass(final int columnIndex) {
    return sortModel.getColumnClass((C) getColumnModel().getColumnIdentifier(columnIndex));
  }

  /** {@inheritDoc} */
  @Override
  public final void addRefreshStartedListener(final EventListener listener) {
    refreshStartedEvent.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeRefreshStartedListener(final EventListener listener) {
    refreshStartedEvent.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addRefreshDoneListener(final EventListener listener) {
    refreshDoneEvent.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeRefreshDoneListener(final EventListener listener) {
    refreshDoneEvent.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addFilteringListener(final EventListener listener) {
    filteringDoneEvent.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeFilteringListener(final EventListener listener) {
    filteringDoneEvent.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addSortingListener(final EventListener listener) {
    sortingDoneEvent.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeSortingListener(final EventListener listener) {
    sortingDoneEvent.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addTableDataChangedListener(final EventListener listener) {
    tableDataChangedEvent.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeTableDataChangedListener(final EventListener listener) {
    tableDataChangedEvent.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addTableModelClearedListener(final EventListener listener) {
    tableModelClearedEvent.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeTableModelClearedListener(final EventListener listener) {
    tableModelClearedEvent.removeListener(listener);
  }

  /**
   * Refreshes the data in this table model.
   * @see #clear()
   * @see #addItems(java.util.List, boolean)
   */
  protected abstract void doRefresh();

  /**
   * Adds the given items to this table model, filtering on the fly and sorting if sorting is enabled and
   * the items are not being added at the front.
   * @param items the items to add
   * @param atFront if true then the items are added at the front (topmost) in the order they are received, otherwise they are added last
   */
  protected final void addItems(final List<R> items, final boolean atFront) {
    int index = 0;
    for (final R item : items) {
      if (filterCondition.include(item)) {
        if (atFront) {
          visibleItems.add(index++, item);
        }
        else {
          visibleItems.add(item);
        }
      }
      else {
        filteredItems.add(item);
      }
    }
    if (!atFront && sortModel.isSortingEnabled()) {
      sortModel.sort(visibleItems);
    }
    fireTableDataChanged();
  }

  /**
   * Adds the given items to this table model, non-filtered items are added at the given index. If sorting
   * is enabled this model is sorted after the items have been added
   * @param items the items to add
   * @param index the index at which to add the items
   */
  protected final void addItems(final List<R> items, final int index) {
    int counter = 0;
    for (final R item : items) {
      if (filterCondition.include(item)) {
        visibleItems.add(index + counter++, item);
      }
      else {
        filteredItems.add(item);
      }
    }
    if (sortModel.isSortingEnabled()) {
      sortModel.sort(visibleItems);
    }
    fireTableDataChanged();
  }

  /**
   * Returns the value to use when searching through the table.
   * @param rowIndex the row index
   * @param column the column
   * @return the search value
   * @see #findNextItemCoordinate(int, boolean, String)
   */
  protected String getSearchValueAt(final int rowIndex, final TableColumn column) {
    final Object value = getValueAt(rowIndex, column.getModelIndex());

    return value == null ? "" : value.toString();
  }

  /**
   * @param searchText the search text
   * @return a FilterCondition based on the given search text
   */
  protected final FilterCondition<Object> getSearchCondition(final String searchText) {
    if (regularExpressionSearch) {
      return new RegexFilterCondition<>(searchText);
    }

    return item -> !(item == null || searchText == null) && item.toString().toLowerCase().contains(searchText.toLowerCase());
  }

  private void bindEventsInternal() {
    addTableModelListener(e -> tableDataChangedEvent.fire());
    for (final ColumnConditionModel conditionModel : columnModel.getColumnFilterModels()) {
      conditionModel.addConditionStateListener(this::filterContents);
    }
    sortModel.addSortingStateChangedListener(this::sortContents);
  }

  private Point findColumnValue(final Enumeration<TableColumn> visibleColumns, final int row, final FilterCondition<Object> condition) {
    int index = 0;
    while (visibleColumns.hasMoreElements()) {
      if (condition.include(getSearchValueAt(row, visibleColumns.nextElement()))) {
        return new Point(index, row);
      }
      index++;
    }

    return null;
  }

  private static final class RegexFilterCondition<T> implements FilterCondition<T> {

    private final Pattern pattern;

    /**
     * Instantiates a new RegexFilterCondition.
     * @param patternString the regex pattern
     */
    private RegexFilterCondition(final String patternString) {
      this.pattern = Pattern.compile(patternString);
    }

    /**
     * Returns true if the regex pattern is valid and the given item passes the condition.
     * @param item the item
     * @return true if the item should be included
     */
    @Override
    public boolean include(final T item) {
      return item != null && pattern.matcher(item.toString()).find();
    }
  }

  private static final class DefaultFilterCondition<R, C> implements FilterCondition<R> {

    private final Collection<? extends ColumnConditionModel<C>> columnFilters;

    private DefaultFilterCondition(final Collection<? extends ColumnConditionModel<C>> columnFilters) {
      this.columnFilters = columnFilters;
    }

    @Override
    public boolean include(final R item) {
      for (final ColumnConditionModel columnFilter : columnFilters) {
        if (!columnFilter.include(item)) {
          return false;
        }
      }

      return true;
    }
  }

  /**
   * A default ColumnValueProvider implementation
   */
  protected static final class DefaultColumnValueProvider implements ColumnSummaryModel.ColumnValueProvider {

    private final Object columnIdentifier;
    private final FilteredTableModel tableModel;
    private final Format format;

    private boolean useValueSubset = true;

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
      return isInteger() || isDouble();
    }

    @Override
    public Collection getValues() {
      return tableModel.getValues(columnIdentifier, useValueSubset && isValueSubset());
    }

    @Override
    public boolean isInteger() {
      return tableModel.getSortModel().getColumnClass(columnIdentifier).equals(Integer.class);
    }

    @Override
    public boolean isDouble() {
      return tableModel.getSortModel().getColumnClass(columnIdentifier).equals(Double.class);
    }

    @Override
    public boolean isValueSubset() {
      return !tableModel.getSelectionModel().isSelectionEmpty();
    }

    @Override
    public boolean isUseValueSubset() {
      return useValueSubset;
    }

    @Override
    public void setUseValueSubset(final boolean useValueSubset) {
      this.useValueSubset = useValueSubset;
    }
  }
}
