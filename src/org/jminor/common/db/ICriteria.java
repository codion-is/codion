/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import java.io.Serializable;

/**
 * A generic interface for objects serving as where conditions in database queries
 */
public interface ICriteria extends Serializable {
  /**
   * @return a SQL where condition string
   */
  public String toString();
}
