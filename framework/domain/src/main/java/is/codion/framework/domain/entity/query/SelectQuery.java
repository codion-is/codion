/*
 * Copyright (c) 2021 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.query;

/**
 * Specifies a select query, either a full query or a from clause with an optional where clause.
 * For full queries use {@link #query(String)} or {@link #queryContainingWhereClause(String)}.
 * For partial queries (without a columns clause), based on a from clause
 * and/or where clause use the {@link Builder} provided by {@link #builder()}.
 */
public interface SelectQuery {

  /**
   * @return the COLUMNS clause string
   */
  String getColumnsClause();

  /**
   * @return the FROM clause
   */
  String getFromClause();

  /**
   * @return the WHERE clause
   */
  String getWhereClause();

  /**
   * @return the order by clause
   */
  String getOrderByClause();

  /**
   * @return a new {@link SelectQuery.Builder} instance.
   */
  static Builder builder() {
    return new DefaultSelectQueryBuilder();
  }

  /**
   * Builds a {@link SelectQuery}.
   */
  interface Builder {

    /**
     * Specifies the columns clause to use, without the SELECT keyword.
     * @param columnsClause the columns clause
     * @return this Builder instance
     */
    Builder columnsClause(String columnsClause);

    /**
     * Specifies the from clause to use, without the FROM keyword.
     * @param fromClause the from clause
     * @return this Builder instance
     */
    Builder fromClause(String fromClause);

    /**
     * Specifies the where clause to use, without the WHERE keyword.
     * @param whereClause the where clause
     * @return this Builder instance
     */
    Builder whereClause(String whereClause);

    /**
     * Specifies the order by clause to use, without the ORDER BY keywords.
     * @param orderByClause the order by clause
     * @return this Builder instance
     */
    Builder orderByClause(String orderByClause);

    /**
     * @return a new SelectQuery instance
     */
    SelectQuery build();
  }
}
