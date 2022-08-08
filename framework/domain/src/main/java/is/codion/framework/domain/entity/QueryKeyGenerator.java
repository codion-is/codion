/*
 * Copyright (c) 2021 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.common.db.database.Database;

import static java.util.Objects.requireNonNull;

final class QueryKeyGenerator extends AbstractQueriedKeyGenerator {

  private final String query;

  QueryKeyGenerator(String query) {
    this.query = requireNonNull(query, "query");
  }

  @Override
  protected String query(Database database) {
    return query;
  }
}
