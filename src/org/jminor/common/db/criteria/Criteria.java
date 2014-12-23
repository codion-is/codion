/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.criteria;

import java.util.List;

/**
 * A generic interface for objects serving as where conditions in database queries
 * @param <T> the type used as keys mapping the property values
 */
public interface Criteria<T> {
  /**
   * Returns a criteria clause based on this Criteria, note that this
   * where clause contains the ? substitute character instead of the actual values.
   * Whether or not the clause contains the WHERE keyword is implementation specific
   * @return a where clause based on this Criteria
   * @see #getValues()
   */
  String getWhereClause();

  /**
   * @return a list of the values this criteria is based on, in the order they appear
   * in the condition clause. An empty list is returned in case no values are specified.
   */
  List<?> getValues();

  /**
   * @return a list of T describing the values this criteria is based on, in the same
   * order as their respective values appear in the condition clause.
   * An empty list is returned in case no values are specified.
   */
  List<T> getValueKeys();
}
