/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.table;

import is.codion.common.state.State;
import is.codion.common.state.StateObserver;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A table selection model
 * @param <R> the type of rows in the table model
 */
public interface TableSelectionModel<R> {

  /**
   * @return a StateObserver indicating whether the selection is empty
   */
  StateObserver selectionEmpty();

  /**
   * @return a StateObserver indicating whether one or more items are selected
   */
  StateObserver selectionNotEmpty();

  /**
   * @return a StateObserver indicating whether multiple rows are selected
   */
  StateObserver multipleSelection();

  /**
   * @return a StateObserver indicating whether a single row is selected
   */
  StateObserver singleSelection();

  /**
   * @return a State controlling the single selection mode of this selection model
   */
  State singleSelectionMode();

  /**
   * To prevent a selection change, add a listener throwing a {@link is.codion.common.model.CancelException}.
   * @param listener a listener to be notified before the selection changes
   */
  void addBeforeSelectionChangeListener(Runnable listener);

  /**
   * @param listener the listener to remove
   */
  void removeBeforeSelectionChangeListener(Runnable listener);

  /**
   * @param listener a listener to be notified each time the selection changes
   */
  void addSelectionListener(Runnable listener);

  /**
   * @param listener the listener to remove
   */
  void removeSelectionListener(Runnable listener);

  /**
   * @param listener a listener to be notified each time the selected index changes
   */
  void addSelectedIndexListener(Consumer<Integer> listener);

  /**
   * @param listener the listener to remove
   */
  void removeSelectedIndexListener(Consumer<Integer> listener);

  /**
   * @param listener a listener to be notified each time the selected indexes change
   */
  void addSelectedIndexesListener(Consumer<List<Integer>> listener);

  /**
   * @param listener the listener to remove
   */
  void removeSelectedIndexesListener(Consumer<List<Integer>> listener);

  /**
   * @param listener a listener to be notified each time the selected item changes
   */
  void addSelectedItemListener(Consumer<R> listener);

  /**
   * @param listener the listener to remove
   */
  void removeSelectedItemListener(Consumer<R> listener);

  /**
   * @param listener a listener to be notified each time the selected items change
   */
  void addSelectedItemsListener(Consumer<List<R>> listener);

  /**
   * @param listener the listener to remove
   */
  void removeSelectedItemsListener(Consumer<List<R>> listener);

  /**
   * Moves all selected indexes down one index, wraps around.
   * If the selection is empty the first item in this model is selected.
   * @see #addSelectionListener(Runnable)
   */
  void moveSelectionDown();

  /**
   * Moves all selected indexes up one index, wraps around.
   * If the selection is empty the last item in this model is selected.
   * @see #addSelectionListener(Runnable)
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
   * @see #addSelectionListener(Runnable)
   */
  void selectAll();

  /**
   * Sets the items passing the predicate test as the selection
   * @param predicate the predicate
   */
  void setSelectedItems(Predicate<R> predicate);

  /**
   * Adds the items passing the predicate test to the selection
   * @param predicate the predicate
   */
  void addSelectedItems(Predicate<R> predicate);

  /**
   * Adds these indexes to the selection
   * @param indexes the indexes to add to the selection
   */
  void addSelectedIndexes(Collection<Integer> indexes);

  /**
   * @return the number of selected indexes in the underlying selection model.
   */
  int selectionCount();

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
   * @param item the item
   * @return true if the item is selected
   */
  boolean isSelectedItem(R item);

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
   * @return the selected item, or an empty Optional in case the selection is empty
   */
  Optional<R> selectedItem();

  /**
   * Clears the selection
   */
  void clearSelection();

  /**
   * @return true if the selection is empty
   */
  boolean isSelectionEmpty();

  /**
   * @return true if the selection is not empty
   */
  boolean isSelectionNotEmpty();
}
