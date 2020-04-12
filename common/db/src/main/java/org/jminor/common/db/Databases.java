/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.MethodLogger;
import org.jminor.common.user.User;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Utility class for {@link Database} implementations and misc. database related things.
 * @see Database#DATABASE_TYPE
 */
public final class Databases {

  private static Database instance;

  private Databases() {}

  /**
   * @return a Database instance based on the current runtime database type property
   * @see Database#DATABASE_TYPE
   * @see Database#getDatabaseType()
   * @throws IllegalArgumentException in case an unsupported database type is specified
   * @throws RuntimeException in case of an exception occurring while instantiating the database implementation instance
   */
  public static synchronized Database getInstance() {
    try {
      if (Databases.instance == null || !Databases.instance.getType().equals(Database.getDatabaseType())) {
        //refresh the instance
        Databases.instance = DatabaseProvider.getInstance().createDatabase();
      }

      return Databases.instance;
    }
    catch (final IllegalArgumentException e) {
      throw e;
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Closes the given ResultSet instance, swallowing any SQLExceptions that occur
   * @param resultSet the result set to close
   */
  public static void closeSilently(final ResultSet resultSet) {
    try {
      if (resultSet != null) {
        resultSet.close();
      }
    }
    catch (final SQLException ignored) {/*ignored*/}
  }

  /**
   * Closes the given Statement instance, swallowing any SQLExceptions that occur
   * @param statement the statement to close
   */
  public static void closeSilently(final Statement statement) {
    try {
      if (statement != null) {
        statement.close();
      }
    }
    catch (final SQLException ignored) {/*ignored*/}
  }

  /**
   * Closes the given Connection instance, swallowing any SQLExceptions that occur
   * @param connection the connection to close
   */
  public static void closeSilently(final Connection connection) {
    try {
      if (connection != null) {
        connection.close();
      }
    }
    catch (final SQLException ignored) {/*ignored*/}
  }

  /**
   * Returns true if the given connection is valid, if the underlying driver supports {@code isValid()}
   * it is used, otherwise a simple query is run
   * @param connection the connection to validate
   * @param database the underlying database implementation
   * @param timeoutInSeconds the timeout
   * @return true if the connection is valid
   */
  public static boolean isValid(final Connection connection, final Database database, final int timeoutInSeconds) {
    requireNonNull(connection, "connection");
    requireNonNull(database, "database");
    try {
      if (database.supportsIsValid()) {
        return connection.isValid(timeoutInSeconds);
      }

      return validateWithQuery(connection, database, timeoutInSeconds);
    }
    catch (final SQLException e) {
      return false;
    }
  }

  /**
   * Creates a log message from the given information
   * @param user the user
   * @param sqlStatement the sql statement
   * @param values the values
   * @param exception the exception, if any
   * @param entry the log entry
   * @return a log message
   */
  public static String createLogMessage(final User user, final String sqlStatement, final List values,
                                        final Exception exception, final MethodLogger.Entry entry) {
    final StringBuilder logMessage = new StringBuilder(user.toString()).append("\n");
    if (entry == null) {
      logMessage.append(sqlStatement == null ? "no sql statement" : sqlStatement).append(", ").append(values);
    }
    else {
      entry.append(logMessage);
    }
    if (exception != null) {
      logMessage.append("\n").append(" [Exception: ").append(exception.getMessage()).append("]");
    }

    return logMessage.toString();
  }

  private static boolean validateWithQuery(final Connection connection, final Database database,
                                           final int timeoutInSeconds) throws SQLException {
    ResultSet rs = null;
    try (final Statement statement = connection.createStatement()) {
      if (timeoutInSeconds > 0) {
        try {
          statement.setQueryTimeout(timeoutInSeconds);
        }
        catch (final SQLException ignored) {/*Not all databases have implemented this feature*/}
      }
      rs = statement.executeQuery(database.getCheckConnectionQuery());

      return true;
    }
    finally {
      closeSilently(rs);
    }
  }
}