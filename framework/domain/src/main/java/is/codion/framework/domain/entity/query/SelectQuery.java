/*
 * Copyright (c) 2021 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.query;

import static java.util.Objects.requireNonNull;

/**
 * Specifies a select query, either a full query or a from clause with an optional where clause.
 * For full queries use {@link #selectQuery(String)} or {@link #selectQueryContainingWhereClause(String)}.
 * For partial queries (without a columns clause), based on a from clause
 * and/or where clause use the {@link Builder} provided by {@link #builder()}.
 */
public interface SelectQuery {

  /**
   * @return the query string
   */
  String getQuery();

  /**
   * @return true if the query string contains a columns clause
   */
  boolean containsColumnsClause();

  /**
   * @return true if the query string contains a WHERE clause
   */
  boolean containsWhereClause();

  /**
   * A convenience method for creating a SelectQuery based on a full query, without a WHERE clause.
   * @param selectQuery the query, may not contain a WHERE clause
   * @return a SelectQuery based on the given query
   */
  static SelectQuery selectQuery(final String selectQuery) {
    return new DefaultSelectQuery(requireNonNull(selectQuery, "selectQuery"), true, false);
  }

  /**
   * A convenience method for creating a SelectQuery based on a full query, containing a WHERE clause.
   * @param selectQueryContainingWhereClause the query, must contain a WHERE clause
   * @return a SelectQuery based on the given query
   */
  static SelectQuery selectQueryContainingWhereClause(final String selectQueryContainingWhereClause) {
    return new DefaultSelectQuery(requireNonNull(selectQueryContainingWhereClause, "selectQueryContainingWhereClause"), true, true);
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
     * Specifies a full select query, without a WHERE clause.
     * @param query the query
     * @return this Builder instance
     * @see #queryContainingWhereClause(String)
     */
    Builder query(String query);

    /**
     * Specifies a full select query, including a WHERE clause.
     * @param queryContainingWhereClause the query, including a WHERE clause
     * @return this Builder instance
     */
    Builder queryContainingWhereClause(String queryContainingWhereClause);

    /**
     * @param fromClause the from clause
     * @return this Builder instance
     */
    Builder fromClause(String fromClause);

    /**
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
