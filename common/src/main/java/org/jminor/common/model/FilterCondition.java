/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

/**
 * An interface describing a filtering condition.
 * @param <T> the type of items this condition filters.
 */
public interface FilterCondition<T> {

  /**
   * @param item the item
   * @return true if {@code item} should be included, false if it should be filtered
   */
  boolean include(T item);

  /**
   * A convenience condition class which always returns true
   */
  final class AcceptAllCondition<T> implements FilterCondition<T> {
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
   * A convenience condition class which always returns false
   */
  final class RejectAllCondition<T> implements FilterCondition<T> {
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
