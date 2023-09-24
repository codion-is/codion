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
import is.codion.common.logging.MethodLogger;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static is.codion.common.db.connection.DatabaseConnection.SQL_STATE_NO_DATA;

abstract class AbstractQueriedKeyGenerator implements KeyGenerator {

  protected final <T> void selectAndPopulate(Entity entity, ColumnDefinition<T> primaryKeyColumn,
                                             DatabaseConnection databaseConnection) throws SQLException {
    MethodLogger methodLogger = databaseConnection.getMethodLogger();
    Connection connection = databaseConnection.getConnection();
    if (connection == null) {
      throw new IllegalStateException("No connection available when querying for key value");
    }
    String query = query(databaseConnection.database());
    if (query == null) {
      throw new IllegalStateException("Queried key generator returned no query");
    }
    SQLException exception = null;
    logEntry(methodLogger, query);
    try (PreparedStatement statement = connection.prepareStatement(query);
         ResultSet resultSet = statement.executeQuery()) {
      if (!resultSet.next()) {
        throw new SQLException("No rows returned when querying for a key value", SQL_STATE_NO_DATA);
      }

      entity.put(primaryKeyColumn.attribute(), primaryKeyColumn.get(resultSet, 1));
    }
    catch (SQLException e) {
      exception = e;
      throw e;
    }
    finally {
      logExit(methodLogger, exception);
      databaseConnection.database().queryCounter().select();
    }
  }

  protected abstract String query(Database database);

  private static void logEntry(MethodLogger methodLogger, Object argument) {
    if (methodLogger != null && methodLogger.isEnabled()) {
      methodLogger.enter("selectAndPopulate", argument);
    }
  }

  private static MethodLogger.Entry logExit(MethodLogger methodLogger, Throwable exception) {
    if (methodLogger != null && methodLogger.isEnabled()) {
      return methodLogger.exit("selectAndPopulate", exception);
    }

    return null;
  }
}
