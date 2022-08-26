/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.connection;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.result.ResultPacker;
import is.codion.common.logging.MethodLogger;
import is.codion.common.user.User;

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
   * @throws is.codion.common.db.exception.AuthenticationException in case of an authentication error
   */
  DefaultDatabaseConnection(Database database, User user) throws DatabaseException {
    this.database = requireNonNull(database, "database");
    this.connection = disableAutoCommit(database.createConnection(user));
    this.user = requireNonNull(user, "user");
  }

  /**
   * Constructs a new DefaultDatabaseConnection instance, based on the given Connection object.
   * NB. auto commit is disabled on the Connection that is provided.
   * @param database the database
   * @param connection the Connection object to base this DefaultDatabaseConnection on
   * @throws DatabaseException in case of an exception while retrieving the username from the connection meta-data
   */
  DefaultDatabaseConnection(Database database, Connection connection) throws DatabaseException {
    this.database = requireNonNull(database, "database");
    this.connection = disableAutoCommit(connection);
    this.user = user(connection);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + ": " + user.username();
  }

  @Override
  public User user() {
    return user;
  }

  @Override
  public void setMethodLogger(MethodLogger methodLogger) {
    this.methodLogger = methodLogger;
  }

  @Override
  public MethodLogger getMethodLogger() {
    return methodLogger;
  }

  @Override
  public void close() {
    try {
      if (connection != null && !connection.isClosed()) {
        connection.rollback();
      }
    }
    catch (SQLException ex) {
      System.err.println("DefaultDatabaseConnection.close(), connection invalid");
    }
    Database.closeSilently(connection);
    connection = null;
    transactionOpen = false;
  }

  @Override
  public boolean isConnected() {
    return connection != null && database.isConnectionValid(connection);
  }

  @Override
  public void setConnection(Connection connection) {
    this.connection = connection;
  }

  @Override
  public Connection getConnection() {
    return connection;
  }

  @Override
  public Database database() {
    return database;
  }

  @Override
  public int selectInteger(String sql) throws SQLException {
    List<Integer> integers = select(sql, INTEGER_RESULT_PACKER, 1);
    if (!integers.isEmpty()) {
      return integers.get(0);
    }

    throw new SQLException("No records returned when querying for an integer", SQL_STATE_NO_DATA);
  }

  @Override
  public long selectLong(String sql) throws SQLException {
    List<Long> longs = select(sql, LONG_RESULT_PACKER, 1);
    if (!longs.isEmpty()) {
      return longs.get(0);
    }

    throw new SQLException("No records returned when querying for a long", SQL_STATE_NO_DATA);
  }

  @Override
  public void beginTransaction() {
    verifyOpenConnection();
    if (transactionOpen) {
      throw new IllegalStateException("Transaction already open");
    }

    logAccess("beginTransaction");
    transactionOpen = true;
    logExit("beginTransaction", null);
  }

  @Override
  public void rollbackTransaction() {
    verifyOpenConnection();
    SQLException exception = null;
    try {
      if (!transactionOpen) {
        throw new IllegalStateException("Transaction is not open");
      }

      logAccess("rollbackTransaction");
      connection.rollback();
    }
    catch (SQLException e) {
      exception = e;
    }
    finally {
      transactionOpen = false;
      logExit("rollbackTransaction", exception);
    }
  }

  @Override
  public void commitTransaction() {
    verifyOpenConnection();
    SQLException exception = null;
    try {
      if (!transactionOpen) {
        throw new IllegalStateException("Transaction is not open");
      }

      logAccess("commitTransaction");
      connection.commit();
    }
    catch (SQLException e) {
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
    verifyOpenConnection();
    if (transactionOpen) {
      throw new IllegalStateException("Can not perform a commit during an open transaction, use 'commitTransaction()'");
    }

    SQLException exception = null;
    try {
      logAccess("commit");
      connection.commit();
    }
    catch (SQLException e) {
      System.err.println("Exception during commit: " + user.username() + ": " + e.getMessage());
      exception = e;
      throw e;
    }
    finally {
      logExit("commit", exception);
    }
  }

  @Override
  public void rollback() throws SQLException {
    verifyOpenConnection();
    if (transactionOpen) {
      throw new IllegalStateException("Can not perform a rollback during an open transaction, use 'rollbackTransaction()'");
    }

    logAccess("rollback");
    SQLException exception = null;
    try {
      connection.rollback();
    }
    catch (SQLException e) {
      exception = e;
      throw e;
    }
    finally {
      logExit("rollback", exception);
    }
  }

  private <T> List<T> select(String sql, ResultPacker<T> resultPacker, int fetchLimit) throws SQLException {
    verifyOpenConnection();
    database.countQuery(requireNonNull(sql, "sql"));
    Statement statement = null;
    SQLException exception = null;
    ResultSet resultSet = null;
    try {
      logAccess("select", sql);
      statement = connection.createStatement();
      resultSet = statement.executeQuery(sql);

      return resultPacker.pack(resultSet, fetchLimit);
    }
    catch (SQLException e) {
      exception = e;
      throw e;
    }
    finally {
      Database.closeSilently(statement);
      Database.closeSilently(resultSet);
      logExit("select", exception);
    }
  }

  private void logAccess(String method) {
    if (methodLogger != null && methodLogger.isEnabled()) {
      methodLogger.logAccess(method);
    }
  }

  private void logAccess(String method, Object argument) {
    if (methodLogger != null && methodLogger.isEnabled()) {
      methodLogger.logAccess(method, argument);
    }
  }

  private MethodLogger.Entry logExit(String method, Throwable exception) {
    if (methodLogger != null && methodLogger.isEnabled()) {
      return methodLogger.logExit(method, exception);
    }

    return null;
  }

  private void verifyOpenConnection() {
    if (connection == null) {
      throw new IllegalStateException("Connection is closed");
    }
  }

  /**
   * Disables auto-commit on the given connection and returns it.
   * @param connection the connection
   * @return the connection with auto-commit disabled
   * @throws DatabaseException in case disabling auto-commit fails
   */
  private static Connection disableAutoCommit(Connection connection) throws DatabaseException {
    requireNonNull(connection, "connection");
    try {
      connection.setAutoCommit(false);

      return connection;
    }
    catch (SQLException e) {
      System.err.println("Unable to disable auto commit on connection, assuming invalid state");
      throw new DatabaseException(e, "Connection invalid during instantiation");
    }
  }

  /**
   * Returns a User with the username from the meta-data retrieved from the given connection
   * @param connection the connection
   * @return a user based on the information gleamed from the given connection
   * @throws DatabaseException in case of an exception while retrieving the username from the connection meta-data
   * @see java.sql.DatabaseMetaData#getUserName()
   */
  private static User user(Connection connection) throws DatabaseException {
    try {
      return META_DATA_USER_CACHE.computeIfAbsent(connection.getMetaData().getUserName(), User::user);
    }
    catch (SQLException e) {
      throw new DatabaseException(e, "Exception while trying to retrieve username from meta data");
    }
  }
}