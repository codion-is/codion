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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

/**
 * A TableModel implentation that supports filtering, searching and sorting.
 * <pre>
 * AbstractFilteredTableModel tableModel = ...;
 * JTable table = new JTable(tableModel.getTableSorter(),
 *                           tableModel.getColumnModel(),
 *                           tableModel.getSelectionModel());
 * </pre><br>
 * User: Björn Darri<br>
 * Date: 18.4.2010<br>
 * Time: 09:48:07<br>
 * @param <T> the type of the values in this table model
 */
public abstract class AbstractFilteredTableModel<T> extends AbstractTableModel implements FilteredTableModel<T> {

  private final Event evtFilteringStarted = new Event();
  private final Event evtFilteringDone = new Event();
  private final Event evtTableDataChanged = new Event();
  private final Event evtColumnHidden = new Event();
  private final Event evtColumnShown = new Event();

  private final FilterCriteria<T> acceptAllCriteria = new FilterCriteria.AcceptAllCriteria<T>();

  /**
   * Holds visible items
   */
  private final List<T> visibleItems = new ArrayList<T>();

  /**
   * Holds items that are hidden
   */
  private final List<T> hiddenItems = new ArrayList<T>();

  /**
   * The TableColumnModel
   */
  private final TableColumnModel columnModel;

  /**
   * Contains columns that have been hidden
   */
  private final List<TableColumn> hiddenColumns = new ArrayList<TableColumn>();

  /**
   * The sorter model
   */
  private final TableSorter tableSorter;

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
  private FilterCriteria<T> filterCriteria = acceptAllCriteria;

  /**
   * true while the model data is being filtered
   */
  private boolean isFiltering = false;

  /**
   * true while the model data is being sorted
   */
  private boolean isSorting = false;

  /**
   * true if searching the table model should be done via regular expressions
   */
  private boolean regularExpressionSearch = false;

  public AbstractFilteredTableModel(final TableColumnModel columnModel) {
    this.tableSorter = new TableSorter(this);
    this.columnModel = columnModel;
    this.columnIndexCache = new int[columnModel.getColumnCount()];
    bindEventsInternal();
  }

  public List<T> getVisibleItems() {
    return Collections.unmodifiableList(visibleItems);
  }

  public List<T> getHiddenItems() {
    return Collections.unmodifiableList(hiddenItems);
  }

  public int getVisibleItemCount() {
    return visibleItems.size();
  }

  public int getHiddenItemCount() {
    return hiddenItems.size();
  }

  public int getColumnCount() {
    return columnModel.getColumnCount();
  }

  public int getRowCount() {
    return visibleItems.size();
  }

  public boolean isFiltering() {
    return isFiltering;
  }

  public boolean isSorting() {
    return isSorting;
  }

  public boolean contains(final T item, final boolean includeHidden) {
    final boolean ret = visibleItems.contains(item);
    if (!ret && includeHidden) {
      return hiddenItems.contains(item);
    }

    return ret;
  }

  public Point findNextItemCoordinate(final int fromIndex, final boolean forward, final String searchText) {
    return findNextItemCoordinate(fromIndex, forward, getSearchCriteria(searchText));
  }

