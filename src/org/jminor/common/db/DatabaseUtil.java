/*
 * Copyright (c) 2004 - 2012, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.model.tools.MethodLogger;

import org.slf4j.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * A static utility class.
 */
public final class DatabaseUtil {

  /**
   * A result packer for fetching integers from a result set containing a single integer column
   */
  public static final ResultPacker<Integer> INTEGER_RESULT_PACKER = new ResultPacker<Integer>() {
    /** {@inheritDoc} */
    @Override
    public List<Integer> pack(final ResultSet resultSet, final int fetchCount) throws SQLException {
      final List<Integer> integers = new ArrayList<Integer>();
      int counter = 0;
      while (resultSet.next() && (fetchCount < 0 || counter++ < fetchCount)) {
        integers.add(resultSet.getInt(1));
      }

      return integers;
    }
  };

  private DatabaseUtil() {}

  /**
   * Closes the given ResultSet instances, swallowing any SQLExceptions that occur
   * @param resultSets the result sets to close
   */
  public static void closeSilently(final ResultSet... resultSets) {
    if (resultSets == null) {
      return;
    }
    for (final ResultSet resultSet : resultSets) {
      try {
        if (resultSet != null) {
          resultSet.close();
        }
      }
      catch (SQLException ignored) {}
    }
  }

  /**
   * Closes the given Statement instances, swallowing any SQLExceptions that occur
   * @param statements the statements to close
   */
  public static void closeSilently(final Statement... statements) {
    if (statements == null) {
      return;
    }
    for (final Statement statement : statements) {
      try {
        if (statement != null) {
          statement.close();
        }
      }
      catch (SQLException ignored) {}
    }
  }

  /**
   * Performs the given query and returns the result as an integer
   * @param connection the connection
   * @param sql the query must select at least a single number column, any other
   * subsequent columns are disregarded
   * @return the first record in the result as a integer
   * @throws java.sql.SQLException thrown if anything goes wrong during the execution or if no record is returned
   */
  public static int queryInteger(final DatabaseConnection connection,final String sql, final Logger log) throws SQLException {
    @SuppressWarnings("unchecked")
    final List<Integer> integers = (List<Integer>) query(connection, sql, INTEGER_RESULT_PACKER, -1, log);
    if (!integers.isEmpty()) {
      return integers.get(0);
    }

    throw new SQLException("No records returned when querying for an integer", sql);
  }

  /**
   * Executes the given sql query and returns the result in a List
   * @param connection the connection
   * @param sql the query
   * @param resultPacker a ResultPacker instance for creating the return List
   * @param fetchCount the number of records to retrieve, use -1 to retrieve all
   * @return the query result in a List
   * @throws java.sql.SQLException thrown if anything goes wrong during the query execution
   */
  public static List query(final DatabaseConnection connection, final String sql, final ResultPacker resultPacker,
                           final int fetchCount, final Logger log) throws SQLException {
    Databases.QUERY_COUNTER.count(sql);
//    connection.getMethodLogger().logAccess("query", new Object[] {sql});
    Statement statement = null;
    SQLException exception = null;
    ResultSet resultSet = null;
    try {
      statement = connection.getConnection().createStatement();
      resultSet = statement.executeQuery(sql);

      return resultPacker.pack(resultSet, fetchCount);
    }
    catch (SQLException e) {
      exception = e;
      throw e;
    }
    finally {
      closeSilently(statement);
      closeSilently(resultSet);
//      final MethodLogger.Entry logEntry = connection.getMethodLogger().logExit("query", exception, null);
      if (log != null && log.isDebugEnabled()) {
        log.debug(createLogMessage(connection.getUser(), sql, null, exception, null));
      }
    }
  }

  public static String createLogMessage(final User user, final String sqlStatement, final List<?> values, final Exception exception, final MethodLogger.Entry entry) {
    final StringBuilder logMessage = new StringBuilder(user.toString()).append("\n");
    if (entry == null) {
      logMessage.append(sqlStatement == null ? "no sql statement" : sqlStatement).append(", ").append(Util.getCollectionContentsAsString(values, false));
    }
    else {
      logMessage.append(entry.toString(1));
    }
    if (exception != null) {
      logMessage.append("\n").append(" [Exception: ").append(exception.getMessage()).append("]");
    }

    return logMessage.toString();
  }
}
