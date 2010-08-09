/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.criteria;

/**
 * User: Björn Darri
 * Date: 19.7.2010
 * Time: 17:34:21
 */
public interface CriteriaProvider<T> {

  /**
   * @return a criteria object
   */
  Criteria<T> getCriteria();
}
