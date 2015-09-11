/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.criteria;

/**
 * An interface encapsulating a set of Criteria objects, that should be either AND'ed or OR'ed together in a query context
 */
public interface CriteriaSet<T> extends Criteria<T> {

  /**
   * Adds a new Criteria object to this set, adding a null criteria has no effect
   * @param criteria the Criteria to add
   */
  void add(final Criteria<T> criteria);

  /**
   * @return the number of criteria in this set
   */
  int getCriteriaCount();
}
