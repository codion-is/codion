/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.Collator;
import java.util.*;

/**
 * A TableModel implentation that supports filtering, searching and sorting.
 * <pre>
 * AbstractFilteredTableModel tableModel = ...;
 * JTable table = new JTable(tableModel, tableModel.getColumnModel(), tableModel.getSelectionModel());
 * </pre><br>
 * User: Björn Darri<br>
 * Sorting functionality based on TableSorter by Philip Milne, Brendon McLean, Dan van Enckevort and Parwinder Sekhon<br>
 * Date: 18.4.2010<br>
 * Time: 09:48:07<br>
 * @param <T> the type of the values in this table model
 */
public abstract class AbstractFilteredTableModel<T, C> extends AbstractTableModel implements FilteredTableModel<T, C> {

  private static final Comparator<Comparable<Object>> COMPARABLE_COMPARATOR = new Comparator<Comparable<Object>>() {
    public int compare(final Comparable<Object> o1, final Comparable<Object> o2) {
      return (o1.compareTo(o2));
    }
  };
  private static final Comparator<Object> LEXICAL_COMPARATOR = new Comparator<Object>() {
    private final Collator collator = Collator.getInstance();
    public int compare(final Object o1, final Object o2) {
      return collator.compare(o1.toString(), o2.toString());
    }
  };

  private final Event evtFilteringDone = Events.event();
  private final Event evtSortingStarted = Events.event();
  private final Event evtSortingDone = Events.event();
  private final Event evtRefreshStarted = Events.event();
  private final Event evtRefreshDone = Events.event();
  private final Event evtTableDataChanged = Events.event();
  private final Event evtColumnHidden = Events.event();
  private final Event evtColumnShown = Events.event();

  @SuppressWarnings({"unchecked"})
  private static final SortingState EMPTY_SORTING_STATE = new SortingStateImpl(SortingDirective.UNSORTED, -1);

  /**
   * Holds visible items
   */
  private final List<T> visibleItems = new ArrayList<T>();

  /**
   * Holds items that are filtered
   */
  private final List<T> filteredItems = new ArrayList<T>();

  /**
   * The TableColumnModel
   */
  private final TableColumnModel columnModel;

  /**
   * The SearchModels used for filtering
   */
  private final List<? extends ColumnSearchModel<C>> columnFilterModels;

  /**
   * Contains columns that have been hidden
   */
  private final List<TableColumn> hiddenColumns = new ArrayList<TableColumn>();

  /**
   * The selection model
   */
  private final SelectionModel selectionModel;

  /**
   * Caches the column indexes in the model
   */
  private final int[] columnIndexCache;

  /**
   * the filter criteria used by this model
   */
  private final FilterCriteria<T> filterCriteria = new FilterCriteria<T>() {
    public boolean include(final T item) {
      for (final ColumnSearchModel columnFilter : columnFilterModels) {
        if (columnFilter.isEnabled() && !columnFilter.include(item)) {
          return false;
        }
      }

      return true;
    }
  };

  /**
   * true while the model data is being refreshed
   */
  private boolean isRefreshing = false;

  /**
   * true while the model data is being filtered
   */
  private boolean isFiltering = false;

  /**
   * true while the model data is being sorted
   */
  private boolean isSorting = false;

  private final Map<C, SortingState> sortingStates = new HashMap<C, SortingState>();

  /**
   * true if searching the table model should be done via regular expressions
   */
  private boolean regularExpressionSearch = false;

  /**
   * Instantiates a new table model.
   * @param columnModel the column model to base this table model on
   * @param columnFilterModels the column filter models
   */
  public AbstractFilteredTableModel(final TableColumnModel columnModel,
                                    final List<? extends ColumnSearchModel<C>> columnFilterModels) {
    this.columnModel = columnModel;
    this.columnIndexCache = new int[columnModel.getColumnCount()];
    this.columnFilterModels = columnFilterModels;
    this.selectionModel = new SelectionModel(this);
    clearSortingState();
    bindEventsInternal();
  }

