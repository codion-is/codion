/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.property.Attribute;

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
  OrderBy ascending(Attribute<?>... propertyIds);

    /**
   * Adds a 'descending' order by for the given properties
   * @param propertyIds the property ids
   * @return this OrderBy instance
   */
  OrderBy descending(Attribute<?>... propertyIds);

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
    Attribute<?> getPropertyId();

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
