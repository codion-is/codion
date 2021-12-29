/*
 * Copyright (c) 2021 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.query;

final class DefaultSelectQuery implements SelectQuery {

  private final String query;
  private final String fromClause;
  private final String whereClause;
  private final boolean containsWhereClause;

  DefaultSelectQuery(final String fromClause, final String whereClause) {
    this.query = null;
    this.fromClause = fromClause;
    this.whereClause = whereClause;
    this.containsWhereClause = whereClause != null;
  }

  DefaultSelectQuery(final String query, final boolean containsWhereClause) {
    this.query = query;
    this.fromClause = null;
    this.whereClause = null;
    this.containsWhereClause = containsWhereClause;
  }

  @Override
  public String getQuery() {
    return query;
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
  public boolean containsWhereClause() {
    return containsWhereClause;
  }
}
