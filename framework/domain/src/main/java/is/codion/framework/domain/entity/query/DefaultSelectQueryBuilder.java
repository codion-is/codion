/*
 * Copyright (c) 2021 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.query;

import static java.util.Objects.requireNonNull;

class DefaultSelectQueryBuilder implements SelectQuery.Builder {

  private String query;
  private String fromClause;
  private String whereClause;
  private boolean containsWhereClause;

  @Override
  public SelectQuery.Builder query(final String query) {
    if (fromClause != null || whereClause != null) {
      throw new IllegalStateException("fromClause or whereClause have already been set");
    }
    this.query = requireNonNull(query);

    return this;
  }

  @Override
  public SelectQuery.Builder queryContainingWhereClause(final String queryContainingWhereClause) {
    this.containsWhereClause = true;

    return query(queryContainingWhereClause);
  }

  @Override
  public SelectQuery.Builder fromClause(final String fromClause) {
    if (query != null) {
      throw new IllegalStateException("query has already been set");
    }
    if (requireNonNull(fromClause, "fromClause").trim().toLowerCase().startsWith("from")) {
      throw new IllegalArgumentException("fromClause should not include the 'FROM' keyword");
    }
    this.fromClause = fromClause;

    return this;
  }

  @Override
  public SelectQuery.Builder whereClause(final String whereClause) {
    if (query != null) {
      throw new IllegalStateException("query has already been set");
    }
    if (requireNonNull(whereClause, "whereClause").trim().toLowerCase().startsWith("where")) {
      throw new IllegalArgumentException("whereClause should not include the 'WHERE' keyword");
    }
    this.whereClause = whereClause;
    this.containsWhereClause = true;

    return this;
  }

  @Override
  public SelectQuery build() {
    if (query == null && fromClause == null) {
      throw new IllegalStateException("Query or fromClause must be specified to build a SelectQuery");
    }
    if (query != null) {
      return new DefaultSelectQuery(query, true, containsWhereClause);
    }

    return new DefaultSelectQuery(fromClause, whereClause);
  }
}
