/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.model.tools.MethodLogger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private static final Logger LOG = LoggerFactory.getLogger(DatabaseUtil.class);

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

  /**
   * A result packer for fetching longs from a result set containing a single long column
   */
  public static final ResultPacker<Long> LONG_RESULT_PACKER = new ResultPacker<Long>() {
    @Override
    public List<Long> pack(final ResultSet resultSet, final int fetchCount) throws SQLException {
      final List<Long> longs = new ArrayList<>();
      int counter = 0;
      while (resultSet.next() && (fetchCount < 0 || counter++ < fetchCount)) {
        longs.add(resultSet.getLong(1));
      }

      return longs;
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
      catch (final SQLException ignored) {/*ignored*/}
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
      catch (final SQLException ignored) {/*ignored*/}
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
      catch (final SQLException ignored) {/*ignored*/}
    }
  }

  /**
   * Performs the given query and returns the result as an integer
   * @param connection the connection
   * @param sql the query must select at least a single number column, any other subsequent columns are disregarded
   * @return the first record in the result as a integer
   * @throws SQLException thrown if anything goes wrong during the execution or if no record is returned
   */
  public static int queryInteger(final DatabaseConnection connection, final String sql) throws SQLException {
    final List<Integer> integers = query(connection, sql, INTEGER_RESULT_PACKER, -1);
    if (!integers.isEmpty()) {
      return integers.get(0);
    }

    throw new SQLException("No records returned when querying for an integer", sql);
  }

  /**
   * Performs the given query and returns the result as a long
   * @param connection the connection
   * @param sql the query must select at least a single number column, any other subsequent columns are disregarded
   * @return the first record in the result as a long
   * @throws SQLException thrown if anything goes wrong during the execution or if no record is returned
   */
  public static long queryLong(final DatabaseConnection connection, final String sql) throws SQLException {
    final List<Long> longs = query(connection, sql, LONG_RESULT_PACKER, -1);
    if (!longs.isEmpty()) {
      return longs.get(0);
    }

    throw new SQLException("No records returned when querying for a long", sql);
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
    catch (final SQLException e) {
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

  /**
   * Performs a query on the given connection and returns the result packed by the {@code resultPacker}
   * @param connection the connection
   * @param sql the sql query
   * @param resultPacker the result packer
   * @param fetchCount the number of records to fetch
   * @param <T> the type of object returned by the query
   * @return a List of records based on the given query
   * @throws SQLException thrown if anything goes wrong during the execution
   */
  private static <T> List<T> query(final DatabaseConnection connection, final String sql,
                                   final ResultPacker<T> resultPacker, final int fetchCount) throws SQLException {
    QUERY_COUNTER.count(sql);
    Statement statement = null;
    SQLException exception = null;
    ResultSet resultSet = null;
    final MethodLogger methodLogger = connection.getMethodLogger();
    try {
      if (methodLogger != null && methodLogger.isEnabled()) {
        methodLogger.logAccess("query", new Object[]{sql});
      }
      statement = connection.getConnection(false).createStatement();
      resultSet = statement.executeQuery(sql);

      return resultPacker.pack(resultSet, fetchCount);
    }
    catch (final SQLException e) {
      exception = e;
      throw e;
    }
    finally {
      closeSilently(statement);
      closeSilently(resultSet);
      if (methodLogger != null && methodLogger.isEnabled()) {
        final MethodLogger.Entry logEntry = methodLogger.logExit("query", exception, null);
        if (LOG != null && LOG.isDebugEnabled()) {
          LOG.debug(createLogMessage(connection.getUser(), sql, null, exception, logEntry));
        }
      }
    }
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
        catch (final SQLException ignored) {/*Not all databases have implemented this feature*/}
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
     * Counts the given query, based on its first character
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
