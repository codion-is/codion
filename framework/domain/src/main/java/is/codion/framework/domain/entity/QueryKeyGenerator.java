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
    ColumnDefinition<?> columnDefinition = entity.entityDefinition().primaryKeyColumnDefinitions().get(0);
    if (entity.isNull(columnDefinition.attribute())) {
      selectAndPopulate(entity, columnDefinition, connection);
    }
  }

  @Override
  protected String query(Database database) {
    return query;
  }
}
