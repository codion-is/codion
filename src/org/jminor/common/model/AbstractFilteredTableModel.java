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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

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

  public static final Comparator<Comparable<Object>> COMPARABLE_COMPARATOR = new Comparator<Comparable<Object>>() {
    public int compare(final Comparable<Object> o1, final Comparable<Object> o2) {
      return (o1.compareTo(o2));
    }
  };
  public static final Comparator<Object> LEXICAL_COMPARATOR = new Comparator<Object>() {
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

  private static final SortingState EMPTY_SORTING_STATE = new SortingStateImpl(-1, SortingDirective.UNSORTED);

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
  private final List<? extends SearchModel<C>> columnFilterModels;

  /**
   * Contains columns that have been hidden
   */
  private final List<TableColumn> hiddenColumns = new ArrayList<TableColumn>();

  /**
   * The selection model
   */
  private final SelectionModel selectionModel = new SelectionModel();

  /**
   * Caches the column indexes in the model
   */
  private final int[] columnIndexCache;

  /**
   * the filter criteria used by this model
   */
  private final FilterCriteria<T> filterCriteria = new FilterCriteria<T>() {
    public boolean include(final T item) {
      for (final SearchModel columnFilter : columnFilterModels) {
        if (columnFilter.isSearchEnabled() && !columnFilter.include(item)) {
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

  private final List<Row<T>> viewToModel = new ArrayList<Row<T>>();
  private final Map<Integer, Integer> modelToView = new HashMap<Integer, Integer>();
  private final List<SortingState> sortingStates = new ArrayList<SortingState>();

  /**
   * true if searching the table model should be done via regular expressions
   */
  private boolean regularExpressionSearch = false;

  public AbstractFilteredTableModel(final TableColumnModel columnModel,
                                    final List<? extends SearchModel<C>> columnFilterModels) {
    this.columnModel = columnModel;
    this.columnIndexCache = new int[columnModel.getColumnCount()];
    this.columnFilterModels = columnFilterModels;
    addTableModelListener(new SortHandler());
    bindEventsInternal();
  }

  public final List<T> getVisibleItems() {
    return Collections.unmodifiableList(visibleItems);
  }

  public final List<T> getFilteredItems() {
    return Collections.unmodifiableList(filteredItems);
  }

  public final int getVisibleItemCount() {
    return visibleItems.size();
  }

  public final int getFilteredItemCount() {
    return filteredItems.size();
  }

  public final int getColumnCount() {
    return columnModel.getColumnCount();
  }

  public final int getRowCount() {
    return visibleItems.size();
  }

  public final boolean contains(final T item, final boolean includeFiltered) {
    final boolean ret = visibleItems.contains(item);
    if (!ret && includeFiltered) {
      return filteredItems.contains(item);
    }

    return ret;
  }

  public final boolean isVisible(final T item) {
    return visibleItems.contains(item);
  }

  public final boolean isFiltered(final T item) {
    return filteredItems.contains(item);
  }

  public final Point findNextItemCoordinate(final int fromIndex, final boolean forward, final String searchText) {
    return findNextItemCoordinate(fromIndex, forward, getSearchCriteria(searchText));
  }

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

  public final void refresh() {
    if (isRefreshing) {
      return;
    }
    try {
      isRefreshing = true;
      evtRefreshStarted.fire();
      doRefresh();
    }
    finally {
      isRefreshing = false;
      evtRefreshDone.fire();
    }
  }

  public final void clear() {
    filteredItems.clear();
    final int size = getRowCount();
    if (size > 0) {
      visibleItems.clear();
      fireTableRowsDeleted(0, size - 1);
    }
  }

  public final ListSelectionModel getSelectionModel() {
    return selectionModel;
  }

  public final int getSortPriority(final int columnIndex) {
    int i = 0;
    for (final SortingState state : sortingStates) {
      if (state.getColumnIndex() == columnIndex) {
        return i;
      }
      i++;
    }

    return -1;
  }

  public final SortingDirective getSortingDirective(final int columnIndex) {
    return getSortingState(columnIndex).getDirective();
  }

  public final void setSortingDirective(final int columnIndex, final SortingDirective directive) {
    final SortingState state = getSortingState(columnIndex);
    if (!state.equals(EMPTY_SORTING_STATE)) {
      sortingStates.remove(state);
    }
    if (directive != SortingDirective.UNSORTED) {
      sortingStates.add(new SortingStateImpl(columnIndex, directive));
    }
    sortingStatusChanged();
  }

  public final int compare(final T objectOne, final T objectTwo, final int columnIndex, final SortingDirective directive) {
    final Comparable valueOne = getComparable(objectOne, columnIndex);
    final Comparable valueTwo = getComparable(objectTwo, columnIndex);
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
      comparison = getComparator(columnIndex).compare(valueOne, valueTwo);
    }
    if (comparison != 0) {
      return directive == SortingDirective.DESCENDING ? -comparison : comparison;
    }

    return 0;
  }

  public final boolean isRegularExpressionSearch() {
    return regularExpressionSearch;
  }

  public final void setRegularExpressionSearch(final boolean value) {
    this.regularExpressionSearch = value;
  }

  public final SearchModel<C> getFilterModel(final int columnIndex) {
    return columnFilterModels.get(columnIndex);
  }

  public final List<SearchModel<C>> getFilterModels() {
    return Collections.unmodifiableList(columnFilterModels);
  }

  public final void selectAll() {
    getSelectionModel().setSelectionInterval(0, getVisibleItemCount() - 1);
  }

  public final void clearSelection() {
    getSelectionModel().clearSelection();
  }

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

  public final void setSelectedItemIndex(final int index) {
    selectionModel.setSelectionInterval(index, index);
  }

  public final void addSelectedItemIndex(final int index) {
    selectionModel.addSelectionInterval(index, index);
  }

  public final void setSelectedItemIndexes(final List<Integer> indexes) {
    selectionModel.clearSelection();
    selectionModel.addSelectedItemIndexes(indexes);
  }

  public final List<T> getSelectedItems() {
    final Collection<Integer> selectedModelIndexes = getSelectedIndexes();
    final List<T> selectedItems = new ArrayList<T>();
    for (final int modelIndex : selectedModelIndexes) {
      selectedItems.add(getItemAt(modelIndex));
    }

    return selectedItems;
  }

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

  public final T getSelectedItem() {
    final int index = selectionModel.getSelectedIndex();
    if (index >= 0 && index < getVisibleItemCount()) {
      return getItemAt(index);
    }
    else {
      return null;
    }
  }

  public final void setSelectedItem(final T item) {
    setSelectedItems(Arrays.asList(item));
  }

  /**
   * Adds these indexes to the selection
   * @param indexes the indexes to add to the selection
   */
  public final void addSelectedItemIndexes(final List<Integer> indexes) {
    selectionModel.addSelectedItemIndexes(indexes);
  }

  public final int getSelectedIndex() {
    return selectionModel.getSelectedIndex();
  }

  public final int getSelectionCount() {
    return selectionModel.getSelectionCount();
  }

  public final boolean isSelectionEmpty() {
    return selectionModel.isSelectionEmpty();
  }

  public final T getItemAt(final int index) {
    return visibleItems.get(modelIndex(index));
  }

  public final int indexOf(final T item) {
    return viewIndex(visibleItems.indexOf(item));
  }

  public final void clearSortingState() {
    sortingStates.clear();
    sortingStatusChanged();
  }

  /**
   * Filters this table model
   * @see #addFilteringListener(java.awt.event.ActionListener)
   */
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

  public final FilterCriteria<T> getFilterCriteria() {
    return filterCriteria;
  }

  public final void setFilterCriteria(final FilterCriteria<T> filterCriteria) {
    throw new UnsupportedOperationException("AbstractFilteredTableModel.setFilterCriteria(FilterCriteria)");
  }

  public final List<T> getAllItems() {
    final List<T> entities = new ArrayList<T>(visibleItems);
    entities.addAll(filteredItems);

    return entities;
  }

  /**
   * Removes the given items from this table model
   * @param items the items to remove from the model
   */
  public final void removeItems(final List<T> items) {
    for (final T item : items) {
      removeItem(item);
    }
  }

  /**
   * Removes the given item from this table model
   * @param item the item to remove from the model
   */
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

  public final TableColumnModel getColumnModel() {
    return columnModel;
  }

  /**
   * Returns the TableColumn for the given identifier
   * @param identifier the identifier for which to retrieve the column
   * @return the TableColumn associated with the given identifier
   */
  public final TableColumn getTableColumn(final Object identifier) {
    return columnModel.getColumn(columnModel.getColumnIndex(identifier));
  }

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

  public final void hideColumn(final Object columnIdentifier) {
    final TableColumn column = getTableColumn(columnIdentifier);
    columnModel.removeColumn(column);
    hiddenColumns.add(column);
    evtColumnHidden.fire(new ActionEvent(columnIdentifier, 0, "hideColumn"));
  }

  public final boolean isColumnVisible(final Object columnIdentifier) {
    for (final TableColumn column : hiddenColumns) {
      if (column.getIdentifier().equals(columnIdentifier)) {
        return false;
      }
    }

    return true;
  }

  public final List<TableColumn> getHiddenColumns() {
    return Collections.unmodifiableList(hiddenColumns);
  }

  public final State stateSelectionEmpty() {
    return selectionModel.stateSelectionEmpty().getLinkedState();
  }

  /**
   * @return a State active when multiple rows are selected
   */
  public final State stateMultipleSelection() {
    return selectionModel.stateMultipleSelection().getLinkedState();
  }

  public final void addColumnHiddenListener(final ActionListener listener) {
    evtColumnHidden.addListener(listener);
  }

  public final void removeColumnHiddenListener(final ActionListener listener) {
    evtColumnHidden.removeListener(listener);
  }

  public final void addColumnShownListener(final ActionListener listener) {
    evtColumnShown.addListener(listener);
  }

  public final void removeColumnShownListener(final ActionListener listener) {
    evtColumnShown.removeListener(listener);
  }

  public final void addRefreshStartedListener(final ActionListener listener) {
    evtRefreshStarted.addListener(listener);
  }

  public final void removeRefreshStartedListener(final ActionListener listener) {
    evtRefreshStarted.removeListener(listener);
  }

  public final void addRefreshDoneListener(final ActionListener listener) {
    evtRefreshDone.addListener(listener);
  }

  public final void removeRefreshDoneListener(final ActionListener listener) {
    evtRefreshDone.removeListener(listener);
  }

  public final void addFilteringListener(final ActionListener listener) {
    evtFilteringDone.addListener(listener);
  }

  public final void removeFilteringListener(final ActionListener listener) {
    evtFilteringDone.removeListener(listener);
  }

  public final void addSortingListener(final ActionListener listener) {
    evtSortingDone.addListener(listener);
  }

  public final void removeSortingListener(final ActionListener listener) {
    evtSortingDone.removeListener(listener);
  }

  public final void addSelectedIndexListener(final ActionListener listener) {
    selectionModel.addSelectedIndexListener(listener);
  }

  public final void removeSelectedIndexListener(final ActionListener listener) {
    selectionModel.removeSelectedIndexListener(listener);
  }

  public final void addSelectionChangedListener(final ActionListener listener) {
    selectionModel.addSelectionChangedListener(listener);
  }

  public final void removeSelectionChangedListener(final ActionListener listener) {
    selectionModel.removeSelectionChangedListener(listener);
  }

  public final void addTableDataChangedListener(final ActionListener listener) {
    evtTableDataChanged.addListener(listener);
  }

  public final void removeTableDataChangedListener(final ActionListener listener) {
    evtTableDataChanged.removeListener(listener);
  }

  protected abstract void doRefresh();

  protected Comparable getComparable(final Object object, final int columnIndex) {
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

  protected final FilterCriteria<Object> getSearchCriteria(final String searchText) {
    if (regularExpressionSearch) {
      return new RegexFilterCriteria<Object>(searchText);
    }

    return new FilterCriteria<Object>() {
      public boolean include(final Object item) {
        return !(item == null || searchText == null) && item.toString().toLowerCase().contains(searchText.toLowerCase());
      }
    };
  }

  protected final boolean isFiltering() {
    return isFiltering;
  }

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
    for (final SearchModel searchModel : columnFilterModels) {
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

  private Comparator getComparator(final int column) {
    final Class columnClass = getColumnClass(column);
    if (columnClass.equals(String.class)) {
      return LEXICAL_COMPARATOR;
    }
    if (Comparable.class.isAssignableFrom(columnClass)) {
      return COMPARABLE_COMPARATOR;
    }

    return LEXICAL_COMPARATOR;
  }

  private int viewIndex(final int modelIndex) {
    final Map<Integer, Integer> view = getModelToView();
    if (!view.isEmpty() && modelIndex >= 0 && modelIndex < view.size()) {
      return view.get(modelIndex);
    }

    return -1;
  }

  private int modelIndex(final int viewIndex) {
    final List<Row<T>> model = getViewToModel();
    if (!model.isEmpty() && viewIndex >= 0 && viewIndex < model.size()) {
      return model.get(viewIndex).getModelIndex();
    }

    return -1;
  }

  private List<Row<T>> getViewToModel() {
    if (!visibleItems.isEmpty() && viewToModel.isEmpty()) {
      final int tableModelRowCount = getRowCount();
      for (int row = 0; row < tableModelRowCount; row++) {
        viewToModel.add(new Row<T>(row, this));
      }

      if (isSorted()) {
        Collections.sort(viewToModel);
      }
    }
    return viewToModel;
  }

  private boolean isSorted() {
    return !sortingStates.isEmpty();
  }

  private SortingState getSortingState(final int column) {
    for (final SortingState sortingColumn : sortingStates) {
      if (sortingColumn.getColumnIndex() == column) {
        return sortingColumn;
      }
    }
    return EMPTY_SORTING_STATE;
  }

  private void sortingStatusChanged() {
    evtSortingStarted.fire();
    clearSorting();
    fireTableDataChanged();
    evtSortingDone.fire();
  }

  private Map<Integer, Integer> getModelToView() {
    if (!visibleItems.isEmpty() && modelToView.isEmpty()) {
      final int n = getViewToModel().size();
      for (int i = 0; i < n; i++) {
        modelToView.put(modelIndex(i), i);
      }
    }
    return modelToView;
  }

  private void clearSorting() {
    viewToModel.clear();
    modelToView.clear();
  }

  private List<SortingState> getSortingStates() {
    return sortingStates;
  }

  private final class SortHandler implements TableModelListener {
    public void tableChanged(final TableModelEvent e) {
      // If we're not sorting by anything, just pass the event along.
      if (!isSorted()) {
        clearSorting();
        return;
      }

      // If the table structure has changed, cancel the sorting; the
      // sorting columns may have been either moved or deleted from
      // the model.
      if (e.getFirstRow() == TableModelEvent.HEADER_ROW) {
        clearSortingState();
        return;
      }

      // Something has happened to the data that may have invalidated the row order.
      clearSorting();
    }
  }

  private static final class Row<T> implements Comparable<Row<T>> {

    private final AbstractFilteredTableModel<T, ?> tableModel;
    private final int modelIndex;

    private Row(final int modelIndex, final AbstractFilteredTableModel<T, ?> tableModel) {
      this.modelIndex = modelIndex;
      this.tableModel = tableModel;
    }

    public int getModelIndex() {
      return modelIndex;
    }

    public int compareTo(final Row<T> o) {
      final T one = tableModel.getItemAt(modelIndex);
      final T two = tableModel.getItemAt(o.modelIndex);

      for (final SortingState directive : tableModel.getSortingStates()) {
        final int comparison = tableModel.compare(one, two, directive.getColumnIndex(), directive.getDirective());
        if (comparison != 0) {
          return comparison;
        }
      }

      return 0;
    }

    @Override
    public boolean equals(final Object obj) {
      //noinspection unchecked
      return obj instanceof Row && ((Row) obj).modelIndex == modelIndex;
    }

    @Override
    public int hashCode() {
      return modelIndex;
    }
  }

  private static final class SortingStateImpl implements SortingState {
    private final int column;
    private final SortingDirective direction;

    private SortingStateImpl(final int column, final SortingDirective direction) {
      this.column = column;
      this.direction = direction;
    }

    public int getColumnIndex() {
      return column;
    }

    public SortingDirective getDirective() {
      return direction;
    }
  }

  private final class SelectionModel extends DefaultListSelectionModel {

    private final Event evtSelectionChanged = Events.event();
    private final Event evtSelectedIndexChanged = Events.event();
    private final State stSelectionEmpty = States.state(true);
    private final State stMultipleSelection = States.state(false);

    /**
     * true while the selection is being updated
     */
    private boolean isUpdatingSelection = false;
    /**
     * Holds the topmost (minimum) selected index
     */
    private int selectedIndex = -1;

    @Override
    public void fireValueChanged(final int firstIndex, final int lastIndex, final boolean isAdjusting) {
      super.fireValueChanged(firstIndex, lastIndex, isAdjusting);
      stSelectionEmpty.setActive(SelectionModel.this.isSelectionEmpty());
      stMultipleSelection.setActive(getSelectionCount() > 1);
      final int minSelIndex = getMinSelectionIndex();
      if (selectedIndex != minSelIndex) {
        selectedIndex = minSelIndex;
        evtSelectedIndexChanged.fire();
      }
      if (!(isAdjusting || isUpdatingSelection || isSorting)) {
        evtSelectionChanged.fire();
      }
    }

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

    public boolean isUpdatingSelection() {
      return isUpdatingSelection;
    }

    public int getSelectedIndex() {
      return selectedIndex;
    }

    public void addSelectedIndexListener(final ActionListener listener) {
      evtSelectedIndexChanged.addListener(listener);
    }

    public void removeSelectedIndexListener(final ActionListener listener) {
      evtSelectedIndexChanged.addListener(listener);
    }

    public void addSelectionChangedListener(final ActionListener listener) {
      evtSelectionChanged.addListener(listener);
    }

    public void removeSelectionChangedListener(final ActionListener listener) {
      evtSelectionChanged.addListener(listener);
    }

    public State stateMultipleSelection() {
      return stMultipleSelection.getLinkedState();
    }

    public State stateSelectionEmpty() {
      return stSelectionEmpty.getLinkedState();
    }
  }
}
