/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.table;

import org.jminor.common.model.Event;
import org.jminor.common.model.EventAdapter;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.Events;
import org.jminor.common.model.FilterCriteria;
import org.jminor.common.model.State;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.States;
import org.jminor.common.model.Util;

import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
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

  private static final Comparator<Comparable<Object>> COMPARABLE_COMPARATOR = new Comparator<Comparable<Object>>() {
    /** {@inheritDoc} */
    @Override
    public int compare(final Comparable<Object> o1, final Comparable<Object> o2) {
      return (o1.compareTo(o2));
    }
  };
  private static final Comparator LEXICAL_COMPARATOR = Util.getSpaceAwareCollator();

  private final Map<C, Comparator> columnComparators = new HashMap<C, Comparator>();

  private final Comparator<R> rowComparator = new Comparator<R>() {
    @Override
    public int compare(final R o1, final R o2) {
      for (final Map.Entry<C, SortingState> state : getOrderedSortingStates()) {
        final int comparison = compareRows(o1, o2, state.getKey(), state.getValue().getDirective());
        if (comparison != 0) {
          return comparison;
        }
      }

      return 0;
    }
  };

  private static final SortingState EMPTY_SORTING_STATE = new SortingStateImpl(SortingDirective.UNSORTED, -1);

  private final Event evtFilteringDone = Events.event();
  private final Event evtSortingStarted = Events.event();
  private final Event evtSortingDone = Events.event();
  private final Event evtRefreshStarted = Events.event();
  private final Event evtRefreshDone = Events.event();
  private final Event evtTableDataChanged = Events.event();
  private final Event evtTableModelCleared = Events.event();

  /**
   * Holds visible items
   */
  private final List<R> visibleItems = new ArrayList<R>();

  /**
   * Holds items that are filtered
   */
  private final List<R> filteredItems = new ArrayList<R>();

  /**
   * The TableColumnModel
   */
  private final FilteredTableColumnModel<C> columnModel;

  /**
   * holds the column sorting states
   */
  private final Map<C, SortingState> sortingStates = new HashMap<C, SortingState>();

  /**
   * The selection model
   */
  private final SelectionModel selectionModel;

  /**
   * true while the model data is being sorted
   */
  private boolean sorting = false;

  /**
   * the filter criteria used by this model
   */
  private final FilterCriteria<R> filterCriteria = new FilterCriteria<R>() {
    /** {@inheritDoc} */
    @Override
    public boolean include(final R item) {
      for (final ColumnSearchModel columnFilter : columnModel.getColumnFilterModels()) {
        if (columnFilter.isEnabled() && !columnFilter.include(item)) {
          return false;
        }
      }

      return true;
    }
  };

  /**
   * true if searching the table model should be done via regular expressions
   */
  private boolean regularExpressionSearch = false;

  /**
   * Instantiates a new table model.
   * @param columns the columns to base this model on
   * @param columnFilterModels the column filter models
   * @throws IllegalArgumentException in case <code>columnModel</code> is null
   */
  public AbstractFilteredTableModel(final List<TableColumn> columns, final Collection<? extends ColumnSearchModel<C>> columnFilterModels) {
    this.columnModel = new DefaultFilteredTableColumnModel<C>(columns, columnFilterModels);
    this.selectionModel = new SelectionModel(this);
    resetSortingStates();
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
    return visibleItems.size();
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
    return getVisibleItemCount();
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
      evtRefreshStarted.fire();
      final List<R> selectedItems = new ArrayList<R>(getSelectedItems());
      doRefresh();
      setSelectedItems(selectedItems);
    }
    finally {
      evtRefreshDone.fire();
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
    evtTableModelCleared.fire();
  }

  /** {@inheritDoc} */
  @Override
  public final ListSelectionModel getSelectionModel() {
    return selectionModel;
  }

  /** {@inheritDoc} */
  @Override
  public final int getSortingPriority(final C columnIdentifier) {
    return getSortingState(columnIdentifier).getPriority();
  }

  /** {@inheritDoc} */
  @Override
  public final SortingDirective getSortingDirective(final C columnIdentifier) {
    return getSortingState(columnIdentifier).getDirective();
  }

  /** {@inheritDoc} */
  @Override
  public final void setSortingDirective(final C columnIdentifier, final SortingDirective directive,
                                        final boolean addColumnToSort) {
    if (!addColumnToSort) {
      resetSortingStates();
    }
    if (directive == SortingDirective.UNSORTED) {
      sortingStates.put(columnIdentifier, EMPTY_SORTING_STATE);
    }
    else {
      final SortingState state = getSortingState(columnIdentifier);
      if (state.equals(EMPTY_SORTING_STATE)) {
        final int priority = getNextSortPriority();
        sortingStates.put(columnIdentifier, new SortingStateImpl(directive, priority));
      }
      else {
        sortingStates.put(columnIdentifier, new SortingStateImpl(directive, state.getPriority()));
      }
    }
    sortVisibleItems();
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
  public final void selectAll() {
    selectionModel.setSelectionInterval(0, getVisibleItemCount() - 1);
  }

  /** {@inheritDoc} */
  @Override
  public final void clearSelection() {
    selectionModel.clearSelection();
  }

  /** {@inheritDoc} */
  @Override
  public final List<Integer> getSelectedIndexes() {
    return selectionModel.getSelectedIndexes();
  }

  /** {@inheritDoc} */
  @Override
  public final void moveSelectionUp() {
    if (!visibleItems.isEmpty()) {
      if (selectionModel.isSelectionEmpty()) {
        selectionModel.setSelectionInterval(visibleItems.size() - 1, visibleItems.size() - 1);
      }
      else {
        final Collection<Integer> selected = selectionModel.getSelectedIndexes();
        final List<Integer> newSelected = new ArrayList<Integer>(selected.size());
        for (final Integer index : selected) {
          newSelected.add(index == 0 ? visibleItems.size() - 1 : index - 1);
        }
        setSelectedIndexes(newSelected);
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void moveSelectionDown() {
    if (!visibleItems.isEmpty()) {
      if (selectionModel.isSelectionEmpty()) {
        selectionModel.setSelectionInterval(0, 0);
      }
      else {
        final Collection<Integer> selected = selectionModel.getSelectedIndexes();
        final List<Integer> newSelected = new ArrayList<Integer>(selected.size());
        for (final Integer index : selected) {
          newSelected.add(index == visibleItems.size() - 1 ? 0 : index + 1);
        }
        setSelectedIndexes(newSelected);
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public final R getSelectedItem() {
    final int index = selectionModel.getSelectedIndex();
    if (index >= 0 && index < getVisibleItemCount()) {
      return getItemAt(index);
    }
    else {
      return null;
    }
  }

  /** {@inheritDoc} */
  @Override
  public final List<R> getSelectedItems() {
    final Collection<Integer> selectedModelIndexes = selectionModel.getSelectedIndexes();
    final List<R> selectedItems = new ArrayList<R>();
    for (final int modelIndex : selectedModelIndexes) {
      selectedItems.add(getItemAt(modelIndex));
    }

    return selectedItems;
  }

  /** {@inheritDoc} */
  @Override
  public final void setSelectedIndex(final int index) {
    if (index < 0 || index > getVisibleItemCount() - 1) {
      throw new IndexOutOfBoundsException("Index: " + index + ", size: " + getVisibleItemCount());
    }
    selectionModel.setSelectionInterval(index, index);
  }

  /** {@inheritDoc} */
  @Override
  public final void setSelectedIndexes(final Collection<Integer> indexes) {
    for (final Integer index : indexes) {
      if (index < 0 || index > getVisibleItemCount() - 1) {
        throw new IndexOutOfBoundsException("Index: " + index + ", size: " + getVisibleItemCount());
      }
    }
    selectionModel.setSelectedIndexes(indexes);
  }

  /** {@inheritDoc} */
  @Override
  public final void setSelectedItem(final R item) {
    setSelectedItems(Arrays.asList(item));
  }

  /** {@inheritDoc} */
  @Override
  public final void setSelectedItems(final Collection<R> items) {
    if (!selectionModel.isSelectionEmpty()) {
      selectionModel.clearSelection();
    }
    addSelectedItems(items);
  }

  /** {@inheritDoc} */
  @Override
  public void addSelectedItem(final R item) {
    addSelectedItems(Arrays.asList(item));
  }

  /** {@inheritDoc} */
  @Override
  public void addSelectedItems(final Collection<R> items) {
    final List<Integer> indexes = new ArrayList<Integer>();
    for (final R item : items) {
      final int index = indexOf(item);
      if (index >= 0) {
        indexes.add(index);
      }
    }
    selectionModel.addSelectedIndexes(indexes);
  }

  /** {@inheritDoc} */
  @Override
  public final void addSelectedIndex(final int index) {
    if (index < 0 || index > getVisibleItemCount() - 1) {
      throw new IndexOutOfBoundsException("Index: " + index + ", size: " + getVisibleItemCount());
    }
    selectionModel.addSelectionInterval(index, index);
  }

  /** {@inheritDoc} */
  @Override
  public final void addSelectedIndexes(final Collection<Integer> indexes) {
    selectionModel.addSelectedIndexes(indexes);
  }

  /** {@inheritDoc} */
  @Override
  public final int getSelectedIndex() {
    return selectionModel.getSelectedIndex();
  }

  /** {@inheritDoc} */
  @Override
  public final int getSelectionCount() {
    return selectionModel.getSelectionCount();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isSelectionEmpty() {
    return selectionModel.isSelectionEmpty();
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
    try {
      final List<R> selectedItems = getSelectedItems();
      visibleItems.addAll(filteredItems);
      filteredItems.clear();
      for (final ListIterator<R> iterator = visibleItems.listIterator(); iterator.hasNext();) {
        final R item = iterator.next();
        if (!filterCriteria.include(item)) {
          filteredItems.add(item);
          iterator.remove();
        }
      }
      Collections.sort(visibleItems, rowComparator);
      fireTableDataChanged();
      setSelectedItems(selectedItems);
    }
    finally {
      evtFilteringDone.fire();
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
    throw new UnsupportedOperationException("AbstractFilteredTableModel.setFilterCriteria(FilterCriteria)");
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
    final int visibleCount = visibleItems.size();
    visibleItems.removeAll(items);
    filteredItems.removeAll(items);
    if (visibleItems.size() != visibleCount) {
      fireTableDataChanged();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void removeItems(final int fromIndex, final int toIndex) {
    visibleItems.subList(fromIndex, toIndex).clear();
    fireTableRowsDeleted(fromIndex, toIndex);
  }

  /** {@inheritDoc} */
  @Override
  public final FilteredTableColumnModel<C> getColumnModel() {
    return columnModel;
  }

  /** {@inheritDoc} */
  @Override
  public final StateObserver getSelectionEmptyObserver() {
    return selectionModel.getSelectionEmptyObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final StateObserver getMultipleSelectionObserver() {
    return selectionModel.getMultipleSelectionObserver();
  }

  /** {@inheritDoc} */
  @Override
  public StateObserver getSingleSelectionObserver() {
    return selectionModel.getSingleSelectionObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final void addRefreshStartedListener(final EventListener listener) {
    evtRefreshStarted.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeRefreshStartedListener(final EventListener listener) {
    evtRefreshStarted.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addRefreshDoneListener(final EventListener listener) {
    evtRefreshDone.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeRefreshDoneListener(final EventListener listener) {
    evtRefreshDone.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addFilteringListener(final EventListener listener) {
    evtFilteringDone.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeFilteringListener(final EventListener listener) {
    evtFilteringDone.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addSortingListener(final EventListener listener) {
    evtSortingDone.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeSortingListener(final EventListener listener) {
    evtSortingDone.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addSelectedIndexListener(final EventListener listener) {
    selectionModel.addSelectedIndexListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeSelectedIndexListener(final EventListener listener) {
    selectionModel.removeSelectedIndexListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addSelectionChangedListener(final EventListener listener) {
    selectionModel.addSelectionChangedListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeSelectionChangedListener(final EventListener listener) {
    selectionModel.removeSelectionChangedListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addTableDataChangedListener(final EventListener listener) {
    evtTableDataChanged.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeTableDataChangedListener(final EventListener listener) {
    evtTableDataChanged.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addTableModelClearedListener(final EventListener listener) {
    evtTableModelCleared.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeTableModelClearedListener(final EventListener listener) {
    evtTableModelCleared.removeListener(listener);
  }

  /**
   * Refreshes the data in this table model.
   * @see #clear()
   * @see #addItems(java.util.List, boolean)
   */
  protected abstract void doRefresh();

  /**
   * Returns a Comparable instance for the given rowObject and columnIdentifier,
   * used when sorting this table model
   * @param rowObject the object representing a given row
   * @param columnIdentifier the column identifier
   * @return a Comparable for the given row and column
   */
  protected abstract Comparable getComparable(final R rowObject, final C columnIdentifier);

  /**
   * Adds the given items to this table model, filtering on the fly and sorting if sorting is enabled and
   * the items are not being added at the front.
   * @param items the items to add
   * @param atFront if true then the items are added at the front (topmost), otherwise they are added last
   */
  protected final void addItems(final List<R> items, final boolean atFront) {
    for (final R item : items) {
      if (filterCriteria.include(item)) {
        if (atFront) {
          visibleItems.add(0, item);
        }
        else {
          visibleItems.add(item);
        }
      }
      else {
        filteredItems.add(item);
      }
    }
    if (!atFront && isSortingStateEnabled()) {
      Collections.sort(visibleItems, rowComparator);
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
    if (isSortingStateEnabled()) {
      Collections.sort(visibleItems, rowComparator);
    }
    fireTableDataChanged();
  }

  /**
   * Returns the value to use when searching through the table.
   * @param rowIndex the row index
   * @param columnIdentifier the column identifier
   * @return the search value
   */
  protected String getSearchValueAt(final int rowIndex, final C columnIdentifier) {
    final Object value = getValueAt(rowIndex, columnModel.getTableColumn(columnIdentifier).getModelIndex());

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

  /**
   * @param columnIdentifier the column identifier
   * @return the column Class
   */
  protected Class getColumnClass(final C columnIdentifier) {
    return Object.class;
  }

  /**
   * Initializes a comparator used when sorting by the give column,
   * the comparator receives the column values, but never null.
   * @param columnIdentifier the column identifier
   * @return the comparator to use when sorting by the given column
   */
  protected Comparator initializeColumnComparator(final C columnIdentifier) {
    final Class columnClass = getColumnClass(columnIdentifier);
    if (columnClass.equals(String.class)) {
      return LEXICAL_COMPARATOR;
    }
    if (Comparable.class.isAssignableFrom(columnClass)) {
      return COMPARABLE_COMPARATOR;
    }

    return LEXICAL_COMPARATOR;
  }

  /**
   * @return true while this table model is being sorted
   */
  protected final boolean isSorting() {
    return sorting;
  }

  private void bindEventsInternal() {
    addTableModelListener(new TableModelListener() {
      /** {@inheritDoc} */
      @Override
      public void tableChanged(final TableModelEvent e) {
        evtTableDataChanged.fire();
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
  }

  @SuppressWarnings({"unchecked"})
  private Point findColumnValue(final Enumeration<TableColumn> visibleColumns, final int row, final FilterCriteria<Object> criteria) {
    int index = 0;
    while (visibleColumns.hasMoreElements()) {
      final TableColumn column = visibleColumns.nextElement();
      if (criteria.include(getSearchValueAt(row, (C) column.getIdentifier()))) {
        return new Point(index, row);
      }
      index++;
    }

    return null;
  }

  private SortingState getSortingState(final C columnIdentifier) {
    final SortingState state = sortingStates.get(columnIdentifier);
    if (state == null) {
      throw new IllegalArgumentException("No sorting state assigned to column identified by : " + columnIdentifier);
    }

    return state;
  }

  @SuppressWarnings({"unchecked"})
  private int getNextSortPriority() {
    int maxPriority = -1;
    for (final SortingState state : sortingStates.values()) {
      maxPriority = Math.max(state.getPriority(), maxPriority);
    }

    return maxPriority + 1;
  }

  private void sortVisibleItems() {
    try {
      sorting = true;
      evtSortingStarted.fire();
      final List<R> selectedItems = new ArrayList<R>(getSelectedItems());
      Collections.sort(visibleItems, rowComparator);
      fireTableDataChanged();
      setSelectedItems(selectedItems);
    }
    finally {
      sorting = false;
      evtSortingDone.fire();
    }
  }

  private List<Map.Entry<C, SortingState>> getOrderedSortingStates() {
    final ArrayList<Map.Entry<C, SortingState>> entries = new ArrayList<Map.Entry<C, SortingState>>();
    for (final Map.Entry<C, SortingState> entry : sortingStates.entrySet()) {
      if (!EMPTY_SORTING_STATE.equals(entry.getValue())) {
        entries.add(entry);
      }
    }
    Collections.sort(entries, new Comparator<Map.Entry<C, SortingState>>() {
      /** {@inheritDoc} */
      @Override
      public int compare(final Map.Entry<C, SortingState> o1, final Map.Entry<C, SortingState> o2) {
        final Integer priorityOne = o1.getValue().getPriority();
        final Integer priorityTwo = o2.getValue().getPriority();

        return priorityOne.compareTo(priorityTwo);
      }
    });

    return entries;
  }

  private int compareRows(final R rowOne, final R rowTwo, final C columnIdentifier, final SortingDirective directive) {
    final Comparable valueOne = getComparable(rowOne, columnIdentifier);
    final Comparable valueTwo = getComparable(rowTwo, columnIdentifier);
    final int comparison;
    // Define null less than everything, except null.
    if (valueOne == null && valueTwo == null) {
      comparison = 0;
    }
    else if (valueOne == null) {
      comparison = -1;
    }
    else if (valueTwo == null) {
      comparison = 1;
    }
    else {
      Comparator comparator = columnComparators.get(columnIdentifier);
      if (comparator == null) {
        comparator = initializeColumnComparator(columnIdentifier);
        columnComparators.put(columnIdentifier, comparator);
      }
      //noinspection unchecked
      comparison = comparator.compare(valueOne, valueTwo);
    }
    if (comparison != 0) {
      return directive == SortingDirective.DESCENDING ? -comparison : comparison;
    }

    return 0;
  }

  private boolean isSortingStateEnabled() {
    for (final SortingState state : sortingStates.values()) {
      if (!state.equals(EMPTY_SORTING_STATE)) {
        return true;
      }
    }

    return false;
  }

  @SuppressWarnings({"unchecked"})
  private void resetSortingStates() {
    final Enumeration<TableColumn> columns = columnModel.getColumns();
    while (columns.hasMoreElements()) {
      sortingStates.put((C) columns.nextElement().getIdentifier(), EMPTY_SORTING_STATE);
    }
  }

  private static final class SortingStateImpl implements SortingState {

    private final SortingDirective direction;
    private final int priority;

    private SortingStateImpl(final SortingDirective direction, final int priority) {
      Util.rejectNullValue(direction, "direction");
      this.direction = direction;
      this.priority = priority;
    }

    /** {@inheritDoc} */
    @Override
    public int getPriority() {
      return priority;
    }

    /** {@inheritDoc} */
    @Override
    public SortingDirective getDirective() {
      return direction;
    }
  }

  private static final class SelectionModel extends DefaultListSelectionModel {

    private final Event evtSelectionChanged = Events.event();
    private final Event evtSelectedIndexChanged = Events.event();
    private final State stSelectionEmpty = States.state(true);
    private final State stMultipleSelection = States.state(false);
    private final State stSingleSelection = States.state(false);

    private final AbstractFilteredTableModel tableModel;

    /**
     * true while the selection is being updated
     */
    private boolean isUpdatingSelection = false;
    /**
     * Holds the topmost (minimum) selected index
     */
    private int selectedIndex = -1;

    private SelectionModel(final AbstractFilteredTableModel tableModel) {
      this.tableModel = tableModel;
    }

    /** {@inheritDoc} */
    @Override
    public void fireValueChanged(final int firstIndex, final int lastIndex, final boolean isAdjusting) {
      super.fireValueChanged(firstIndex, lastIndex, isAdjusting);
      stSelectionEmpty.setActive(isSelectionEmpty());
      stSingleSelection.setActive(getSelectionCount() == 1);
      stMultipleSelection.setActive(!stSelectionEmpty.isActive() && !stSingleSelection.isActive());
      final int minSelIndex = getMinSelectionIndex();
      if (selectedIndex != minSelIndex) {
        selectedIndex = minSelIndex;
        evtSelectedIndexChanged.fire();
      }
      if (!(isAdjusting || isUpdatingSelection || tableModel.isSorting())) {
        evtSelectionChanged.fire();
      }
    }

    /**
     * @return the number of selected rows
     */
    private int getSelectionCount() {
      if (isSelectionEmpty()) {
        return 0;
      }

      int counter = 0;
      final int min = getMinSelectionIndex();
      final int max = getMaxSelectionIndex();
      if (min >= 0 && max >= 0) {
        for (int i = min; i <= max; i++) {
          if (isSelectedIndex(i)) {
            counter++;
          }
        }
      }

      return counter;
    }

    /**
     * Adds these indexes to the selection
     * @param indexes the indexes to add to the selection
     */
    private void addSelectedIndexes(final Collection<Integer> indexes) {
      if (indexes.isEmpty()) {
        return;
      }
      final Iterator<Integer> iterator = indexes.iterator();
      //keep the first index and add last in order to avoid firing evtSelectionChanged
      //for each index being added, see fireValueChanged() above
      final int firstIndex = iterator.next();
      try {
        isUpdatingSelection = true;
        while (iterator.hasNext()) {
          final int index = iterator.next();
          addSelectionInterval(index, index);
        }
      }
      finally {
        isUpdatingSelection = false;
        addSelectionInterval(firstIndex, firstIndex);
      }
    }

    /**
     * @param indexes the indexes to select
     */
    private void setSelectedIndexes(final Collection<Integer> indexes) {
      clearSelection();
      addSelectedIndexes(indexes);
    }

    /**
     * @return the selected indexes
     */
    private List<Integer> getSelectedIndexes() {
      final List<Integer> indexes = new ArrayList<Integer>();
      final int min = getMinSelectionIndex();
      final int max = getMaxSelectionIndex();
      for (int i = min; i <= max; i++) {
        if (isSelectedIndex(i)) {
          indexes.add(i);
        }
      }

      return indexes;
    }

    /**
     * @return the topmost (lowest row index) selected index
     */
    private int getSelectedIndex() {
      return selectedIndex;
    }

    /**
     * @param listener a listener notified each time the topmost (lowest row index) selected index changes
     */
    private void addSelectedIndexListener(final EventListener listener) {
      evtSelectedIndexChanged.addListener(listener);
    }

    /**
     * @param listener the listener to remove
     */
    private void removeSelectedIndexListener(final EventListener listener) {
      evtSelectedIndexChanged.addListener(listener);
    }

    /**
     * @param listener a listener notified each time the selection changes
     */
    private void addSelectionChangedListener(final EventListener listener) {
      evtSelectionChanged.addListener(listener);
    }

    /**
     * @param listener the listener to remove
     */
    private void removeSelectionChangedListener(final EventListener listener) {
      evtSelectionChanged.addListener(listener);
    }

    /**
     * @return a state active when multiple rows are selected
     */
    private StateObserver getMultipleSelectionObserver() {
      return stMultipleSelection.getObserver();
    }

    /**
     * @return a state active when a single row is selected
     */
    private StateObserver getSingleSelectionObserver() {
      return stSingleSelection.getObserver();
    }

    /**
     * @return a state active when the selection is empty
     */
    private StateObserver getSelectionEmptyObserver() {
      return stSelectionEmpty.getObserver();
    }
  }

  private static final class RegexFilterCriteria<T> implements FilterCriteria<T> {

    private final Pattern pattern;

    /**
     * Instantiates a new RegexFilterCriteria.
     * @param patternString the regex pattern
     */
    public RegexFilterCriteria(final String patternString) {
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
}
