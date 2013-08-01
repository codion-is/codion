/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.table;

import org.jminor.common.model.Event;
import org.jminor.common.model.EventAdapter;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.Events;
import org.jminor.common.model.FilterCriteria;
import org.jminor.common.model.Util;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.ListIterator;
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
  private final List<R> visibleItems = new ArrayList<R>();

  /**
   * Holds items that are filtered
   */
  private final List<R> filteredItems = new ArrayList<R>();

  /**
   * The selection model
   */
  private final TableSelectionModel<R> selectionModel;

  /**
   * The TableColumnModel
   */
  private final FilteredTableColumnModel<C> columnModel;

  /**
   * The sort model
   */
  private final TableSortModel<R, C> sortModel;

  /**
   * the filter criteria used by this model
   */
  private FilterCriteria<R> filterCriteria;

  /**
   * true if searching the table model should be done via regular expressions
   */
  private boolean regularExpressionSearch = false;

  /**
   * Instantiates a new table model.
   * @param sortModel the sort model to use
   * @param columnFilterModels the column filter models
   * @throws IllegalArgumentException in case <code>columnModel</code> is null
   */
  public AbstractFilteredTableModel(final TableSortModel<R, C> sortModel, final Collection<? extends ColumnSearchModel<C>> columnFilterModels) {
    Util.rejectNullValue(sortModel, "sortModel");
    this.sortModel = sortModel;
    this.columnModel = new DefaultFilteredTableColumnModel<C>(sortModel.getColumns(), columnFilterModels);
    this.selectionModel = new DefaultTableSelectionModel<R>(this);
    this.filterCriteria = new FilterCriteriaImpl<R, C>(this.columnModel.getColumnFilterModels());
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
    return findNextItemCoordinate(fromIndex, forward, getSearchCriteria(searchText));
  }

  /** {@inheritDoc} */
  @Override
  public final Point findNextItemCoordinate(final int fromIndex, final boolean forward, final FilterCriteria<Object> criteria) {
    if (forward) {
      for (int row = fromIndex >= getVisibleItemCount() ? 0 : fromIndex; row < getVisibleItemCount(); row++) {
        final Point point = findColumnValue(columnModel.getColumns(), row, criteria);
        if (point != null) {
          return point;
        }
      }
    }
    else {
      for (int row = fromIndex < 0 ? getVisibleItemCount() - 1 : fromIndex; row >= 0; row--) {
        final Point point = findColumnValue(columnModel.getColumns(), row, criteria);
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
      final List<R> selectedItems = new ArrayList<R>(selectionModel.getSelectedItems());
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
  public final TableSelectionModel<R> getSelectionModel() {
    return selectionModel;
  }

  /** {@inheritDoc} */
  @Override
  public final TableSortModel<R, C> getSortModel() {
    return sortModel;
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
  public final void filterContents() {
    if (filterCriteria == null) {
      return;
    }
    try {
      final List<R> selectedItems = selectionModel.getSelectedItems();
      visibleItems.addAll(filteredItems);
      filteredItems.clear();
      for (final ListIterator<R> iterator = visibleItems.listIterator(); iterator.hasNext();) {
        final R item = iterator.next();
        if (!filterCriteria.include(item)) {
          filteredItems.add(item);
          iterator.remove();
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
  public final FilterCriteria<R> getFilterCriteria() {
    return filterCriteria;
  }

  /** {@inheritDoc} */
  @Override
  public final void setFilterCriteria(final FilterCriteria<R> filterCriteria) {
    this.filterCriteria = filterCriteria;
    filterContents();
  }

  /** {@inheritDoc} */
  @Override
  public final List<R> getAllItems() {
    final List<R> entities = new ArrayList<R>(visibleItems);
    entities.addAll(filteredItems);

    return entities;
  }

  /** {@inheritDoc} */
  @Override
  public final void removeItem(final R item) {
    if (visibleItems.contains(item)) {
      final int index = indexOf(item);
      visibleItems.remove(item);
      fireTableRowsDeleted(index, index);
    }
    else {
      if (filteredItems.contains(item)) {
        filteredItems.remove(item);
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void removeItems(final Collection<R> items) {
    for (final R rowItem : items) {
      removeItem(rowItem);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void removeItems(final int fromIndex, final int toIndex) {
    visibleItems.subList(fromIndex, toIndex).clear();
    fireTableRowsDeleted(fromIndex, toIndex);
  }

  /**
   * A default implementation returning false
   * @return false
   */
  @Override
  public boolean vetoSelectionChange() {
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public final FilteredTableColumnModel<C> getColumnModel() {
    return columnModel;
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
      if (filterCriteria.include(item)) {
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
      if (filterCriteria.include(item)) {
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
   * @return a FilterCriteria based on the given search text
   */
  protected final FilterCriteria<Object> getSearchCriteria(final String searchText) {
    if (regularExpressionSearch) {
      return new RegexFilterCriteria<Object>(searchText);
    }

    return new FilterCriteria<Object>() {
      /** {@inheritDoc} */
      @Override
      public boolean include(final Object item) {
        return !(item == null || searchText == null) && item.toString().toLowerCase().contains(searchText.toLowerCase());
      }
    };
  }

  private void bindEventsInternal() {
    addTableModelListener(new TableModelListener() {
      /** {@inheritDoc} */
      @Override
      public void tableChanged(final TableModelEvent e) {
        tableDataChangedEvent.fire();
      }
    });
    for (final ColumnSearchModel searchModel : columnModel.getColumnFilterModels()) {
      searchModel.addSearchStateListener(new EventAdapter() {
        /** {@inheritDoc} */
        @Override
        public void eventOccurred() {
          filterContents();
        }
      });
    }
    sortModel.addSortingStateChangedListener(new EventAdapter() {
      @Override
      public void eventOccurred() {
        sortVisibleItems();
      }
    });
  }

  private Point findColumnValue(final Enumeration<TableColumn> visibleColumns, final int row, final FilterCriteria<Object> criteria) {
    int index = 0;
    while (visibleColumns.hasMoreElements()) {
      if (criteria.include(getSearchValueAt(row, visibleColumns.nextElement()))) {
        return new Point(index, row);
      }
      index++;
    }

    return null;
  }

  private void sortVisibleItems() {
    try {
      sortingStartedEvent.fire();
      final List<R> selectedItems = new ArrayList<R>(selectionModel.getSelectedItems());
      sortModel.sort(visibleItems);
      fireTableDataChanged();
      selectionModel.setSelectedItems(selectedItems);
    }
    finally {
      sortingDoneEvent.fire();
    }
  }

  private static final class RegexFilterCriteria<T> implements FilterCriteria<T> {

    private final Pattern pattern;

    /**
     * Instantiates a new RegexFilterCriteria.
     * @param patternString the regex pattern
     */
    private RegexFilterCriteria(final String patternString) {
      this.pattern = Pattern.compile(patternString);
    }

    /**
     * Returns true if the regex pattern is valid and the given item passes the criteria.
     * @param item the item
     * @return true if the item should be included
     */
    @Override
    public boolean include(final T item) {
      return item != null && pattern.matcher(item.toString()).find();
    }
  }

  private static final class FilterCriteriaImpl<R, C> implements FilterCriteria<R> {

    private final Collection<? extends ColumnSearchModel<C>> columnFilters;

    private FilterCriteriaImpl(final Collection<? extends ColumnSearchModel<C>> columnFilters) {
      this.columnFilters = columnFilters;
    }

    /** {@inheritDoc} */
    @Override
    public boolean include(final R item) {
      for (final ColumnSearchModel columnFilter : columnFilters) {
        if (!columnFilter.include(item)) {
          return false;
        }
      }

      return true;
    }
  }
}
