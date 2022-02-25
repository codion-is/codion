/*
 * Copyright (c) 2021 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.query;

/**
 * Defines a select query, by specifying a from clause with optional columns, where and orderBy clauses.
 * {@link Builder} provided by {@link #builder(String)}.
 */
public interface SelectQuery {

  /**
   * @return the COLUMNS clause string
   */
  String getColumns();

  /**
   * @return the FROM clause
   */
  String getFrom();

  /**
   * @return the WHERE clause
   */
  String getWhere();

  /**
   * @return the order by clause
   */
  String getOrderBy();

  /**
   * Creates a {@link Builder} with the given from clause
   * @param from the from clause, without the FROM keyword.
   * @return a new {@link SelectQuery.Builder} instance.
   */
  static Builder builder(String from) {
    return new DefaultSelectQueryBuilder(from);
  }

  /**
   * Builds a {@link SelectQuery}.
   */
  interface Builder {

    /**
     * Specifies the columns clause to use, without the SELECT keyword.
     * @param columns the columns clause
     * @return this Builder instance
     */
    Builder columns(String columns);

    /**
     * Specifies the where clause to use, without the WHERE keyword.
     * @param where the where clause
     * @return this Builder instance
     */
    Builder where(String where);

    /**
     * Specifies the order by clause to use, without the ORDER BY keywords.
     * @param orderBy the order by clause
     * @return this Builder instance
     */
    Builder orderBy(String orderBy);

    /**
     * @return a new SelectQuery instance
     */
    SelectQuery build();
  }
}
