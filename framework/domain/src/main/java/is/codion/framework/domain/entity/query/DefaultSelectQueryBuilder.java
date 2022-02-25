/*
 * Copyright (c) 2021 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.query;

import static java.util.Objects.requireNonNull;

class DefaultSelectQueryBuilder implements SelectQuery.Builder {

  private final String from;
  private String columns;
  private String where;
  private String orderBy;

  DefaultSelectQueryBuilder(String from) {
    if (requireNonNull(from, "from").trim().toLowerCase().startsWith("from")) {
      throw new IllegalArgumentException("from clause should not include the 'FROM' keyword");
    }
    this.from = from;
  }

  @Override
  public SelectQuery.Builder columns(String columns) {
    if (requireNonNull(columns, "columns").trim().toLowerCase().startsWith("select")) {
      throw new IllegalArgumentException("columns clause should not include the 'SELECT' keyword");
    }
    this.columns = columns;
    return this;
  }

  @Override
  public SelectQuery.Builder where(String where) {
    if (requireNonNull(where, "where").trim().toLowerCase().startsWith("where")) {
      throw new IllegalArgumentException("where clause should not include the 'WHERE' keyword");
    }
    this.where = where;

    return this;
  }

  @Override
  public SelectQuery.Builder orderBy(String orderBy) {
    if (requireNonNull(orderBy, "orderBy").trim().toLowerCase().startsWith("order by")) {
      throw new IllegalArgumentException("orderBy clause should not include the 'ORDER BY' keywords");
    }
    this.orderBy = orderBy;

    return this;
  }

  @Override
  public SelectQuery build() {
    return new DefaultSelectQuery(columns, from, where, orderBy);
  }
}
