/*
 * Copyright (c) 2021 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.query;

final class DefaultSelectQuery implements SelectQuery {

  private final String query;
  private final boolean containsWhereClause;
  private final boolean containsColumnsClause;

  DefaultSelectQuery(final String fromClause, final String whereClause) {
    this("from " + fromClause + (whereClause == null ? "" : "\nwhere " + whereClause), false, whereClause != null);
  }

  DefaultSelectQuery(final String query, final boolean containsColumnsClause, final boolean containsWhereClause) {
    this.query = query;
    this.containsColumnsClause = containsColumnsClause;
    this.containsWhereClause = containsWhereClause;
  }

  @Override
  public String getQuery() {
    return query;
  }

  @Override
  public boolean containsColumnsClause() {
    return containsColumnsClause;
  }

  @Override
  public boolean containsWhereClause() {
    return containsWhereClause;
  }
}
