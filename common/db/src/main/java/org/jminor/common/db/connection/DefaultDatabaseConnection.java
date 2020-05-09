/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.connection;

import org.jminor.common.MethodLogger;
import org.jminor.common.db.database.Database;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.result.ResultPacker;
import org.jminor.common.user.User;
import org.jminor.common.user.Users;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

/**
 * A default DatabaseConnection implementation, which wraps a standard JDBC Connection object.
 * This class is not thread-safe.
 */
final class DefaultDatabaseConnection implements DatabaseConnection {

  private static final ResultPacker<Integer> INTEGER_RESULT_PACKER = resultSet -> resultSet.getInt(1);
  private static final ResultPacker<Long> LONG_RESULT_PACKER = resultSet -> resultSet.getLong(1);

  private static final Map<String, User> META_DATA_USER_CACHE = new ConcurrentHashMap<>();

  private final User user;
  private final Database database;

  private Connection connection;
  private boolean transactionOpen = false;

  private MethodLogger methodLogger;

  /**
   * Constructs a new DefaultDatabaseConnection instance, initialized and ready for use.
   * @param database the database
   * @param user the user to base this database connection on
   * @throws DatabaseException in case there is a problem connecting to the database
   * @throws org.jminor.common.db.exception.AuthenticationException in case of an authentication error
   */
  DefaultDatabaseConnection(final Database database, final User user) throws DatabaseException {
    this.database = requireNonNull(database, "database");
    this.connection = disableAutoCommit(database.createConnection(user));
    this.user = requireNonNull(user, "user");
  }

  /**
   * Constructs a new DefaultDatabaseConnection instance, based on the given Connection object.
   * NB. auto commit is disabled on the Connection that is provided.
   * @param database the database
   * @param connection the Connection object to base this DefaultDatabaseConnection on
   * @throws IllegalArgumentException in case the given connection is invalid
   * @throws DatabaseException in case of an exception while retrieving the username from the connection meta data
   */
  DefaultDatabaseConnection(final Database database, final Connection connection) throws DatabaseException {
    this.database = requireNonNull(database, "database");
    this.connection = disableAutoCommit(connection);
    this.user = getUser(connection);
  }

  @Override
  public void close() throws Exception {
    disconnect();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + ": " + user.getUsername();
  }

  @Override
  public User getUser() {
    return user;
  }

  @Override
  public void setMethodLogger(final MethodLogger methodLogger) {
    this.methodLogger = methodLogger;
  }

  @Override
  public MethodLogger getMethodLogger() {
    return methodLogger;
  }

  @Override
  public void disconnect() {
    try {
      if (connection != null && !connection.isClosed()) {
        connection.rollback();
      }
    }
    catch (final SQLException ex) {
      System.err.println("DefaultDatabaseConnection.disconnect(), connection invalid");
    }
    Database.closeSilently(connection);
    connection = null;
  }

  @Override
  public boolean isConnected() {
    return connection != null && database.isConnectionValid(connection);
  }

  @Override
  public void setConnection(final Connection connection) {
    this.connection = connection;
  }

  @Override
  public Connection getConnection() {
    return connection;
  }

  @Override
  public Database getDatabase() {
    return database;
  }

  @Override
  public int selectInteger(final String sql) throws SQLException {
    final List<Integer> integers = select(sql, INTEGER_RESULT_PACKER, -1);
    if (!integers.isEmpty()) {
      return integers.get(0);
    }

    throw new SQLException("No records returned when querying for an integer", sql);
  }

  @Override
  public long selectLong(final String sql) throws SQLException {
    final List<Long> longs = select(sql, LONG_RESULT_PACKER, -1);
    if (!longs.isEmpty()) {
      return longs.get(0);
    }

    throw new SQLException("No records returned when querying for a long", sql);
  }

  @Override
  public void beginTransaction() {
    if (transactionOpen) {
      throw new IllegalStateException("Transaction already open");
    }

    logAccess("beginTransaction", new Object[0]);
    transactionOpen = true;
    logExit("beginTransaction", null);
  }

  @Override
  public void rollbackTransaction() {
    SQLException exception = null;
    try {
      if (!transactionOpen) {
        throw new IllegalStateException("Transaction is not open");
      }

      logAccess("rollbackTransaction", null);
      connection.rollback();
    }
    catch (final SQLException e) {
      exception = e;
    }
    finally {
      transactionOpen = false;
      logExit("rollbackTransaction", exception);
    }
  }

