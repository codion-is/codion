/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
  public void afterInsert(final Entity entity, final List<ColumnProperty<?>> primaryKeyProperties,
                          final DatabaseConnection connection, final Statement insertStatement) throws SQLException {
    try (final ResultSet generatedKeys = insertStatement.getGeneratedKeys()) {
      if (generatedKeys.next()) {
        ColumnProperty<?> property = primaryKeyProperties.get(0);
        entity.put((Attribute<Object>) property.getAttribute(), generatedKeys.getObject(property.getColumnName()));
      }
    }
  }
}
