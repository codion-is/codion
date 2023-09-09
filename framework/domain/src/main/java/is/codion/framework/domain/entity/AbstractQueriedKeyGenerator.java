/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
import java.util.List;

import static is.codion.common.db.connection.DatabaseConnection.SQL_STATE_NO_DATA;

abstract class AbstractQueriedKeyGenerator implements KeyGenerator {

  protected final <T> void selectAndPopulate(Entity entity, ColumnDefinition<T> columnDefinition,
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
    databaseConnection.database().countQuery(query);
    PreparedStatement statement = null;
    SQLException exception = null;
    ResultSet resultSet = null;
    logEntry(methodLogger, query);
    try {
      statement = connection.prepareStatement(query);
      resultSet = statement.executeQuery();
      List<T> result = columnDefinition.resultPacker().pack(resultSet, -1);
      if (result.isEmpty()) {
        throw new SQLException("No records returned when querying for a key value", SQL_STATE_NO_DATA);
      }

      entity.put(columnDefinition.attribute(), result.get(0));
    }
    catch (SQLException e) {
      exception = e;
      throw e;
    }
    finally {
      Database.closeSilently(statement);
      Database.closeSilently(resultSet);
      logExit(methodLogger, exception);
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
