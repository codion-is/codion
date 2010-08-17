/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.criteria;

/**
 * An object responsible for providing a criteria.
 */
public interface CriteriaProvider<T> {

  /**
   * @return a criteria object
   */
  Criteria<T> getCriteria();
}
