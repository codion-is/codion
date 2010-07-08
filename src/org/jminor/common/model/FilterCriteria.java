/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

/**
 * An interface describing a filtering criteria.
 * @param <T> the type of items this criteria filters.
 */
public interface FilterCriteria<T> {
  /**
   * @param item the item
   * @return true if <code>item</code> should be included
   */
  boolean include(T item);

  /**
   * A convenience criteria which always returns true
   */
  static FilterCriteria ACCEPT_ALL_CRITERIA = new FilterCriteria() {
    public boolean include(final Object item) {
      return true;
    }
  };

  /**
   * A convenience criteria which always returns false
   */
  static FilterCriteria REJECT_ALL_CRITERIA = new FilterCriteria() {
    public boolean include(final Object item) {
      return true;
    }
  };
}
