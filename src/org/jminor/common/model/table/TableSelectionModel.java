/*
 * Copyright (c) 2004 - 2013, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.table;

import org.jminor.common.model.EventListener;
import org.jminor.common.model.StateObserver;

import javax.swing.ListSelectionModel;
import java.util.Collection;
import java.util.List;

/**
 * A table selection model
 * @param <R> the type of rows in the table model
 */
public interface TableSelectionModel<R> extends ListSelectionModel {

  /**
   * @return a StateObserver indicating that the selection is empty
   */
  StateObserver getSelectionEmptyObserver();

  /**
   * @return a StateObserver indicating that multiple rows are selected
   */
  StateObserver getMultipleSelectionObserver();

  /**
   * @return a StateObserver indicating that a single row is selected
   */
  StateObserver getSingleSelectionObserver();

  /**
   * @param listener a listener to be notified each time the selection changes
   */
  void addSelectionChangedListener(final EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeSelectionChangedListener(final EventListener listener);

  /**
   * @param listener a listener to be notified each time the selected index changes
   */
  void addSelectedIndexListener(final EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeSelectedIndexListener(final EventListener listener);

  /**
   * Moves all selected indexes down one index, wraps around.
   * If the selection is empty the first item in this model is selected.
   * @see #addSelectionChangedListener(org.jminor.common.model.EventListener)
   */
  void moveSelectionDown();

  /**
   * Moves all selected indexes up one index, wraps around.
   * If the selection is empty the last item in this model is selected.
   * @see #addSelectionChangedListener(org.jminor.common.model.EventListener)
   */
  void moveSelectionUp();

  /**
   * @return true if no rows are selected in this table model
   */
  boolean isSelectionEmpty();

  /**
   * @return the index of the selected record, -1 if none is selected and
   * the lowest index if more than one record is selected
   */
  int getSelectedIndex();

  /**
   * Selects the item at <code>index</code>
   * @param index the index
   */
  void addSelectedIndex(final int index);

  /**
   * Clears the selection and selects the item at <code>index</code>
   * @param index the index
   */
  void setSelectedIndex(final int index);

  /**
   * Clears the selection
   * @see #addSelectionChangedListener(EventListener)
   */
  void clearSelection();

  /**
   * Selects the given indexes
   * @param indexes the indexes to select
   */
  void setSelectedIndexes(final Collection<Integer> indexes);

  /**
   * @return the selected indexes, an empty list if selection is empty
   */
  List<Integer> getSelectedIndexes();

  /**
   * Selects all visible entities
   * @see #addSelectionChangedListener(EventListener)
   */
  void selectAll();

  /**
   * Adds these indexes to the selection
   * @param indexes the indexes to add to the selection
   */
  void addSelectedIndexes(final Collection<Integer> indexes);

  /**
   * @return the number of selected indexes in the underlying selection model.
   */
  int getSelectionCount();
  /**
   * Selects the given items
   * @param items the items to select
   */
  void setSelectedItems(final Collection<R> items);

  /**
   * @return a list containing the selected items
   */
  List<R> getSelectedItems();

  /**
   * @return the selected item, null if none is selected
   */
  R getSelectedItem();

  /**
   * Sets the selected item
   * @param item the item to select
   */
  void setSelectedItem(final R item);

  /**
   * Adds the given item to the selection
   * @param item the item to add to the selection
   */
  void addSelectedItem(final R item);

  /**
   * Adds the given items to the selection
   * @param items the items to add to the selection
   */
  void addSelectedItems(final Collection<R> items);

  /**
   * A proxy for connecting to a table model
   * @param <R> the row type
   */
  interface TableModelProxy<R> {
    /**
     * @return the size of the table model
     */
    int getSize();

    /**
     * @param item the item
     * @return the index of the item in the table model
     */
    int indexOf(final R item);

    /**
     * @param index the index
     * @return the item at the given index in the table model
     */
    R getItemAt(final int index);
  }
}
