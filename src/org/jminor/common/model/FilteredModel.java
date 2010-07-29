/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.awt.event.ActionListener;
import java.util.List;

/**
 * User: darri
 * Date: 8.7.2010
 * Time: 12:07:46
 */
public interface FilteredModel<T> {

  void addFilteringListener(final ActionListener listener);

  void removeFilteringListener(final ActionListener listener);

  /**
   * Filters the table according to the criteria returned by <code>getFilterCriteria()</code>.
   * This method does not interfere with the internal ordering of the visible items.
   * @see #getFilterCriteria()
   */
  void filterContents();

  /**
   * Returns the filter criteria defined by this model, this method should return
   * a "accept all" criteria instead of null, if no criteria is defined.
   * @return the filter criteria
   */
  FilterCriteria<T> getFilterCriteria();

  /**
   * @param filterCriteria the FilterCriteria to use
   */
  void setFilterCriteria(final FilterCriteria<T> filterCriteria);

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
   * @return true if this combo box model contains the item
   */
  boolean contains(final T item, final boolean includeFiltered);

  /**
   * Returns true if the given item is visible in this combo box model
   * @param item the item
   * @return true if the given item is visible
   */
  boolean isVisible(final T item);

  /**
   * Returns true if the given item is filtered in this combo box model
   * @param item the item
   * @return true if the given item is filtered
   */
  boolean isFiltered(final T item);
}
