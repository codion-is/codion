/*
 * Copyright (c) 2021 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.query;

final class DefaultSelectQuery implements SelectQuery {

  private final String columnsClause;
  private final String fromClause;
  private final String whereClause;
  private final String orderByClause;

  DefaultSelectQuery(String columnsClause, String fromClause, String whereClause,
                     String orderByClause) {
    this.columnsClause = columnsClause;
    this.fromClause = fromClause;
    this.whereClause = whereClause;
    this.orderByClause = orderByClause;
  }

  @Override
  public String getColumns() {
    return columnsClause;
  }

  @Override
  public String getFrom() {
    return fromClause;
  }

  @Override
  public String getWhere() {
    return whereClause;
  }

  @Override
  public String getOrderBy() {
    return orderByClause;
  }
}
