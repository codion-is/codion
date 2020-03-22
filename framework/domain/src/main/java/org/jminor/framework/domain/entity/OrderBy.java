/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.entity;

import java.io.Serializable;
import java.util.List;

/**
 * Specifies a order by clause
 */
public interface OrderBy extends Serializable {

  /**
   * Adds an 'ascending' order by for the given properties
   * @param propertyIds the property ids
   * @return this OrderBy instance
   */
  OrderBy ascending(String... propertyIds);

    /**
   * Adds a 'descending' order by for the given properties
   * @param propertyIds the property ids
   * @return this OrderBy instance
   */
  OrderBy descending(String... propertyIds);

  /**
   * @return the order by properties comprising this order by clause
   */
  List<OrderByProperty> getOrderByProperties();

  /**
   * Specifies a order by property and whether it's ascending or descending
   */
  interface OrderByProperty extends Serializable {

    /**
     * @return the id of the property to order by
     */
    String getPropertyId();

    /**
     * @return true if the order is ascending, false for descending
     */
    boolean isAscending();
  }

  /**
   * Creates a {@link OrderBy} instance.
   * @return a {@link OrderBy} instance
   */
  static OrderBy orderBy() {
    return new DefaultOrderBy();
  }
}
