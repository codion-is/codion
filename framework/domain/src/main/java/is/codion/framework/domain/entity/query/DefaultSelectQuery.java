/*
 * Copyright (c) 2021 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.query;

final class DefaultSelectQuery implements SelectQuery {

  private final String columnsClause;
  private final String fromClause;
  private final String whereClause;
  private final String orderByClause;

  DefaultSelectQuery(final String columnsClause, final String fromClause, final String whereClause,
                     final String orderByClause) {
    this.columnsClause = columnsClause;
    this.fromClause = fromClause;
    this.whereClause = whereClause;
    this.orderByClause = orderByClause;
  }

  @Override
  public String getColumnsClause() {
    return columnsClause;
  }

  @Override
  public String getFromClause() {
    return fromClause;
  }

  @Override
  public String getWhereClause() {
    return whereClause;
  }

  @Override
  public String getOrderByClause() {
    return orderByClause;
  }
}
