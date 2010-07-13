/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.util.List;

/**
 * User: darri
 * Date: 8.7.2010
 * Time: 12:07:46
 */
public interface FilteredModel<T> {

  Event eventFilteringDone();

  /**
   * @return an Event fired when the model is about to be filtered
   */
  Event eventFilteringStarted();

  /**
   * Filters the table according to the criteria returned by <code>getFilterCriteria()</code>
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
   * @return an unmodifiable view of the hidden items
   */
  List<T> getHiddenItems();

  /**
   * @return all visible and hidden items in this table model
   */
  List<T> getAllItems();

  /**
   * @param includeHidden if true then filtered items are included
   * @return all items in this table model
   */
  List<T> getAllItems(final boolean includeHidden);

  /**
   * @return the number of currently visible items
   */
  int getVisibleItemCount();

  /**
   * @return the number of currently filtered (hidden) items
   */
  int getHiddenItemCount();
}
