/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.criteria;

import java.util.List;

/**
 * A generic interface for objects serving as where conditions in database queries
 * @param <T> the type used as keys mapping the property values
 */
public interface Criteria<T> {
  /**
   * @return a SQL where condition string without the 'where' keyword
   */
  String asString();

  /**
   * @return a list of the values this criteria is based on, in the order they appear
   * in the condition clause.
   */
  List<Object> getValues();

  /**
   * @return a list of T describing the values this criteria is based on, in the same
   * order as their respective values appear in the condition clause
   */
  List<T> getValueKeys();
}
