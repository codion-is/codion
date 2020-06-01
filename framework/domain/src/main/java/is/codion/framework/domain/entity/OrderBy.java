/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.attribute.Attribute;

import java.io.Serializable;
import java.util.List;

/**
 * Specifies a order by clause
 */
public interface OrderBy extends Serializable {

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
   * Specifies a order by property and whether it's ascending or descending
   */
  interface OrderByAttribute extends Serializable {

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
