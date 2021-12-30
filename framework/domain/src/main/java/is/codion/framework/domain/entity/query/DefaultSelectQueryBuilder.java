/*
 * Copyright (c) 2021 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.query;

import static java.util.Objects.requireNonNull;

class DefaultSelectQueryBuilder implements SelectQuery.Builder {

  private String columnsClause;
  private String fromClause;
  private String whereClause;
  private String orderByClause;

  @Override
  public SelectQuery.Builder columnsClause(final String columnsClause) {
    if (requireNonNull(columnsClause, "columnsClause").trim().toLowerCase().startsWith("select")) {
      throw new IllegalArgumentException("columnsClause should not include the 'SELECT' keyword");
    }
    this.columnsClause = columnsClause;
    return this;
  }

  @Override
  public SelectQuery.Builder fromClause(final String fromClause) {
    if (requireNonNull(fromClause, "fromClause").trim().toLowerCase().startsWith("from")) {
      throw new IllegalArgumentException("fromClause should not include the 'FROM' keyword");
    }
    this.fromClause = fromClause;

    return this;
  }

  @Override
  public SelectQuery.Builder whereClause(final String whereClause) {
    if (requireNonNull(whereClause, "whereClause").trim().toLowerCase().startsWith("where")) {
      throw new IllegalArgumentException("whereClause should not include the 'WHERE' keyword");
    }
    this.whereClause = whereClause;

    return this;
  }

  @Override
  public SelectQuery.Builder orderByClause(final String orderByClause) {
    if (requireNonNull(orderByClause, "orderByClause").trim().toLowerCase().startsWith("order by")) {
      throw new IllegalArgumentException("orderByClause should not include the 'ORDER BY' keywords");
    }
    this.orderByClause = orderByClause;

    return this;
  }

  @Override
  public SelectQuery build() {
    if (fromClause == null) {
      throw new IllegalStateException("A fromClause must be specified to build a SelectQuery");
    }

    return new DefaultSelectQuery(columnsClause, fromClause, whereClause, orderByClause);
  }
}
