/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

/**
 * An interface describing a filtering criteria.
 * @param <T> the type of items this criteria filters.
 */
public interface FilterCriteria<T> {

  /**
   * @param item the item
   * @return true if <code>item</code> should be included, false if it should be filtered
   */
  boolean include(T item);

  /**
   * A convenience criteria class which always returns true
   */
  final class AcceptAllCriteria<T> implements FilterCriteria<T> {
    /**
     * @param item the item
     * @return true
     */
    @Override
    public boolean include(final T item) {
      return true;
    }
  }

  /**
   * A convenience criteria class which always returns false
   */
  final class RejectAllCriteria<T> implements FilterCriteria<T> {
    /**
     * @param item the item
     * @return false
     */
    @Override
    public boolean include(final T item) {
      return false;
    }
  }
}
