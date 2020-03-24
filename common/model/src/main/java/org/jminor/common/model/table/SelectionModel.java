/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.table;

import org.jminor.common.event.EventDataListener;
import org.jminor.common.event.EventListener;
import org.jminor.common.state.State;
import org.jminor.common.state.StateObserver;

import java.util.Collection;
import java.util.List;

/**
 * A table selection model
 * @param <R> the type of rows in the table model
 */
public interface SelectionModel<R> {

  /**
   * @return a StateObserver indicating that the selection is empty
   */
  StateObserver getSelectionEmptyObserver();

  /**
   * @return a StateObserver indicating that one or more items are selected
   */
  StateObserver getSelectionNotEmptyObserver();

  /**
   * @return a StateObserver indicating that multiple rows are selected
   */
  StateObserver getMultipleSelectionObserver();

  /**
   * @return a StateObserver indicating that a single row is selected
   */
  StateObserver getSingleSelectionObserver();

  /**
   * @return a State controlling the single selection mode of this selection model
   */
  State getSingleSelectionModeState();

  /**
   * @param listener a listener to be notified each time the selection changes
   */
  void addSelectionChangedListener(EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeSelectionChangedListener(EventListener listener);

  /**
   * @param listener a listener to be notified each time the selected index changes
   */
  void addSelectedIndexListener(EventDataListener<Integer> listener);

  /**
   * @param listener the listener to remove
   */
  void removeSelectedIndexListener(EventDataListener<Integer> listener);

  /**
   * @param listener a listener to be notified each time the selected item changes
   */
  void addSelectedItemListener(EventDataListener<R> listener);

  /**
   * @param listener the listener to remove
   */
  void removeSelectedItemListener(EventDataListener<R> listener);

  /**
   * @param listener a listener to be notified each time the selected items change
   */
  void addSelectedItemsListener(EventDataListener<List<R>> listener);

  /**
   * @param listener the listener to remove
   */
  void removeSelectedItemsListener(EventDataListener<List<R>> listener);

  /**
   * Moves all selected indexes down one index, wraps around.
   * If the selection is empty the first item in this model is selected.
   * @see #addSelectionChangedListener(EventListener)
   */
  void moveSelectionDown();

  /**
   * Moves all selected indexes up one index, wraps around.
   * If the selection is empty the last item in this model is selected.
   * @see #addSelectionChangedListener(EventListener)
   */
  void moveSelectionUp();

  /**
   * @return the index of the selected record, -1 if none is selected and
   * the lowest index if more than one record is selected
   */
  int getSelectedIndex();

  /**
   * Selects the item at {@code index}
   * @param index the index
   */
  void addSelectedIndex(int index);

  /**
   * Removes the item at {@code index} from the selection
   * @param index the index
   */
  void removeSelectedIndex(int index);

  /**
   * Removes the given indexes from the selection
   * @param indexes the indexes
   */
  void removeSelectedIndexes(Collection<Integer> indexes);

  /**
   * Clears the selection and selects the item at {@code index}
   * @param index the index
   */
  void setSelectedIndex(int index);

  /**
   * Selects the given indexes
   * @param indexes the indexes to select
   */
  void setSelectedIndexes(Collection<Integer> indexes);

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
  void addSelectedIndexes(Collection<Integer> indexes);

  /**
   * @return the number of selected indexes in the underlying selection model.
   */
  int getSelectionCount();

  /**
   * Selects the given items
   * @param items the items to select
   */
  void setSelectedItems(Collection<R> items);

  /**
   * @return a list containing the selected items
   */
  List<R> getSelectedItems();

  /**
   * @return the item at the lowest selected index, null if none is selected
   */
  R getSelectedItem();

  /**
   * Sets the selected item
   * @param item the item to select
   */
  void setSelectedItem(R item);

  /**
   * Adds the given item to the selection
   * @param item the item to add to the selection
   */
  void addSelectedItem(R item);

  /**
   * Adds the given items to the selection
   * @param items the items to add to the selection
   */
  void addSelectedItems(Collection<R> items);

  /**
   * Remove the given item from the selection
   * @param item the item to remove from the selection
   */
  void removeSelectedItem(R item);

  /**
   * Remove the given items from the selection
   * @param items the items to remove from the selection
   */
  void removeSelectedItems(Collection<R> items);

  /**
   * Clears the selection
   */
  void clearSelection();

  /**
   * @return true if the selection is empty
   */
  boolean isSelectionEmpty();
}
