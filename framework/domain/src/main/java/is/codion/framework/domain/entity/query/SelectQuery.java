/*
 * Copyright (c) 2021 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.query;

/**
 * Specifies a select query, either a full query or a from clause with an optional where clause.
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
     * Specifies full select query, without a WHERE clause.
     * @param query the query
     * @return this Builder instance
     * @see #queryContainingWhereClause(String)
     */
    Builder query(String query);

    /**
     * Specifies full select query, including a WHERE clause.
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
