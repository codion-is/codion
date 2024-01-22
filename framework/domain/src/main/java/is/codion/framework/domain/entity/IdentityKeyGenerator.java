/*
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.common.db.connection.DatabaseConnection;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

final class IdentityKeyGenerator implements KeyGenerator {

  @Override
  public boolean inserted() {
    return false;
  }

  @Override
  public boolean returnGeneratedKeys() {
    return true;
  }

  @Override
  public void afterInsert(Entity entity, DatabaseConnection connection, Statement insertStatement) throws SQLException {
    try (ResultSet generatedKeys = insertStatement.getGeneratedKeys()) {
      if (!generatedKeys.next()) {
        throw new SQLException("Identity key generator returned no generated keys", DatabaseConnection.SQL_STATE_NO_DATA);
      }
      ColumnDefinition<Object> column = (ColumnDefinition<Object>) entity.definition().primaryKey().definitions().get(0);
      // must fetch value by column name, since some databases (PostgreSQL for example), return all columns, not just generated ones
      entity.put(column.attribute(), column.prepareValue(generatedKeys.getObject(column.name())));
    }
  }
}
