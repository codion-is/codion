/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.util.List;

/**
 * Specifies an order by clause.
 * @see #ascending(Column[])
 * @see #descending(Column[])
 * @see #builder()
 */
public interface OrderBy {

  /**
   * @return the order by attributes comprising this order by clause
   */
  List<OrderByAttribute> orderByAttributes();

  /**
   * Specifies an order by attribute and whether it's ascending or descending
   */
  interface OrderByAttribute {

    /**
     * @return the attribute to order by
     */
    Column<?> attribute();

    /**
     * @return true if the order is ascending, false for descending
     */
    boolean isAscending();

    /**
     * @return the {@link NullOrder} when ordering by this attribute
     */
    NullOrder nullOrder();
  }

  /**
   * Specifies how to handle null values during order by.
   */
  enum NullOrder {

    /**
     * Nulls first.
     */
    NULLS_FIRST,

    /**
     * Nulls last.
     */
    NULLS_LAST,

    /**
     * Database default, as in, no null ordering directive.
     */
    DEFAULT
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
    Builder ascending(Column<?>... attributes);

    /**
     * Adds an 'ascending' order by for the given attributes with nulls appearing first
     * @param attributes the attributes
     * @return this builder instance
     * @throws IllegalArgumentException in case {@code attributes} is empty
     */
    Builder ascendingNullsFirst(Column<?>... attributes);

    /**
     * Adds an 'ascending' order by for the given attributes with nulls appearing last
     * @param attributes the attributes
     * @return this builder instance
     * @throws IllegalArgumentException in case {@code attributes} is empty
     */
    Builder ascendingNullsLast(Column<?>... attributes);

    /**
     * Adds a 'descending' order by for the given attributes
     * @param attributes the attributes
     * @return this builder instance
     * @throws IllegalArgumentException in case {@code attributes} is empty
     */
    Builder descending(Column<?>... attributes);

    /**
     * Adds a 'descending' order by for the given attributes with nulls appearing first
     * @param attributes the attributes
     * @return this builder instance
     * @throws IllegalArgumentException in case {@code attributes} is empty
     */
    Builder descendingNullsFirst(Column<?>... attributes);

    /**
     * Adds a 'descending' order by for the given attributes with nulls appearing last
     * @param attributes the attributes
     * @return this builder instance
     * @throws IllegalArgumentException in case {@code attributes} is empty
     */
    Builder descendingNullsLast(Column<?>... attributes);

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
  static OrderBy ascending(Column<?>... attributes) {
    return builder().ascending(attributes).build();
  }

  /**
   * @param attributes the attributes to order by descending
   * @return a new descending OrderBy instance based on the given attributes
   */
  static OrderBy descending(Column<?>... attributes) {
    return builder().descending(attributes).build();
  }
}
