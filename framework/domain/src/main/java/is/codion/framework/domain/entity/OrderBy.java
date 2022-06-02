/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.util.List;

/**
 * Specifies an order by clause
 */
public interface OrderBy {

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
   * Builds a {@link OrderBy} instance.
   */
  interface Builder {

    /**
     * Adds an 'ascending' order by for the given attributes
     * @param attributes the attributes
     * @return this builder instance
     * @throws IllegalArgumentException in case {@code attributes} is empty
     */
    Builder ascending(Attribute<?>... attributes);

    /**
     * Adds a 'descending' order by for the given attributes
     * @param attributes the attributes
     * @return this builder instance
     * @throws IllegalArgumentException in case {@code attributes} is empty
     */
    Builder descending(Attribute<?>... attributes);

    /**
     * @return a new {@link OrderBy} instance based on this builder
     */
    OrderBy build();
  }

  /**
   * Creates a {@link OrderBy.Builder} instance.
   * @return a {@link OrderBy.Builder} instance
   */
  static OrderBy.Builder builder() {
    return new DefaultOrderBy.DefaultOrderByBuilder();
  }

  /**
   * @param attributes the attributes to order by ascending
   * @return a new ascending OrderBy instance based on the given attributes
   */
  static OrderBy ascending(Attribute<?>... attributes) {
    return builder().ascending(attributes).build();
  }

  /**
   * @param attributes the attributes to order by descending
   * @return a new descending OrderBy instance based on the given attributes
   */
  static OrderBy descending(Attribute<?>... attributes) {
    return builder().descending(attributes).build();
  }
}
