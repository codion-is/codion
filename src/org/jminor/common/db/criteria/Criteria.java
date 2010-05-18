/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.criteria;

import java.util.List;

/**
 * A generic interface for objects serving as where conditions in database queries
 */
public interface Criteria<T> {
  /**
   * @return a SQL where condition string without the 'where' keyword  @param database the Database instance
   */
  String asString();

  /**
   * @return a list of the values this criteria is based on
   */
  List<Object> getValues();

  /**
   * @return a list of objects describing the values this criteria is based on, if any
   */
  List<T> getValueKeys();
}