  @Override
  public void commitTransaction() {
    SQLException exception = null;
    try {
      if (!transactionOpen) {
        throw new IllegalStateException("Transaction is not open");
      }

      logAccess("commitTransaction", null);
      connection.commit();
    }
    catch (final SQLException e) {
      exception = e;
    }
    finally {
      transactionOpen = false;
      logExit("commitTransaction", exception);
    }
  }

  @Override
  public boolean isTransactionOpen() {
    return transactionOpen;
  }

  @Override
  public void commit() throws SQLException {
    if (transactionOpen) {
      throw new IllegalStateException("Can not perform a commit during an open transaction, use 'commitTransaction()'");
    }

    SQLException exception = null;
    try {
      logAccess("commit", null);
      connection.commit();
    }
    catch (final SQLException e) {
      System.err.println("Exception during commit: " + user.getUsername() + ": " + e.getMessage());
      exception = e;
      throw e;
    }
    finally {
      logExit("commit", exception);
    }
  }

  @Override
  public void rollback() throws SQLException {
    if (transactionOpen) {
      throw new IllegalStateException("Can not perform a rollback during an open transaction, use 'rollbackTransaction()'");
    }

    logAccess("rollback", null);
    SQLException exception = null;
    try {
      connection.rollback();
    }
    catch (final SQLException e) {
      exception = e;
      throw e;
    }
    finally {
      logExit("rollback", exception);
    }
  }

  /**
   * Performs a query and returns the result packed by the {@code resultPacker}
   * @param sql the sql query
   * @param resultPacker the result packer
   * @param fetchCount the maximum number of records to fetch
   * @param <T> the type of object returned by the query
   * @return a List of records based on the given query
   * @throws SQLException thrown if anything goes wrong during the execution
   */
  private <T> List<T> select(final String sql, final ResultPacker<T> resultPacker, final int fetchCount) throws SQLException {
    requireNonNull(resultPacker, "resultPacker");
    database.countQuery(requireNonNull(sql, "sql"));
    Statement statement = null;
    SQLException exception = null;
    ResultSet resultSet = null;
    try {
      logAccess("query", new Object[] {sql});
      statement = connection.createStatement();
      resultSet = statement.executeQuery(sql);

      return resultPacker.pack(resultSet, fetchCount);
    }
    catch (final SQLException e) {
      exception = e;
      throw e;
    }
    finally {
      Database.closeSilently(statement);
      Database.closeSilently(resultSet);
      logExit("query", exception);
    }
  }

  private void logAccess(final String method, final Object[] arguments) {
    if (methodLogger != null && methodLogger.isEnabled()) {
      methodLogger.logAccess(method, arguments);
    }
  }

  private MethodLogger.Entry logExit(final String method, final Throwable exception) {
    if (methodLogger != null && methodLogger.isEnabled()) {
      return methodLogger.logExit(method, exception);
    }

    return null;
  }

  /**
   * Disables auto-commit on the given connection and returns it.
   * @param connection the connection
   * @return the connection with auto-commit disabled
   * @throws DatabaseException in case disabling auto-commit fails
   */
  private static Connection disableAutoCommit(final Connection connection) throws DatabaseException {
    requireNonNull(connection, "connection");
    try {
      connection.setAutoCommit(false);

      return connection;
    }
    catch (final SQLException e) {
      System.err.println("Unable to disable auto commit on connection, assuming invalid state");
      throw new DatabaseException(e, "Connection invalid during instantiation");
    }
  }

  /**
   * Returns a User with the username from the meta data retrieved from the given connection
   * @param connection the connection
   * @return a user based on the information gleamed from the given connection
   * @throws DatabaseException in case of an exception while retrieving the username from the connection meta data
   * @see java.sql.DatabaseMetaData#getUserName()
   */
  private static User getUser(final Connection connection) throws DatabaseException {
    try {
      return META_DATA_USER_CACHE.computeIfAbsent(connection.getMetaData().getUserName(), Users::user);
    }
    catch (final SQLException e) {
      throw new DatabaseException(e, "Exception while trying to retrieve username from meta data");
    }
  }
}