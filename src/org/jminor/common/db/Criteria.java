/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

/**
 * A generic interface for objects serving as where conditions in database queries
 */
public interface Criteria {
  /**
   * @return a SQL where condition string without the 'where' keyword
   */
  @Override
  public String toString();
}
