/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 *
 */
package org.jminor.common.model;

public interface IFilterCriteria {
  /**
   * @param item the item
   * @return true if <code>item</code> should be included
   */
  public boolean include(Object item);
}
