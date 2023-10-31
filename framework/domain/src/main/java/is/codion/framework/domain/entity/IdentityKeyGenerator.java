/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson.
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
      if (generatedKeys.next()) {
        ColumnDefinition<?> column = entity.definition().primaryKey().columnDefinitions().get(0);
        entity.put((Attribute<Object>) column.attribute(), generatedKeys.getObject(column.columnName()));
      }
    }
  }
}
