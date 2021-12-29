/*
 * Copyright (c) 2021 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.query;

import static java.util.Objects.requireNonNull;

/**
 * Specifies a select query, either a full query or a from clause with an optional where clause.
 * For full queries use {@link #query(String)} or {@link #queryContainingWhereClause(String)}.
 * For partial queries (without a columns clause), based on a from clause
 * and/or where clause use the {@link Builder} provided by {@link #builder()}.
 */
public interface SelectQuery {

  /**
   * @return the query string
   */
  String getQuery();

  /**
   * @return the FROM clause
   */
  String getFromClause();

  /**
   * @return the WHERE clause
   */
  String getWhereClause();

  /**
   * @return true if this query contains a WHERE clause
   */
  boolean containsWhereClause();

  /**
   * A convenience method for creating a SelectQuery based on a full query, without a WHERE clause.
   * @param query the query, may not contain a WHERE clause
   * @return a SelectQuery based on the given query
   */
  static SelectQuery query(final String query) {
    return new DefaultSelectQuery(requireNonNull(query, "query"), false);
  }

  /**
   * A convenience method for creating a SelectQuery based on a full query, containing a WHERE clause.
   * @param queryContainingWhereClause the query, must contain a WHERE clause
   * @return a SelectQuery based on the given query
   */
  static SelectQuery queryContainingWhereClause(final String queryContainingWhereClause) {
    return new DefaultSelectQuery(requireNonNull(queryContainingWhereClause, "queryContainingWhereClause"), true);
  }

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
     * @return a new SelectQuery instance
     */
    SelectQuery build();
  }
}
