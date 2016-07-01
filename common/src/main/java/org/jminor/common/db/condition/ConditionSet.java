/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.condition;

/**
 * An interface encapsulating a set of Condition objects, that should be either AND'ed or OR'ed together in a query context
 * @param <T> the type used to describe the condition values
 */
public interface ConditionSet<T> extends Condition<T> {

  /**
   * Adds a new Condition object to this set, adding a null condition has no effect
   * @param condition the Condition to add
   */
  void add(final Condition<T> condition);

  /**
   * @return the number of condition in this set
   */
  int getConditionCount();
}
