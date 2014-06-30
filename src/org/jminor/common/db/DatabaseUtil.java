/*
 * Copyright (c) 2004 - 2012, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.model.tools.MethodLogger;

import java.sql.Connection;
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
   * A synchronized query counter.
   * @see #getDatabaseStatistics()
   */
  public static final QueryCounter QUERY_COUNTER = new QueryCounter();

  /**
   * A result packer for fetching integers from a result set containing a single integer column
   */
  public static final ResultPacker<Integer> INTEGER_RESULT_PACKER = new ResultPacker<Integer>() {
    @Override
    public List<Integer> pack(final ResultSet resultSet, final int fetchCount) throws SQLException {
      final List<Integer> integers = new ArrayList<>();
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
   * Closes the given Connection instances, swallowing any SQLExceptions that occur
   * @param connections the connections to close
   */
  public static void closeSilently(final Connection... connections) {
    if (connections == null) {
      return;
    }
    for (final Connection connection : connections) {
      try {
        if (connection != null) {
          connection.close();
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
  public static int queryInteger(final DatabaseConnection connection,final String sql) throws SQLException {
    @SuppressWarnings("unchecked")
    final List<Integer> integers = (List<Integer>) connection.query(sql, INTEGER_RESULT_PACKER, -1);
    if (!integers.isEmpty()) {
      return integers.get(0);
    }

    throw new SQLException("No records returned when querying for an integer", sql);
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
  public static String createLogMessage(final User user, final String sqlStatement, final List<?> values,
                                        final Exception exception, final MethodLogger.Entry entry) {
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

  /**
   * Returns true if the given connection is valid, if the underlying driver supports <code>isValid()</code>
   * it is used, otherwise a simple query is run
   * @param connection the connection to validate
   * @param database the underlying database implementation
   * @param timeoutInSeconds the timeout
   * @return true if the connection is valid
   */
  public static boolean isValid(final Connection connection, final Database database, final int timeoutInSeconds) {
    Util.rejectNullValue(connection, "connection");
    Util.rejectNullValue(database, "database");
    try {
      if (database.supportsIsValid()) {
        return connection.isValid(timeoutInSeconds);
      }

      return validateWithQuery(connection, database, timeoutInSeconds);
    }
    catch (SQLException e) {
      return false;
    }
  }

  /**
   * Returns the statistics gathered via {@link #QUERY_COUNTER}.
   * @return a DatabaseStatistics object containing query statistics collected since
   * the last time this function was called.
   */
  public static Database.Statistics getDatabaseStatistics() {
    QUERY_COUNTER.updateQueriesPerSecond();
    return new Databases.DatabaseStatistics(QUERY_COUNTER.getQueriesPerSecond(),
            QUERY_COUNTER.getSelectsPerSecond(), QUERY_COUNTER.getInsertsPerSecond(),
            QUERY_COUNTER.getDeletesPerSecond(), QUERY_COUNTER.getUpdatesPerSecond());
  }

  private static boolean validateWithQuery(final Connection connection, final Database database,
                                           final int timeoutInSeconds) throws SQLException {
    ResultSet rs = null;
    Statement statement = null;
    try {
      statement = connection.createStatement();
      if (timeoutInSeconds > 0) {
        try {
          statement.setQueryTimeout(timeoutInSeconds);
        }
        catch (SQLException ignored) {/*Not all databases have implemented this feature*/}
      }
      rs = statement.executeQuery(database.getCheckConnectionQuery());

      return true;
    }
    finally {
      closeSilently(rs);
      closeSilently(statement);
    }
  }

  /**
   * A class for counting query types, providing averages over time
   */
  public static final class QueryCounter {

    private static final double THOUSAND = 1000d;

    private long queriesPerSecondTime = System.currentTimeMillis();
    private int queriesPerSecond = 0;
    private int queriesPerSecondCounter = 0;
    private int selectsPerSecond = 0;
    private int selectsPerSecondCounter = 0;
    private int insertsPerSecond = 0;
    private int insertsPerSecondCounter = 0;
    private int updatesPerSecond = 0;
    private int updatesPerSecondCounter = 0;
    private int deletesPerSecond = 0;
    private int deletesPerSecondCounter = 0;
    private int undefinedPerSecond = 0;
    private int undefinedPerSecondCounter = 0;

    /**
     * Counts the given query, based on it's first character
     * @param sql the sql query
     */
    public synchronized void count(final String sql) {
      queriesPerSecondCounter++;
      switch (Character.toLowerCase(sql.charAt(0))) {
        case 's':
          selectsPerSecondCounter++;
          break;
        case 'i':
          insertsPerSecondCounter++;
          break;
        case 'u':
          updatesPerSecondCounter++;
          break;
        case 'd':
          deletesPerSecondCounter++;
          break;
        default:
          undefinedPerSecondCounter++;
      }
    }

    /**
     * @return the number of queries being run per second
     */
    public synchronized int getQueriesPerSecond() {
      return queriesPerSecond;
    }

    /**
     * @return the number of select queries being run per second
     */
    public synchronized int getSelectsPerSecond() {
      return selectsPerSecond;
    }

    /**
     * @return the number of delete queries being run per second
     */
    public synchronized int getDeletesPerSecond() {
      return deletesPerSecond;
    }

    /**
     * @return the number of insert queries being run per second
     */
    public synchronized int getInsertsPerSecond() {
      return insertsPerSecond;
    }

    /**
     * @return the number of update queries being run per second
     */
    public synchronized int getUpdatesPerSecond() {
      return updatesPerSecond;
    }

    /**
     * @return the number of undefined queries being run per second
     */
    public synchronized int getUndefinedPerSecond() {
      return undefinedPerSecond;
    }

    private synchronized void updateQueriesPerSecond() {
      final long current = System.currentTimeMillis();
      final double seconds = (current - queriesPerSecondTime) / THOUSAND;
      if (seconds > 0) {
        queriesPerSecond = (int) (queriesPerSecondCounter / seconds);
        selectsPerSecond = (int) (selectsPerSecondCounter / seconds);
        insertsPerSecond = (int) (insertsPerSecondCounter / seconds);
        deletesPerSecond = (int) (deletesPerSecondCounter / seconds);
        updatesPerSecond = (int) (updatesPerSecondCounter / seconds);
        undefinedPerSecond = (int) (undefinedPerSecondCounter / seconds);
        queriesPerSecondCounter = 0;
        selectsPerSecondCounter = 0;
        insertsPerSecondCounter = 0;
        deletesPerSecondCounter = 0;
        updatesPerSecondCounter = 0;
        undefinedPerSecondCounter = 0;
        queriesPerSecondTime = current;
      }
    }
  }
}
