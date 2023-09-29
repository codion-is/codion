/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.common.db.connection.DatabaseConnection;
import is.codion.common.db.database.Database;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;

import java.sql.SQLException;

import static java.util.Objects.requireNonNull;

final class QueryKeyGenerator extends AbstractQueriedKeyGenerator {

  private final String query;

  QueryKeyGenerator(String query) {
    this.query = requireNonNull(query, "query");
  }

  @Override
  public void beforeInsert(Entity entity, DatabaseConnection connection) throws SQLException {
    ColumnDefinition<?> columnDefinition = entity.entityDefinition().primaryKey().definitions().get(0);
    if (entity.isNull(columnDefinition.attribute())) {
      selectAndPopulate(entity, columnDefinition, connection);
    }
  }

  @Override
  protected String query(Database database) {
    return query;
  }
}