  public Point findNextItemCoordinate(final int fromIndex, final boolean forward, final FilterCriteria<Object> criteria) {
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

  public void clear() {
    hiddenItems.clear();
    final int size = getRowCount();
    if (size > 0) {
      visibleItems.clear();
      fireTableRowsDeleted(0, size - 1);
    }
  }

  public TableSorter getTableSorter() {
    return tableSorter;
  }

  public ListSelectionModel getSelectionModel() {
    return selectionModel;
  }

  public void setSortingStatus(final int columnIndex, final int status) {
    if (columnIndex == -1) {
      throw new RuntimeException("Column index can not be negative");
    }

    tableSorter.setSortingStatus(columnIndex, status);
  }

  public boolean isRegularExpressionSearch() {
    return regularExpressionSearch;
  }

  public void setRegularExpressionSearch(final boolean value) {
    this.regularExpressionSearch = value;
  }

  public void selectAll() {
    getSelectionModel().setSelectionInterval(0, getVisibleItemCount() - 1);
  }

  public void clearSelection() {
    getSelectionModel().clearSelection();
  }

  public Collection<Integer> getSelectedViewIndexes() {
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

  public Collection<Integer> getSelectedModelIndexes() {
    final Collection<Integer> indexes = new ArrayList<Integer>();
    final int min = selectionModel.getMinSelectionIndex();
    final int max = selectionModel.getMaxSelectionIndex();
    if (min >= 0 && max >= 0) {
      for (int i = min; i <= max; i++) {
        if (selectionModel.isSelectedIndex(i)) {
          indexes.add(tableSorter.modelIndex(i));
        }
      }
    }

    return indexes;
  }

  public void moveSelectionUp() {
    if (visibleItems.size() > 0) {
      if (getSelectionModel().isSelectionEmpty()) {
        getSelectionModel().setSelectionInterval(visibleItems.size() - 1, visibleItems.size() - 1);
      }
      else {
        final Collection<Integer> selected = getSelectedViewIndexes();
        final List<Integer> newSelected = new ArrayList<Integer>(selected.size());
        for (final Integer index : selected) {
          newSelected.add(index == 0 ? visibleItems.size() - 1 : index - 1);
        }

        setSelectedItemIndexes(newSelected);
      }
    }
  }

  public void moveSelectionDown() {
    if (visibleItems.size() > 0) {
      if (getSelectionModel().isSelectionEmpty()) {
        getSelectionModel().setSelectionInterval(0, 0);
      }
      else {
        final Collection<Integer> selected = getSelectedViewIndexes();
        final List<Integer> newSelected = new ArrayList<Integer>(selected.size());
        for (final Integer index : selected) {
          newSelected.add(index == visibleItems.size() - 1 ? 0 : index + 1);
        }

        setSelectedItemIndexes(newSelected);
      }
    }
  }

  public void setSelectedItemIndex(final int index) {
    selectionModel.setSelectionInterval(index, index);
  }

  public void addSelectedItemIndex(final int index) {
    selectionModel.addSelectionInterval(index, index);
  }

  public void setSelectedItemIndexes(final List<Integer> indexes) {
    selectionModel.clearSelection();
    selectionModel.addSelectedItemIndexes(indexes);
  }

  public List<T> getSelectedItems() {
    final Collection<Integer> selectedModelIndexes = getSelectedModelIndexes();
    final List<T> selectedItems = new ArrayList<T>();
    for (final int modelIndex : selectedModelIndexes) {
      selectedItems.add(visibleItems.get(modelIndex));
    }

    return selectedItems;
  }

  public void setSelectedItems(final List<T> items) {
    final List<Integer> indexes = new ArrayList<Integer>();
    for (final T item : items) {
      final int index = viewIndexOf(item);
      if (index >= 0) {
        indexes.add(index);
      }
    }

    setSelectedItemIndexes(indexes);
  }

  public T getSelectedItem() {
    final int index = selectionModel.getSelectedIndex();
    if (index >= 0 && index < getVisibleItemCount()) {
      return getItemAtViewIndex(index);
    }
    else {
      return null;
    }
  }

  public void setSelectedItem(final T item) {
    setSelectedItems(Arrays.asList(item));
  }

  /**
   * Adds these indexes to the selection
   * @param indexes the indexes to add to the selection
   */
  public void addSelectedItemIndexes(final List<Integer> indexes) {
    selectionModel.addSelectedItemIndexes(indexes);
  }

  public int getSelectedIndex() {
    return selectionModel.getSelectedIndex();
  }

  public int getSelectionCount() {
    return selectionModel.getSelectionCount();
  }

  public boolean isSelectionEmpty() {
    return selectionModel.isSelectionEmpty();
  }

  public ListSelectionModel getSelectionModel2() {
    return selectionModel;
  }

  public T getItemAtViewIndex(final int index) {
    if (index >= 0 && index < visibleItems.size()) {
      return visibleItems.get(tableSorter.modelIndex(index));
    }

    throw new ArrayIndexOutOfBoundsException("No visible item found at index: " + index + ", size: " + getVisibleItemCount());
  }

  public T getItemAt(final int index) {
    return visibleItems.get(index);
  }

  public int indexOf(final T item) {
    return visibleItems.indexOf(item);
  }

  /**
   * Filters this table model
   * @see #eventFilteringStarted()
   * @see #eventFilteringDone()
   */
  public void filterContents() {
    try {
      isFiltering = true;
      evtFilteringStarted.fire();
      final List<T> selectedItems = getSelectedItems();
      visibleItems.addAll(hiddenItems);
      hiddenItems.clear();
      for (final ListIterator<T> iterator = visibleItems.listIterator(); iterator.hasNext();) {
        final T item = iterator.next();
        if (!getFilterCriteria().include(item)) {
          hiddenItems.add(item);
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

  public FilterCriteria<T> getFilterCriteria() {
    return filterCriteria;
  }

  public void setFilterCriteria(final FilterCriteria filterCriteria) {
    if (filterCriteria == null) {
      this.filterCriteria = acceptAllCriteria;
    }
    else {
      this.filterCriteria = filterCriteria;
    }
    filterContents();
  }

  public List<T> getAllItems() {
    return getAllItems(true);
  }

  public List<T> getAllItems(final boolean includeHidden) {
    final List<T> entities = new ArrayList<T>(visibleItems);
    if (includeHidden) {
      entities.addAll(hiddenItems);
    }

    return entities;
  }

  /**
   * Removes the given items from this table model
   * @param items the items to remove from the model
   */
  public void removeItems(final List<T> items) {
    for (final T item : items) {
      removeItem(item);
    }
  }

  /**
   * Removes the given item from this table model
   * @param item the item to remove from the model
   */
  public void removeItem(final T item) {
    if (visibleItems.contains(item)) {
      final int index = indexOf(item);
      visibleItems.remove(item);
      fireTableRowsDeleted(index, index);
    }
    else {
      if (hiddenItems.contains(item)) {
        hiddenItems.remove(item);
      }
    }
  }

  public TableColumnModel getColumnModel() {
    return columnModel;
  }

  /**
   * Returns the TableColumn for the given identifier
   * @param identifier the identifier for which to retrieve the column
   * @return the TableColumn associated with the given identifier
   */
  public TableColumn getTableColumn(final Object identifier) {
    return columnModel.getColumn(columnModel.getColumnIndex(identifier));
  }

  public void setColumnVisible(final Object columnIdentifier, final boolean visible) {
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

  public void showColumn(final Object columnIdentifier) {
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

  public void hideColumn(final Object columnIdentifier) {
    final TableColumn column = getTableColumn(columnIdentifier);
    columnModel.removeColumn(column);
    hiddenColumns.add(column);
    evtColumnHidden.fire(new ActionEvent(columnIdentifier, 0, "hideColumn"));
  }

  public boolean isColumnVisible(final Object columnIdentifier) {
    for (final TableColumn column : hiddenColumns) {
      if (column.getIdentifier().equals(columnIdentifier)) {
        return false;
      }
    }

    return true;
  }

  public List<TableColumn> getHiddenColumns() {
    return Collections.unmodifiableList(hiddenColumns);
  }

  public State stateSelectionEmpty() {
    return selectionModel.stSelectionEmpty.getLinkedState();
  }

  /**
   * @return a State active when multiple rows are selected
   */
  public State stateMultipleSelection() {
    return selectionModel.stMultipleSelection.getLinkedState();
  }

  /**
   * @return an Event fired whenever a column is hidden,
   * the ActionEvent source is the column identifier.
   */
  public Event eventColumnHidden() {
    return evtColumnHidden;
  }

  /**
   * @return an Event fired whenever a column is shown,
   * the ActionEvent source is the column identifier.
   */
  public Event eventColumnShown() {
    return evtColumnShown;
  }

  public Event eventFilteringDone() {
    return evtFilteringDone;
  }

  public Event eventFilteringStarted() {
    return evtFilteringStarted;
  }

  public Event eventSelectedIndexChanged() {
    return selectionModel.evtSelectedIndexChanged;
  }

  public Event eventSelectionChanged() {
    return selectionModel.evtSelectionChanged;
  }

  /**
   * @return an Event fired when the selection is changing
   */
  public Event eventSelectionChangedAdjusting() {
    return selectionModel.evtSelectionChangedAdjusting;
  }

  public Event eventTableDataChanged() {
    return evtTableDataChanged;
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
  protected int convertColumnIndexToView(final int modelColumnIndex) {
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
  protected void addItems(final List<T> items, final boolean atFront) {
    for (final T item : items) {
      if (getFilterCriteria().include(item)) {
        if (atFront) {
          visibleItems.add(0, item);
        }
        else {
          visibleItems.add(item);
        }
      }
      else {
        hiddenItems.add(item);
      }
    }
    fireTableDataChanged();
  }

  protected int modelIndexOf(final T item) {
    return visibleItems.indexOf(item);
  }

  protected int viewIndexOf(final T item) {
    return tableSorter.viewIndex(modelIndexOf(item));
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

  protected FilterCriteria<Object> getSearchCriteria(final String searchText) {
    if (regularExpressionSearch) {
      return new RegexFilterCriteria<Object>(searchText);
    }

    return new FilterCriteria<Object>() {
      public boolean include(final Object item) {
        if (item == null || searchText == null) {
          return false;
        }

        return item.toString().toLowerCase().contains(searchText.toLowerCase());
      }
    };
  }

  private void bindEventsInternal() {
    final List<T> selectedItems = new ArrayList<T>();
    addTableModelListener(new TableModelListener() {
      public void tableChanged(TableModelEvent e) {
        evtTableDataChanged.fire();
      }
    });
    tableSorter.eventBeforeSort().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        isSorting = true;
        selectedItems.clear();
        selectedItems.addAll(getSelectedItems());
      }
    });
    tableSorter.eventAfterSort().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        isSorting = false;
        setSelectedItems(selectedItems);
        selectedItems.clear();
      }
    });
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

  class SelectionModel extends DefaultListSelectionModel {

    private final Event evtSelectionChangedAdjusting = new Event();
    private final Event evtSelectionChanged = new Event();
    private final Event evtSelectedIndexChanged = new Event();
    private final State stSelectionEmpty = new State(true);
    private final State stMultipleSelection = new State(false);

    /**
     * true while the selection is being updated
     */
    private boolean isUpdatingSelection = false;
    /**
     * Holds the topmost (minimum) selected index
     */
    private int selectedIndex = -1;

    @Override
    public void fireValueChanged(int firstIndex, int lastIndex, boolean isAdjusting) {
      super.fireValueChanged(firstIndex, lastIndex, isAdjusting);
      stSelectionEmpty.setActive(isSelectionEmpty());
      stMultipleSelection.setActive(getSelectionCount() > 1);
      final int minSelIndex = getMinSelectionIndex();
      if (selectedIndex != minSelIndex) {
        selectedIndex = minSelIndex;
        evtSelectedIndexChanged.fire();
      }
      if (isAdjusting || isUpdatingSelection || isSorting) {
        evtSelectionChangedAdjusting.fire();
      }
      else {
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
        if (indexes.size() > 0) {
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

    public Event eventSelectedIndexChanged() {
      return evtSelectedIndexChanged;
    }

    public Event eventSelectionChanged() {
      return evtSelectionChanged;
    }

    public Event eventSelectionChangedAdjusting() {
      return evtSelectionChangedAdjusting;
    }

    public State stateMultipleSelection() {
      return stMultipleSelection.getLinkedState();
    }

    public State stateSelectionEmpty() {
      return stSelectionEmpty.getLinkedState();
    }
  }
}
