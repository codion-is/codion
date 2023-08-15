/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.common.db.connection.DatabaseConnection;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

final class IdentityKeyGenerator implements KeyGenerator {

  @Override
  public boolean isInserted() {
    return false;
  }

  @Override
  public boolean returnGeneratedKeys() {
    return true;
  }

  @Override
  public void afterInsert(Entity entity, DatabaseConnection connection, Statement insertStatement) throws SQLException {
    try (ResultSet generatedKeys = insertStatement.getGeneratedKeys()) {
      if (generatedKeys.next()) {
        ColumnDefinition<?> column = entity.definition().primaryKeyColumnDefinitions().get(0);
        entity.put((Attribute<Object>) column.attribute(), generatedKeys.getObject(column.columnName()));
      }
    }
  }
}