  /** {@inheritDoc} */
  public final List<T> getVisibleItems() {
    return Collections.unmodifiableList(visibleItems);
  }

  /** {@inheritDoc} */
  public final List<T> getFilteredItems() {
    return Collections.unmodifiableList(filteredItems);
  }

  /** {@inheritDoc} */
  public final int getVisibleItemCount() {
    return visibleItems.size();
  }

  /** {@inheritDoc} */
  public final int getFilteredItemCount() {
    return filteredItems.size();
  }

  /** {@inheritDoc} */
  public final int getColumnCount() {
    return columnModel.getColumnCount();
  }

  /** {@inheritDoc} */
  public final int getRowCount() {
    return visibleItems.size();
  }

  /** {@inheritDoc} */
  public final boolean contains(final T item, final boolean includeFiltered) {
    final boolean ret = visibleItems.contains(item);
    if (!ret && includeFiltered) {
      return filteredItems.contains(item);
    }

    return ret;
  }

  /** {@inheritDoc} */
  public final boolean isVisible(final T item) {
    return visibleItems.contains(item);
  }

  /** {@inheritDoc} */
  public final boolean isFiltered(final T item) {
    return filteredItems.contains(item);
  }

  /** {@inheritDoc} */
  public final Point findNextItemCoordinate(final int fromIndex, final boolean forward, final String searchText) {
    return findNextItemCoordinate(fromIndex, forward, getSearchCriteria(searchText));
  }

  /** {@inheritDoc} */
  public final Point findNextItemCoordinate(final int fromIndex, final boolean forward, final FilterCriteria<Object> criteria) {
    if (forward) {
      for (int row = fromIndex >= getRowCount() ? 0 : fromIndex; row < getRowCount(); row++) {
        for (int column = 0; column < getColumnCount(); column++) {
          if (criteria.include(getSearchValueAt(row, column))) {
            return new Point(column, row);
          }
        }
      }
    }
    else {
      for (int row = fromIndex < 0 ? getRowCount() - 1 : fromIndex; row >= 0; row--) {
        for (int column = 0; column < getColumnCount(); column++) {
          if (criteria.include(getSearchValueAt(row, column))) {
            return new Point(column, row);
          }
        }
      }
    }

    return null;
  }

  /** {@inheritDoc} */
  public final void refresh() {
    if (isRefreshing) {
      return;
    }
    try {
      clearSortingState();
      isRefreshing = true;
      evtRefreshStarted.fire();
      doRefresh();
    }
    finally {
      isRefreshing = false;
      evtRefreshDone.fire();
    }
  }

  /** {@inheritDoc} */
  public final void clear() {
    filteredItems.clear();
    final int size = getRowCount();
    if (size > 0) {
      visibleItems.clear();
      fireTableRowsDeleted(0, size - 1);
    }
  }

  /** {@inheritDoc} */
  public final ListSelectionModel getSelectionModel() {
    return selectionModel;
  }

  /** {@inheritDoc} */
  public final int getSortingPriority(final C columnIdentifier) {
    return getSortingState(columnIdentifier).getPriority();
  }

  /** {@inheritDoc} */
  public final SortingDirective getSortingDirective(final C columnIdentifier) {
    return getSortingState(columnIdentifier).getDirective();
  }

  /** {@inheritDoc} */
  public final void setSortingDirective(final C columnIdentifier, final SortingDirective directive) {
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
    sortingStatusChanged();
  }

  /** {@inheritDoc} */
  @SuppressWarnings({"unchecked"})
  public final void clearSortingState() {
    final Enumeration<TableColumn> columns = columnModel.getColumns();
    while (columns.hasMoreElements()) {
      sortingStates.put((C) columns.nextElement().getIdentifier(), EMPTY_SORTING_STATE);
    }
    evtSortingDone.fire();
  }

