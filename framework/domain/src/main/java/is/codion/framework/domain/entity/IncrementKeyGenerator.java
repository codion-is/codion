/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.common.db.connection.DatabaseConnection;
import is.codion.common.db.database.Database;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;

import java.sql.SQLException;

import static java.util.Objects.requireNonNull;

final class IncrementKeyGenerator extends AbstractQueriedKeyGenerator {

  private final String query;

  IncrementKeyGenerator(String tableName, String columnName) {
    requireNonNull(tableName, "tableName");
    requireNonNull(columnName, "columnName");
    this.query = "select max(" + columnName + ") + 1 from " + tableName;
  }

  @Override
  public void beforeInsert(Entity entity, DatabaseConnection connection) throws SQLException {
    ColumnDefinition<?> primaryKeyColumn = entity.entityDefinition().primaryKeyColumnDefinitions().get(0);
    if (entity.isNull(primaryKeyColumn.attribute())) {
      selectAndPut(entity, primaryKeyColumn, connection);
    }
  }

  @Override
  protected String query(Database database) {
    return query;
  }
}
