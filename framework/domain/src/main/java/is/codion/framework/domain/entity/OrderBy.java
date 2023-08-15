/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.entity.attribute.Column;

import java.util.List;

/**
 * Specifies an order by clause.
 * @see #ascending(Column[])
 * @see #descending(Column[])
 * @see #builder()
 */
public interface OrderBy {

  /**
   * @return the order by columns comprising this order by clause
   */
  List<OrderByColumn> orderByColumns();

  /**
   * Specifies an order by column and whether it's ascending or descending
   */
  interface OrderByColumn {

    /**
     * @return the column to order by
     */
    Column<?> column();

    /**
     * @return true if the order is ascending, false for descending
     */
    boolean isAscending();

    /**
     * @return the {@link NullOrder} when ordering by this column
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
     * Adds an 'ascending' order by for the given columns
     * @param columns the columns
     * @return this builder instance
     * @throws IllegalArgumentException in case {@code columns} is empty
     */
    Builder ascending(Column<?>... columns);

    /**
     * Adds an 'ascending' order by for the given columns with nulls appearing first
     * @param columns the columns
     * @return this builder instance
     * @throws IllegalArgumentException in case {@code columns} is empty
     */
    Builder ascendingNullsFirst(Column<?>... columns);

    /**
     * Adds an 'ascending' order by for the given columns with nulls appearing last
     * @param columns the columns
     * @return this builder instance
     * @throws IllegalArgumentException in case {@code columns} is empty
     */
    Builder ascendingNullsLast(Column<?>... columns);

    /**
     * Adds a 'descending' order by for the given columns
     * @param columns the columns
     * @return this builder instance
     * @throws IllegalArgumentException in case {@code columns} is empty
     */
    Builder descending(Column<?>... columns);

    /**
     * Adds a 'descending' order by for the given columns with nulls appearing first
     * @param columns the columns
     * @return this builder instance
     * @throws IllegalArgumentException in case {@code columns} is empty
     */
    Builder descendingNullsFirst(Column<?>... columns);

    /**
     * Adds a 'descending' order by for the given columns with nulls appearing last
     * @param columns the columns
     * @return this builder instance
     * @throws IllegalArgumentException in case {@code columns} is empty
     */
    Builder descendingNullsLast(Column<?>... columns);

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
   * @param columns the columns to order by ascending
   * @return a new ascending OrderBy instance based on the given columns
   */
  static OrderBy ascending(Column<?>... columns) {
    return builder().ascending(columns).build();
  }

  /**
   * @param columns the columns to order by descending
   * @return a new descending OrderBy instance based on the given columns
   */
  static OrderBy descending(Column<?>... columns) {
    return builder().descending(columns).build();
  }
}
