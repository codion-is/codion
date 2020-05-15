/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model;

import is.codion.common.event.EventListener;

import java.util.List;
import java.util.function.Predicate;

/**
 * Specifies a data model that can be filtered to hide some or all of the items it contains.
 * @param <T> the type of data in the model.
 */
public interface FilteredModel<T> {

  /**
   * @param listener a listener notified each time this model is filtered
   */
  void addFilteringListener(EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeFilteringListener(EventListener listener);

  /**
   * Filters the table according to the condition returned by {@link #getIncludeCondition()}.
   * If no include condition is specified this method does nothing.
   * This method does not interfere with the internal ordering of the visible items.
   * @see #getIncludeCondition()
   * @see #setIncludeCondition(Predicate)
   * @see #addFilteringListener(EventListener)
   */
  void filterContents();

  /**
   * Returns the include condition defined by this model, null if no include condition has been set.
   * @return the include condition
   */
  Predicate<T> getIncludeCondition();

  /**
   * Sets the include condition and filters the model
   * @param includeCondition the Predicate to use, null if no filtering should be performed
   */
  void setIncludeCondition(Predicate<T> includeCondition);

  /**
   * @return an unmodifiable view of all visible and filtered items in this table model, in no particular order
   * @see #getVisibleItems()
   * @see #getFilteredItems()
   */
  List<T> getItems();

  /**
   * @return an unmodifiable view of the visible items
   */
  List<T> getVisibleItems();

  /**
   * @return an unmodifiable view of the filtered items
   */
  List<T> getFilteredItems();

  /**
   * @return the number of currently visible items
   */
  int getVisibleItemCount();

  /**
   * @return the number of currently filtered items
   */
  int getFilteredItemCount();

  /**
   * Returns true if this model contains the given item, visible or filtered.
   * @param item the item
   * @return true if this model contains the item
   */
  boolean containsItem(T item);

  /**
   * Returns true if this model contains the given item and it is visible, that is, not filtered
   * @param item the item
   * @return true if the given item is visible
   */
  boolean isVisible(T item);

  /**
   * Returns true if this model contains the given item and it is filtered, that is, is not visible
   * @param item the item
   * @return true if the given item is filtered
   */
  boolean isFiltered(T item);
}
