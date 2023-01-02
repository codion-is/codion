/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.common.db.connection.DatabaseConnection;
import is.codion.framework.domain.property.ColumnProperty;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

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
  public void afterInsert(Entity entity, List<ColumnProperty<?>> primaryKeyProperties,
                          DatabaseConnection connection, Statement insertStatement) throws SQLException {
    try (ResultSet generatedKeys = insertStatement.getGeneratedKeys()) {
      if (generatedKeys.next()) {
        ColumnProperty<?> property = primaryKeyProperties.get(0);
        entity.put((Attribute<Object>) property.attribute(), generatedKeys.getObject(property.columnName()));
      }
    }
  }
}