  /** {@inheritDoc} */
  public final int compare(final T objectOne, final T objectTwo, final C columnIdentifier, final SortingDirective directive) {
    final Comparable valueOne = getComparable(objectOne, columnIdentifier);
    final Comparable valueTwo = getComparable(objectTwo, columnIdentifier);
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
      //noinspection unchecked
      comparison = getComparator(columnIdentifier).compare(valueOne, valueTwo);
    }
    if (comparison != 0) {
      return directive == SortingDirective.DESCENDING ? -comparison : comparison;
    }

    return 0;
  }

  /** {@inheritDoc} */
  public final boolean isRegularExpressionSearch() {
    return regularExpressionSearch;
  }

  /** {@inheritDoc} */
  public final void setRegularExpressionSearch(final boolean value) {
    this.regularExpressionSearch = value;
  }

  /** {@inheritDoc} */
  public final ColumnSearchModel<C> getFilterModel(final int columnIndex) {
    return columnFilterModels.get(columnIndex);
  }

  /** {@inheritDoc} */
  public final List<ColumnSearchModel<C>> getFilterModels() {
    return Collections.unmodifiableList(columnFilterModels);
  }

  /** {@inheritDoc} */
  public final void selectAll() {
    getSelectionModel().setSelectionInterval(0, getVisibleItemCount() - 1);
  }

  /** {@inheritDoc} */
  public final void clearSelection() {
    getSelectionModel().clearSelection();
  }

  /** {@inheritDoc} */
  public final Collection<Integer> getSelectedIndexes() {
    final List<Integer> indexes = new ArrayList<Integer>();
    final int min = selectionModel.getMinSelectionIndex();
    final int max = selectionModel.getMaxSelectionIndex();
    for (int i = min; i <= max; i++) {
      if (selectionModel.isSelectedIndex(i)) {
        indexes.add(i);
      }
    }

    return indexes;
  }

  /** {@inheritDoc} */
  public final void moveSelectionUp() {
    if (!visibleItems.isEmpty()) {
      if (getSelectionModel().isSelectionEmpty()) {
        getSelectionModel().setSelectionInterval(visibleItems.size() - 1, visibleItems.size() - 1);
      }
      else {
        final Collection<Integer> selected = getSelectedIndexes();
        final List<Integer> newSelected = new ArrayList<Integer>(selected.size());
        for (final Integer index : selected) {
          newSelected.add(index == 0 ? visibleItems.size() - 1 : index - 1);
        }

        setSelectedItemIndexes(newSelected);
      }
    }
  }

  /** {@inheritDoc} */
  public final void moveSelectionDown() {
    if (!visibleItems.isEmpty()) {
      if (getSelectionModel().isSelectionEmpty()) {
        getSelectionModel().setSelectionInterval(0, 0);
      }
      else {
        final Collection<Integer> selected = getSelectedIndexes();
        final List<Integer> newSelected = new ArrayList<Integer>(selected.size());
        for (final Integer index : selected) {
          newSelected.add(index == visibleItems.size() - 1 ? 0 : index + 1);
        }

        setSelectedItemIndexes(newSelected);
      }
    }
  }

  /** {@inheritDoc} */
  public final void setSelectedItemIndex(final int index) {
    selectionModel.setSelectionInterval(index, index);
  }

  /** {@inheritDoc} */
  public final void addSelectedItemIndex(final int index) {
    selectionModel.addSelectionInterval(index, index);
  }

  /** {@inheritDoc} */
  public final void setSelectedItemIndexes(final List<Integer> indexes) {
    selectionModel.clearSelection();
    selectionModel.addSelectedItemIndexes(indexes);
  }

  /** {@inheritDoc} */
  public final List<T> getSelectedItems() {
    final Collection<Integer> selectedModelIndexes = getSelectedIndexes();
    final List<T> selectedItems = new ArrayList<T>();
    for (final int modelIndex : selectedModelIndexes) {
      selectedItems.add(getItemAt(modelIndex));
    }

    return selectedItems;
  }

  /** {@inheritDoc} */
  public final void setSelectedItems(final List<T> items) {
    final List<Integer> indexes = new ArrayList<Integer>();
    for (final T item : items) {
      final int index = indexOf(item);
      if (index >= 0) {
        indexes.add(index);
      }
    }

    setSelectedItemIndexes(indexes);
  }

  /** {@inheritDoc} */
  public final T getSelectedItem() {
    final int index = selectionModel.getSelectedIndex();
    if (index >= 0 && index < getVisibleItemCount()) {
      return getItemAt(index);
    }
    else {
      return null;
    }
  }

  /** {@inheritDoc} */
  public final void setSelectedItem(final T item) {
    setSelectedItems(Arrays.asList(item));
  }

  /** {@inheritDoc} */
  public final void addSelectedItemIndexes(final List<Integer> indexes) {
    selectionModel.addSelectedItemIndexes(indexes);
  }

  /** {@inheritDoc} */
  public final int getSelectedIndex() {
    return selectionModel.getSelectedIndex();
  }

  /** {@inheritDoc} */
  public final int getSelectionCount() {
    return selectionModel.getSelectionCount();
  }

  /** {@inheritDoc} */
  public final boolean isSelectionEmpty() {
    return selectionModel.isSelectionEmpty();
  }

  /** {@inheritDoc} */
  public final T getItemAt(final int index) {
    return visibleItems.get(index);
  }

  /** {@inheritDoc} */
  public final int indexOf(final T item) {
    return visibleItems.indexOf(item);
  }

  /** {@inheritDoc} */
  public final void filterContents() {
    try {
      isFiltering = true;
      final List<T> selectedItems = getSelectedItems();
      visibleItems.addAll(filteredItems);
      filteredItems.clear();
      for (final ListIterator<T> iterator = visibleItems.listIterator(); iterator.hasNext();) {
        final T item = iterator.next();
        if (!filterCriteria.include(item)) {
          filteredItems.add(item);
          iterator.remove();
        }
      }
      fireTableChanged(new TableModelEvent(this, 0, Integer.MAX_VALUE, -1));
      setSelectedItems(selectedItems);
    }
    finally {
      isFiltering = false;
      evtFilteringDone.fire();
    }
  }

  /** {@inheritDoc} */
  public final FilterCriteria<T> getFilterCriteria() {
    return filterCriteria;
  }

  /** {@inheritDoc} */
  public final void setFilterCriteria(final FilterCriteria<T> filterCriteria) {
    throw new UnsupportedOperationException("AbstractFilteredTableModel.setFilterCriteria(FilterCriteria)");
  }

  /** {@inheritDoc} */
  public final List<T> getAllItems() {
    final List<T> entities = new ArrayList<T>(visibleItems);
    entities.addAll(filteredItems);

    return entities;
  }

  /** {@inheritDoc} */
  public final void removeItems(final List<T> items) {
    for (final T item : items) {
      removeItem(item);
    }
  }

  /** {@inheritDoc} */
  public final void removeItem(final T item) {
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
  public final TableColumnModel getColumnModel() {
    return columnModel;
  }

  /** {@inheritDoc} */
  public final TableColumn getTableColumn(final Object identifier) {
    return columnModel.getColumn(columnModel.getColumnIndex(identifier));
  }

  /** {@inheritDoc} */
  public final void setColumnVisible(final Object columnIdentifier, final boolean visible) {
    if (visible) {
      if (!isColumnVisible(columnIdentifier)) {
        showColumn(columnIdentifier);
      }
    }
    else {
      if (isColumnVisible(columnIdentifier)) {
        hideColumn(columnIdentifier);
      }
    }
  }

  /** {@inheritDoc} */
  public final void showColumn(final Object columnIdentifier) {
    final ListIterator<TableColumn> hiddenColumnIterator = hiddenColumns.listIterator();
    while (hiddenColumnIterator.hasNext()) {
      final TableColumn hiddenColumn = hiddenColumnIterator.next();
      if (hiddenColumn.getIdentifier().equals(columnIdentifier)) {
        hiddenColumnIterator.remove();
        columnModel.addColumn(hiddenColumn);
        evtColumnShown.fire(new ActionEvent(hiddenColumn.getIdentifier(), 0, "showColumn"));
      }
    }
  }

  /** {@inheritDoc} */
  public final void hideColumn(final Object columnIdentifier) {
    final TableColumn column = getTableColumn(columnIdentifier);
    columnModel.removeColumn(column);
    hiddenColumns.add(column);
    evtColumnHidden.fire(new ActionEvent(columnIdentifier, 0, "hideColumn"));
  }

  /** {@inheritDoc} */
  public final boolean isColumnVisible(final Object columnIdentifier) {
    for (final TableColumn column : hiddenColumns) {
      if (column.getIdentifier().equals(columnIdentifier)) {
        return false;
      }
    }

    return true;
  }

  /** {@inheritDoc} */
  public final List<TableColumn> getHiddenColumns() {
    return Collections.unmodifiableList(hiddenColumns);
  }

  /** {@inheritDoc} */
  public final StateObserver getSelectionEmptyState() {
    return selectionModel.getSelectionEmptyState();
  }

  /** {@inheritDoc} */
  public final StateObserver getMultipleSelectionState() {
    return selectionModel.getMultipleSelectionState();
  }

  /** {@inheritDoc} */
  public final void addColumnHiddenListener(final ActionListener listener) {
    evtColumnHidden.addListener(listener);
  }

  /** {@inheritDoc} */
  public final void removeColumnHiddenListener(final ActionListener listener) {
    evtColumnHidden.removeListener(listener);
  }

  /** {@inheritDoc} */
  public final void addColumnShownListener(final ActionListener listener) {
    evtColumnShown.addListener(listener);
  }

  /** {@inheritDoc} */
  public final void removeColumnShownListener(final ActionListener listener) {
    evtColumnShown.removeListener(listener);
  }

  /** {@inheritDoc} */
  public final void addRefreshStartedListener(final ActionListener listener) {
    evtRefreshStarted.addListener(listener);
  }

  /** {@inheritDoc} */
  public final void removeRefreshStartedListener(final ActionListener listener) {
    evtRefreshStarted.removeListener(listener);
  }

  /** {@inheritDoc} */
  public final void addRefreshDoneListener(final ActionListener listener) {
    evtRefreshDone.addListener(listener);
  }

  /** {@inheritDoc} */
  public final void removeRefreshDoneListener(final ActionListener listener) {
    evtRefreshDone.removeListener(listener);
  }

  /** {@inheritDoc} */
  public final void addFilteringListener(final ActionListener listener) {
    evtFilteringDone.addListener(listener);
  }

  /** {@inheritDoc} */
  public final void removeFilteringListener(final ActionListener listener) {
    evtFilteringDone.removeListener(listener);
  }

  /** {@inheritDoc} */
  public final void addSortingListener(final ActionListener listener) {
    evtSortingDone.addListener(listener);
  }

  /** {@inheritDoc} */
  public final void removeSortingListener(final ActionListener listener) {
    evtSortingDone.removeListener(listener);
  }

  /** {@inheritDoc} */
  public final void addSelectedIndexListener(final ActionListener listener) {
    selectionModel.addSelectedIndexListener(listener);
  }

  /** {@inheritDoc} */
  public final void removeSelectedIndexListener(final ActionListener listener) {
    selectionModel.removeSelectedIndexListener(listener);
  }

  /** {@inheritDoc} */
  public final void addSelectionChangedListener(final ActionListener listener) {
    selectionModel.addSelectionChangedListener(listener);
  }

  /** {@inheritDoc} */
  public final void removeSelectionChangedListener(final ActionListener listener) {
    selectionModel.removeSelectionChangedListener(listener);
  }

  /** {@inheritDoc} */
  public final void addTableDataChangedListener(final ActionListener listener) {
    evtTableDataChanged.addListener(listener);
  }

  /** {@inheritDoc} */
  public final void removeTableDataChangedListener(final ActionListener listener) {
    evtTableDataChanged.removeListener(listener);
  }

  /**
   * Refreshes the data in this table model.
   */
  protected abstract void doRefresh();

  /**
   * @param object the value
   * @param columnIdentifier the column identifier
   * @return a Comparable for the given value and column index
   */
  protected Comparable getComparable(final Object object, final C columnIdentifier) {
    return (Comparable) object;
  }

  /**
   * Maps the index of the column in the table model at
   * <code>modelColumnIndex</code> to the index of the column
   * in the view.  Returns the index of the
   * corresponding column in the view; returns -1 if this column is not
   * being displayed.  If <code>modelColumnIndex</code> is less than zero,
   * returns <code>modelColumnIndex</code>.
   * @param modelColumnIndex the index of the column in the model
   * @return the index of the corresponding column in the view
   */
  protected final int convertColumnIndexToView(final int modelColumnIndex) {
    if (modelColumnIndex < 0) {
      return modelColumnIndex;
    }

    final int cachedIndex = columnIndexCache[modelColumnIndex];
    if (cachedIndex > 0) {
      return cachedIndex;
    }

    for (int index = 0; index < getColumnCount(); index++) {
      if (columnModel.getColumn(index).getModelIndex() == modelColumnIndex) {
        columnIndexCache[modelColumnIndex] = index;
        return index;
      }
    }

    return -1;
  }

  /**
   * Adds the given items to this table model
   * @param items the items to add
   * @param atFront if true then the items are added at the front
   */
  protected final void addItems(final List<T> items, final boolean atFront) {
    for (final T item : items) {
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
    fireTableDataChanged();
  }

  /**
   * Returns the value to use when searching through the table.
   * @param rowIndex the row index
   * @param columnIndex the column index
   * @return the search value
   */
  protected String getSearchValueAt(final int rowIndex, final int columnIndex) {
    final Object value = getValueAt(rowIndex, columnIndex);

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
   * @return true while this table model is being filtered
   */
  protected final boolean isFiltering() {
    return isFiltering;
  }

  /**
   * @return true while this table model is being sorted
   */
  protected final boolean isSorting() {
    return isSorting;
  }

  private void bindEventsInternal() {
    final List<T> selectedItems = new ArrayList<T>();
    addTableModelListener(new TableModelListener() {
      public void tableChanged(final TableModelEvent e) {
        evtTableDataChanged.fire();
      }
    });
    evtSortingStarted.addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        isSorting = true;
        selectedItems.clear();
        selectedItems.addAll(getSelectedItems());
      }
    });
    evtSortingDone.addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        isSorting = false;
        setSelectedItems(selectedItems);
        selectedItems.clear();
      }
    });
    for (final ColumnSearchModel searchModel : columnFilterModels) {
      searchModel.addSearchStateListener(new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
          filterContents();
        }
      });
    }
    columnModel.addColumnModelListener(new TableColumnModelListener() {
      public void columnAdded(final TableColumnModelEvent e) {
        Arrays.fill(columnIndexCache, -1);
      }
      public void columnRemoved(final TableColumnModelEvent e) {
        Arrays.fill(columnIndexCache, -1);
      }
      public void columnMoved(final TableColumnModelEvent e) {
        Arrays.fill(columnIndexCache, -1);
      }
      public void columnMarginChanged(final ChangeEvent e) {}
      public void columnSelectionChanged(final ListSelectionEvent e) {}
    });
  }

  private Comparator getComparator(final C columnIdentifier) {
    final Class columnClass = getColumnClass(columnIdentifier);
    if (columnClass.equals(String.class)) {
      return LEXICAL_COMPARATOR;
    }
    if (Comparable.class.isAssignableFrom(columnClass)) {
      return COMPARABLE_COMPARATOR;
    }

    return LEXICAL_COMPARATOR;
  }

  private SortingState getSortingState(final C columnIdentifier) {
    final SortingState state = sortingStates.get(columnIdentifier);
    if (state == null) {
      throw new RuntimeException("No sorting state assigned to column identified by : " + columnIdentifier);
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

  private void sortingStatusChanged() {
    evtSortingStarted.fire();
    Collections.sort(visibleItems, new Comparator<T>() {
      public int compare(final T o1, final T o2) {
        /** {@inheritDoc} */
        for (final Map.Entry<C, SortingState> state : getOrderedSortingStates()) {
          final int comparison = AbstractFilteredTableModel.this.compare(o1, o2, state.getKey(), state.getValue().getDirective());
          if (comparison != 0) {
            return comparison;
          }
        }

        return 0;
      }
    });
    fireTableDataChanged();
    evtSortingDone.fire();
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
      public int compare(Map.Entry<C, SortingState> o1, Map.Entry<C, SortingState> o2) {
        final Integer priorityOne = o1.getValue().getPriority();
        final Integer priorityTwo = o2.getValue().getPriority();

        return priorityOne.compareTo(priorityTwo);
      }
    });

    return entries;
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
    public int getPriority() {
      return priority;
    }

    /** {@inheritDoc} */
    public SortingDirective getDirective() {
      return direction;
    }
  }

  private static final class SelectionModel extends DefaultListSelectionModel {

    private final Event evtSelectionChanged = Events.event();
    private final Event evtSelectedIndexChanged = Events.event();
    private final State stSelectionEmpty = States.state(true);
    private final State stMultipleSelection = States.state(false);

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
      stMultipleSelection.setActive(getSelectionCount() > 1);
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
    public int getSelectionCount() {
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
    public void addSelectedItemIndexes(final List<Integer> indexes) {
      try {
        isUpdatingSelection = true;
        for (int i = 0; i < indexes.size()-1; i++) {
          final int index = indexes.get(i);
          addSelectionInterval(index, index);
        }
      }
      finally {
        isUpdatingSelection = false;
        if (!indexes.isEmpty()) {
          final int lastIndex = indexes.get(indexes.size()-1);
          addSelectionInterval(lastIndex, lastIndex);
        }
      }
    }

    /**
     * @return true while the selection is being updated
     */
    public boolean isUpdatingSelection() {
      return isUpdatingSelection;
    }

    /**
     * @return the topmost (lowest row index) selected index
     */
    public int getSelectedIndex() {
      return selectedIndex;
    }

    /**
     * @param listener a listener notified each time the topmost (lowest row index) selected index changes
     */
    public void addSelectedIndexListener(final ActionListener listener) {
      evtSelectedIndexChanged.addListener(listener);
    }

    /**
     * @param listener the listener to remove
     */
    public void removeSelectedIndexListener(final ActionListener listener) {
      evtSelectedIndexChanged.addListener(listener);
    }

    /**
     * @param listener a listener notified each time the selection changes
     */
    public void addSelectionChangedListener(final ActionListener listener) {
      evtSelectionChanged.addListener(listener);
    }

    /**
     * @param listener the listener to remove
     */
    public void removeSelectionChangedListener(final ActionListener listener) {
      evtSelectionChanged.addListener(listener);
    }

    /**
     * @return a state active when multiple rows are selected
     */
    public StateObserver getMultipleSelectionState() {
      return stMultipleSelection.getObserver();
    }

    /**
     * @return a state active when the selection is empty
     */
    public StateObserver getSelectionEmptyState() {
      return stSelectionEmpty.getObserver();
    }
  }
}
