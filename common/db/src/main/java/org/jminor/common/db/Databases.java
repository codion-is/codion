/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.MethodLogger;
import org.jminor.common.TextUtil;
import org.jminor.common.User;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;

/**
 * Utility class for {@link Database} implementations and misc. database related things.
 * @see Database#DATABASE_TYPE
 */
public final class Databases {

  /**
   * A synchronized query counter.
   * @see #getDatabaseStatistics()
   */
  public static final QueryCounter QUERY_COUNTER = new QueryCounter();

  /**
   * A result packer for fetching integers from a result set containing a single integer column
   */
  public static final ResultPacker<Integer> INTEGER_RESULT_PACKER = resultSet -> resultSet.getInt(1);

  /**
   * A result packer for fetching longs from a result set containing a single long column
   */
  public static final ResultPacker<Long> LONG_RESULT_PACKER = resultSet -> resultSet.getLong(1);

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
      final Database.Type currentType = Database.getDatabaseType();
      final ServiceLoader<Database> loader = ServiceLoader.load(Database.class);
      if (instance == null || !instance.getType().equals(currentType)) {
        //refresh the instance
        instance = findDatabaseServiceOfType(loader, currentType);
      }

      return instance;
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
    Objects.requireNonNull(connection, "connection");
    Objects.requireNonNull(database, "database");
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
   * @return a {@link Database.Statistics} object containing query statistics collected since
   * the last time this function was called.
   */
  public static Database.Statistics getDatabaseStatistics() {
    QUERY_COUNTER.updateQueriesPerSecond();
    return new DatabaseStatistics(QUERY_COUNTER.getQueriesPerSecond(),
            QUERY_COUNTER.getSelectsPerSecond(), QUERY_COUNTER.getInsertsPerSecond(),
            QUERY_COUNTER.getDeletesPerSecond(), QUERY_COUNTER.getUpdatesPerSecond());
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
      logMessage.append(sqlStatement == null ? "no sql statement" : sqlStatement).append(", ").append(TextUtil.getCollectionContentsAsString(values, false));
    }
    else {
      logMessage.append(entry.toString(1));
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

  private static Database findDatabaseServiceOfType(final ServiceLoader<Database> loader, final Database.Type type) {
    for (final Database database : loader) {
      if (database.getType().equals(type)) {
        return database;
      }
    }

    throw new IllegalArgumentException("No database implementation available for type: " + type);
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

  /**
   * A default Database.Statistics implementation.
   */
  static final class DatabaseStatistics implements Database.Statistics, Serializable {

    private static final long serialVersionUID = 1;

    private final long timestamp = System.currentTimeMillis();
    private final int queriesPerSecond;
    private final int selectsPerSecond;
    private final int insertsPerSecond;
    private final int deletesPerSecond;
    private final int updatesPerSecond;

    /**
     * Instantiates a new DatabaseStatistics object
     * @param queriesPerSecond the number of queries being run per second
     * @param selectsPerSecond the number of select queries being run per second
     * @param insertsPerSecond the number of insert queries being run per second
     * @param deletesPerSecond the number of delete queries being run per second
     * @param updatesPerSecond the number of update queries being run per second
     */
    DatabaseStatistics(final int queriesPerSecond, final int selectsPerSecond, final int insertsPerSecond,
                       final int deletesPerSecond, final int updatesPerSecond) {
      this.queriesPerSecond = queriesPerSecond;
      this.selectsPerSecond = selectsPerSecond;
      this.insertsPerSecond = insertsPerSecond;
      this.deletesPerSecond = deletesPerSecond;
      this.updatesPerSecond = updatesPerSecond;
    }

    @Override
    public int getQueriesPerSecond() {
      return queriesPerSecond;
    }

    @Override
    public int getDeletesPerSecond() {
      return deletesPerSecond;
    }

    @Override
    public int getInsertsPerSecond() {
      return insertsPerSecond;
    }

    @Override
    public int getSelectsPerSecond() {
      return selectsPerSecond;
    }

    @Override
    public int getUpdatesPerSecond() {
      return updatesPerSecond;
    }

    @Override
    public long getTimestamp() {
      return timestamp;
    }
  }
}