/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import javax.swing.DefaultListSelectionModel;
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
public abstract class AbstractFilteredTableModel<T> extends AbstractTableModel
        implements FilterCriteria<T>, Refreshable {

  private final Event evtFilteringStarted = new Event();
  private final Event evtFilteringDone = new Event();
  private final Event evtTableDataChanged = new Event();
  private final Event evtColumnHidden = new Event();
  private final Event evtColumnShown = new Event();

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
   * true while the model data is being filtered
   */
  private boolean isFiltering = false;

  /**
   * true while the model data is being sorted
   */
  private boolean isSorting = false;

  private final String mapTypeID;

  public AbstractFilteredTableModel(final String mapTypeID) {
    this.mapTypeID = mapTypeID;
    this.tableSorter = new TableSorter(this);
    this.columnModel = initializeColumnModel(mapTypeID);
    this.columnIndexCache = new int[columnModel.getColumnCount()];
    bindEventsInternal();
  }

  public String getMapTypeID() {
    return mapTypeID;
  }

  public List<T> getVisibleItems() {
    return new ArrayList<T>(visibleItems);
  }

  public List<T> getHiddenItems() {
    return new ArrayList<T>(hiddenItems);
  }

  /**
   * @return the number of currently visible items
   */
  public int getVisibleItemCount() {
    return visibleItems.size();
  }

  /**
   * @return the number of currently filtered (hidden) items
   */
  public int getHiddenItemCount() {
    return hiddenItems.size();
  }

  /** {@inheritDoc} */
  public int getColumnCount() {
    return getColumnModel().getColumnCount();
  }

  /** {@inheritDoc} */
  public int getRowCount() {
    return visibleItems.size();
  }

  public boolean isFiltering() {
    return isFiltering;
  }

  public boolean isSorting() {
    return isSorting;
  }

  /**
   * @param item the object to search for
   * @param includeHidden set to true if the search should include hidden entities
   * @return true if this table model contains the given object
   */
  public boolean contains(final T item, final boolean includeHidden) {
    final boolean ret = viewIndexOf(item) >= 0;
    if (!ret && includeHidden)
      return hiddenItems.indexOf(item) >= 0;

    return ret;
  }

  /**
   * Returns a Point denoting the row and column index of the first value to fulfill
   * a search criteria based on <code>searchText</code>.
   * @param startRowIndex the row index to start searching at
   * @param forward if true then the search is forward, backwards otherwise
   * @param searchText the search text
   * @return the search result coordinate, null if nothing was found
   * @see #getSearchCriteria(String)
   */
  public Point findNextItemCoordinate(final int startRowIndex, final boolean forward, final String searchText) {
    return findNextItemCoordinate(startRowIndex, forward, getSearchCriteria(searchText));
  }

  /**
   * Returns a Point denoting the row (point.y) and column index (point.x) of the first value to fulfill
   * the given search criteria.
   * @param startRowIndex the row index to start searching at, if this is larger than the size of
   * the table model or less than 0 the search starts from either 0 or rowCount - 1 depending on search direction.
   * @param forward if true then the search is forward, backwards otherwise
   * @param criteria the search criteria
   * @return the search result coordinate, null if nothing was found
   * @see FilterCriteria#include(Object)
   */
  public Point findNextItemCoordinate(final int startRowIndex, final boolean forward, final FilterCriteria<Object> criteria) {
    if (forward) {
      int row = startRowIndex;
      if (row >= getRowCount())
        row = 0;//wrap around
      for (; row < getRowCount(); row++) {
        for (int column = 0; column < getColumnCount(); column++) {
          if (criteria.include(getSearchValueAt(row, column)))
            return new Point(column, row);
        }
      }
    }
    else {
      int row = startRowIndex;
      if (row < 0)
        row = getRowCount() - 1;//wrap around
      for (; row >= 0; row--) {
        for (int j = 0; j < getColumnCount(); j++) {
          if (criteria.include(getSearchValueAt(row, j)))
            return new Point(j, row);
        }
      }
    }

    return null;
  }

  /**
   * Clears all entities from this TableModel
   */
  public void clear() {
    hiddenItems.clear();
    final int size = getRowCount();
    if (size > 0) {
      visibleItems.clear();
      fireTableRowsDeleted(0, size - 1);
    }
  }

  /**
   * @return the TableSorter used by this TableModel
   */
  public TableSorter getTableSorter() {
    return tableSorter;
  }

  /**
   * @return the ListSelectionModel this TableModel uses
   */
  public DefaultListSelectionModel getSelectionModel() {
    return selectionModel;
  }

  /**
   * @param columnIndex the index of the column to sort by
   * @param status the sorting status, use TableSorter.DESCENDING, .NOT_SORTED, .ASCENDING
   */
  public void setSortingStatus(final int columnIndex, final int status) {
    if (columnIndex == -1)
      throw new RuntimeException("Column index can not be negative");

    tableSorter.setSortingStatus(columnIndex, status);
  }

  /**
   * Selects all visible entities
   * @see #eventSelectionChanged()
   */
  public void selectAll() {
    getSelectionModel().setSelectionInterval(0, getVisibleItemCount() - 1);
  }

  /**
   * Clears the selection
   * @see #eventSelectionChanged()
   */
  public void clearSelection() {
    getSelectionModel().clearSelection();
  }

  public Collection<Integer> getSelectedViewIndexes() {
    final List<Integer> indexes = new ArrayList<Integer>();
    final int min = selectionModel.getMinSelectionIndex();
    final int max = selectionModel.getMaxSelectionIndex();
    for (int i = min; i <= max; i++)
      if (selectionModel.isSelectedIndex(i))
        indexes.add(i);

    return indexes;
  }

  public Collection<Integer> getSelectedModelIndexes() {
    final Collection<Integer> indexes = new ArrayList<Integer>();
    final int min = selectionModel.getMinSelectionIndex();
    final int max = selectionModel.getMaxSelectionIndex();
    if (min >= 0 && max >= 0) {
      for (int i = min; i <= max; i++)
        if (selectionModel.isSelectedIndex(i))
          indexes.add(tableSorter.modelIndex(i));
    }

    return indexes;
  }

  /**
   * Moves all selected indexes up one index, wraps around
   * @see #eventSelectionChanged()
   */
  public void moveSelectionUp() {
    if (visibleItems.size() > 0) {
      if (getSelectionModel().isSelectionEmpty())
        getSelectionModel().setSelectionInterval(visibleItems.size() - 1, visibleItems.size() - 1);
      else {
        final Collection<Integer> selected = getSelectedViewIndexes();
        final List<Integer> newSelected = new ArrayList<Integer>(selected.size());
        for (final Integer index : selected)
          newSelected.add(index == 0 ? visibleItems.size() - 1 : index - 1);

        setSelectedItemIndexes(newSelected);
      }
    }
  }

  /**
   * Moves all selected indexes down one index, wraps around
   * @see #eventSelectionChanged()
   */
  public void moveSelectionDown() {
    if (visibleItems.size() > 0) {
      if (getSelectionModel().isSelectionEmpty())
        getSelectionModel().setSelectionInterval(0,0);
      else {
        final Collection<Integer> selected = getSelectedViewIndexes();
        final List<Integer> newSelected = new ArrayList<Integer>(selected.size());
        for (final Integer index : selected)
          newSelected.add(index == visibleItems.size() - 1 ? 0 : index + 1);

        setSelectedItemIndexes(newSelected);
      }
    }
  }

  /**
   * Clears the selection and selects the item at <code>index</code>
   * @param index the index
   */
  public void setSelectedItemIndex(final int index) {
    selectionModel.setSelectionInterval(index, index);
  }

  /**
   * Selects the item at <code>index</code>
   * @param index the index
   */
  public void addSelectedItemIndex(final int index) {
    selectionModel.addSelectionInterval(index, index);
  }

  /**
   * Selects the given indexes
   * @param indexes the indexes to select
   */
  public void setSelectedItemIndexes(final List<Integer> indexes) {
    selectionModel.clearSelection();
    selectionModel.addSelectedItemIndexes(indexes);
  }

  /**
   * @return a list containing the selected items
   */
  public List<T> getSelectedItems() {
    final Collection<Integer> selectedModelIndexes = getSelectedModelIndexes();
    final List<T> selectedItems = new ArrayList<T>();
    for (final int modelIndex : selectedModelIndexes)
      selectedItems.add(visibleItems.get(modelIndex));

    return selectedItems;
  }

  /**
   * Selects the given items
   * @param items the items to select
   */
  public void setSelectedItems(final List<T> items) {
    final List<Integer> indexes = new ArrayList<Integer>();
    for (final T item : items) {
      final int index = viewIndexOf(item);
      if (index >= 0)
        indexes.add(index);
    }

    setSelectedItemIndexes(indexes);
  }

  /**
   * @return the selected item, null if none is selected
   */
  public T getSelectedItem() {
    final int index = getSelectionModel().getMinSelectionIndex();
    if (index >= 0 && index < getVisibleItemCount())
      return getItemAtViewIndex(index);
    else
      return null;
  }

  /**
   * Sets the selected item
   * @param item the item to select
   */
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

  /**
   * @return the index of the selected record, -1 if none is selected and
   * the lowest index if more than one record is selected
   */
  public int getSelectedIndex() {
    return selectionModel.getMinSelectedIndex();
  }

  /**
   * @param index the index
   * @return the item at <code>index</code>
   */
  public T getItemAtViewIndex(final int index) {
    if (index >= 0 && index < visibleItems.size())
      return visibleItems.get(tableSorter.modelIndex(index));

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
  public void filterTable() {
    try {
      isFiltering = true;
      evtFilteringStarted.fire();
      final List<T> selectedItems = getSelectedItems();
      visibleItems.addAll(hiddenItems);
      hiddenItems.clear();
      for (final ListIterator<T> iterator = visibleItems.listIterator(); iterator.hasNext();) {
        final T item = iterator.next();
        if (!include(item)) {
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

  /**
   * @return all visible and hidden items in this table model
   */
  public List<T> getAllItems() {
    return getAllItems(true);
  }

  /**
   * @param includeHidden if true then filtered items are included
   * @return all items in this table model
   */
  public List<T> getAllItems(final boolean includeHidden) {
    final List<T> entities = new ArrayList<T>(visibleItems);
    if (includeHidden)
      entities.addAll(hiddenItems);

    return entities;
  }

  /**
   * Removes the given items from this table model
   * @param items the items to remove from the model
   */
  public void removeItems(final List<T> items) {
    for (final T item : items)
      removeItem(item);
  }

  /**
   * Removes the given item from this table model
   * @param item the item to remove from the model
   */
  public void removeItem(final T item) {
    int index = indexOf(item);
    if (index >= 0) {
      visibleItems.remove(index);
      fireTableRowsDeleted(index, index);
    }
    else {
      index = hiddenItems.indexOf(item);
      if (index >= 0)
        hiddenItems.remove(index);
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

  /**
   * Toggles the visibility of the column representing the given columnIdentifier.<br>
   * @param columnIdentifier the column identifier
   * @param visible if true the column is shown, otherwise it is hidden
   */
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
        getColumnModel().addColumn(hiddenColumn);
        evtColumnShown.fire(new ActionEvent(hiddenColumn.getIdentifier(), 0, "showColumn"));
      }
    }
  }

  public void hideColumn(final Object columnIdentifier) {
    final TableColumn column = getTableColumn(columnIdentifier);
    getColumnModel().removeColumn(column);
    hiddenColumns.add(column);
    evtColumnHidden.fire(new ActionEvent(columnIdentifier, 0, "hideColumn"));
  }

  /**
   * @param columnIdentifier the key for which to query if its column is visible
   * @return true if the column is visible, false if it is hidden
   */
  public boolean isColumnVisible(final Object columnIdentifier) {
    for (final TableColumn column : hiddenColumns) {
      if (column.getIdentifier().equals(columnIdentifier))
        return false;
    }

    return true;
  }

  public List<TableColumn> getHiddenColumns() {
    return Collections.unmodifiableList(hiddenColumns);
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

  /**
   * @return a State active when the selection is empty
   */
  public State stateSelectionEmpty() {
    return selectionModel.stSelectionEmpty;
  }

  /**
   * @return an Event fired when the model has been filtered
   */
  public Event eventFilteringDone() {
    return evtFilteringDone;
  }

  /**
   * @return an Event fired when the model is about to be filtered
   */
  public Event eventFilteringStarted() {
    return evtFilteringStarted;
  }

  /**
   * @return an event fired when the minimum (topmost) selected index changes (minSelectionIndex property in ListSelectionModel)
   */
  public Event eventSelectedIndexChanged() {
    return selectionModel.evtSelectedIndexChanged;
  }

  /**
   * @return an Event fired after the selection has changed
   */
  public Event eventSelectionChanged() {
    return selectionModel.evtSelectionChanged;
  }

  /**
   * @return an Event fired when the selection is changing
   */
  public Event eventSelectionChangedAdjusting() {
    return selectionModel.evtSelectionChangedAdjusting;
  }

  /**
   * @return an Event fired after the table data has changed
   */
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
    if (modelColumnIndex < 0)
      return modelColumnIndex;

    final int cachedIndex = columnIndexCache[modelColumnIndex];
    if (cachedIndex > 0)
      return cachedIndex;

    for (int index = 0; index < getColumnCount(); index++)
      if (columnModel.getColumn(index).getModelIndex() == modelColumnIndex) {
        columnIndexCache[modelColumnIndex] = index;
        return index;
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
      if (include(item)) {
        if (atFront)
          visibleItems.add(0, item);
        else
          visibleItems.add(item);
      }
      else
        hiddenItems.add(item);
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
    return new FilterCriteria<Object>() {
      public boolean include(final Object item) {
        return !(item == null || searchText == null) &&
                item.toString().toLowerCase().contains(searchText.toLowerCase());
      }
    };
  }

  protected abstract TableColumnModel initializeColumnModel(final String tableIdentifier);

  private void bindEventsInternal() {
    final List<T> selectedItems = new ArrayList<T>();
    addTableModelListener(new TableModelListener() {
      public void tableChanged(TableModelEvent event) {
        evtTableDataChanged.fire();
      }
    });
    tableSorter.eventBeforeSort().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent event) {
        isSorting = true;
        selectedItems.clear();
        selectedItems.addAll(getSelectedItems());
      }
    });
    tableSorter.eventAfterSort().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent event) {
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

    final Event evtSelectionChangedAdjusting = new Event();
    final Event evtSelectionChanged = new Event();
    final Event evtSelectedIndexChanged = new Event();
    final State stSelectionEmpty = new State(true);

    /**
     * true while the selection is being updated
     */
    private boolean isUpdatingSelection = false;
    /**
     * Holds the topmost (minimum) selected index
     */
    private int minSelectedIndex = -1;

    @Override
    public void fireValueChanged(int min, int max, boolean isAdjusting) {
      super.fireValueChanged(min, max, isAdjusting);
      stSelectionEmpty.setActive(isSelectionEmpty());
      final int minSelIndex = getMinSelectionIndex();
      if (minSelectedIndex != minSelIndex) {
        minSelectedIndex = minSelIndex;
        evtSelectedIndexChanged.fire();
      }
      if (isAdjusting || isUpdatingSelection || isSorting)
        evtSelectionChangedAdjusting.fire();
      else
        evtSelectionChanged.fire();
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

    public int getMinSelectedIndex() {
      return minSelectedIndex;
    }
  }
}
