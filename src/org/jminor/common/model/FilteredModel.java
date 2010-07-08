/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

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
}
