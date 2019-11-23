/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import org.jminor.common.event.EventListener;

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
  void addFilteringListener(final EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeFilteringListener(final EventListener listener);

  /**
   * Filters the table according to the condition returned by {@code getFilterCondition()}.
   * If no filter condition is specified this method does nothing.
   * This method does not interfere with the internal ordering of the visible items.
   * @see #getFilterCondition()
   * @see #setFilterCondition(Predicate)
   * @see #addFilteringListener(EventListener)
   */
  void filterContents();

  /**
   * Returns the filter condition defined by this model, null if no filter condition has been set.
   * @return the filter condition
   */
  Predicate<T> getFilterCondition();

  /**
   * Sets the filter condition and filters the model
   * @param filterCondition the Predicate to use, null if no filtering should be performed
   */
  void setFilterCondition(final Predicate<T> filterCondition);

  /**
   * @return an unmodifiable view of the visible items
   */
  List<T> getVisibleItems();

  /**
   * @return an unmodifiable view of the filtered items
   */
  List<T> getFilteredItems();

  /**
   * @return all visible and filtered items in this table model
   */
  List<T> getAllItems();

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
   * @param includeFiltered if true then the filtered items are included
   * @return true if this model contains the item
   */
  boolean contains(final T item, final boolean includeFiltered);

  /**
   * Returns true if the given item is visible, that is, not filtered in this model
   * @param item the item
   * @return true if the given item is visible
   */
  boolean isVisible(final T item);

  /**
   * Returns true if the given item is being filtered in this model, that is, is not visible
   * @param item the item
   * @return true if the given item is filtered
   */
  boolean isFiltered(final T item);
}
