/*
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
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

  private static void logExit(MethodLogger methodLogger, Throwable exception) {
    if (methodLogger != null && methodLogger.isEnabled()) {
      methodLogger.exit("selectAndPopulate", exception);
    }
  }
}
