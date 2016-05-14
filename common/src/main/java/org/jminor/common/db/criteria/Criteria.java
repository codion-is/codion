/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.criteria;

import java.util.List;

/**
 * A generic interface for objects serving as where conditions in database queries
 * @param <T> the type used to describe the criteria values
 */
public interface Criteria<T> {
  /**
   * Returns a criteria clause based on this Criteria, note that this
   * clause contains the ? substitute character instead of the actual values.
   * Note that this method can return an empty string.
   * @return a where clause based on this Criteria
   * @see #getValues()
   */
  String getWhereClause();

  /**
   * @return a list of the values this criteria is based on, in the order they appear
   * in the condition clause. An empty list is returned in case no values are specified.
   */
  List getValues();

  /**
   * @return a list of T describing the values this criteria is based on, in the same
   * order as their respective values appear in the condition clause.
   * An empty list is returned in case no values are specified.
   */
  List<T> getValueKeys();
}
