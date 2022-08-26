/*
 * Copyright (c) 2021 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.common.db.connection.DatabaseConnection;
import is.codion.common.db.database.Database;
import is.codion.framework.domain.property.ColumnProperty;

import java.sql.SQLException;
import java.util.List;

import static java.util.Objects.requireNonNull;

final class IncrementKeyGenerator extends AbstractQueriedKeyGenerator {

  private final String query;

  IncrementKeyGenerator(String tableName, String columnName) {
    requireNonNull(tableName, "tableName");
    requireNonNull(columnName, "columnName");
    this.query = "select max(" + columnName + ") + 1 from " + tableName;
  }

  @Override
  public void beforeInsert(Entity entity, List<ColumnProperty<?>> primaryKeyProperties,
                           DatabaseConnection connection) throws SQLException {
    ColumnProperty<?> primaryKeyProperty = primaryKeyProperties.get(0);
    if (entity.isNull(primaryKeyProperty.attribute())) {
      selectAndPut(entity, primaryKeyProperty, connection);
    }
  }

  @Override
  protected String query(Database database) {
    return query;
  }
}
