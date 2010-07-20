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
   * A convenience criteria class which always returns true
   */
  class AcceptAllCriteria<T> implements FilterCriteria<T> {
    public boolean include(final T item) {
      return true;
    }
  }

  /**
   * A convenience criteria class which always returns false
   */
  class RejectAllCriteria<T> implements FilterCriteria<T> {
    public boolean include(final T item) {
      return false;
    }
  }
}
