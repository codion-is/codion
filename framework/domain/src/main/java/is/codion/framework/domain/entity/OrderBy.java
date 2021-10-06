/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.util.List;

/**
 * Specifies an order by clause
 */
public interface OrderBy {

  /**
   * Adds an 'ascending' order by for the given attributes
   * @param attributes the attributes
   * @return this OrderBy instance
   */
  OrderBy ascending(Attribute<?>... attributes);

  /**
   * Adds a 'descending' order by for the given attributes
   * @param attributes the attributes
   * @return this OrderBy instance
   */
  OrderBy descending(Attribute<?>... attributes);

  /**
   * @return the order by attributes comprising this order by clause
   */
  List<OrderByAttribute> getOrderByAttributes();

  /**
   * Specifies an order by attribute and whether it's ascending or descending
   */
  interface OrderByAttribute {

    /**
     * @return the attribute to order by
     */
    Attribute<?> getAttribute();

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
