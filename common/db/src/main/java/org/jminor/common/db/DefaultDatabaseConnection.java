/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.MethodLogger;
import org.jminor.common.User;
import org.jminor.common.db.exception.DatabaseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * A default DatabaseConnection implementation, which wraps a standard JDBC Connection object.
 * This class is not thread-safe.
 */
final class DefaultDatabaseConnection implements DatabaseConnection {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultDatabaseConnection.class);

  private static final ResultPacker<Integer> INTEGER_RESULT_PACKER = resultSet -> resultSet.getInt(1);
  private static final ResultPacker<Long> LONG_RESULT_PACKER = resultSet -> resultSet.getLong(1);

  /**
   * The default timoeout in seconds when checking if this connection is valid
   */
  static final int DEFAULT_VALIDITY_CHECK_TIMEOUT = 2;

  private final User user;
  private final Database database;
  private final int validityCheckTimeout;

  private Connection connection;
  private boolean transactionOpen = false;

  private MethodLogger methodLogger;

  /**
   * Constructs a new DefaultDatabaseConnection instance, initialized and ready for use,
   * using {@link DefaultDatabaseConnection#DEFAULT_VALIDITY_CHECK_TIMEOUT} as the validity check timeout.
   * @param database the database
   * @param user the user to base this database connection on
   * @throws DatabaseException in case there is a problem connecting to the database
   */
  DefaultDatabaseConnection(final Database database, final User user) throws DatabaseException {
    this(database, user, DEFAULT_VALIDITY_CHECK_TIMEOUT);
  }

  /**
   * Constructs a new DefaultDatabaseConnection instance, initialized and ready for use
   * @param database the database
   * @param user the user to base this database connection on
   * @param validityCheckTimeout the timoeout in seconds when checking if this connection is valid
   * @throws DatabaseException in case there is a problem connecting to the database
   * @throws org.jminor.common.db.exception.AuthenticationException in case of an authentication error
   */
  DefaultDatabaseConnection(final Database database, final User user,
                            final int validityCheckTimeout) throws DatabaseException {
    this.database = requireNonNull(database, "database");
    this.user = requireNonNull(user, "user");
    this.validityCheckTimeout = validityCheckTimeout;
    initialize(database.createConnection(user));
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
    this(database, connection, 0);
  }

  /**
   * Constructs a new DefaultDatabaseConnection instance, based on the given Connection object.
   * NB. auto commit is disabled on the Connection that is provided.
   * @param database the database
   * @param connection the Connection object to base this DefaultDatabaseConnection on, it is assumed to be in a valid state
   * @param validityCheckTimeout the number of seconds specified when checking if this connection is valid
   * @throws IllegalArgumentException in case the given connection is invalid
   * @throws DatabaseException in case of an exception while retrieving the username from the connection meta data
   */
  DefaultDatabaseConnection(final Database database, final Connection connection,
                            final int validityCheckTimeout) throws DatabaseException {
    this.database = requireNonNull(database, "database");
    this.validityCheckTimeout = validityCheckTimeout;
    initialize(requireNonNull(connection, "connection"));
    this.user = getUser(connection);
  }

  /** {@inheritDoc} */
  @Override
  public void close() throws Exception {
    disconnect();
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return getClass().getSimpleName() + ": " + user.getUsername();
  }

  /** {@inheritDoc} */
  @Override
  public User getUser() {
    return user;
  }

  /** {@inheritDoc} */
  @Override
  public void setMethodLogger(final MethodLogger methodLogger) {
    this.methodLogger = methodLogger;
  }

  /** {@inheritDoc} */
  @Override
  public MethodLogger getMethodLogger() {
    return methodLogger;
  }

  /** {@inheritDoc} */
  @Override
  public void disconnect() {
    try {
      if (connection != null && !connection.isClosed()) {
        connection.rollback();
      }
    }
    catch (final SQLException ex) {
      LOG.warn("DefaultDatabaseConnection.disconnect(), connection invalid", ex);
    }
    Databases.closeSilently(connection);
    connection = null;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isConnected() {
    return connection != null && Databases.isValid(connection, database, validityCheckTimeout);
  }

  /** {@inheritDoc} */
  @Override
  public void setConnection(final Connection connection) {
    this.connection = connection;
  }

  /** {@inheritDoc} */
  @Override
  public Connection getConnection() {
    return connection;
  }

  /** {@inheritDoc} */
  @Override
  public Database getDatabase() {
    return database;
  }

  /** {@inheritDoc} */
  @Override
  public int queryInteger(final String sql) throws SQLException {
    final List<Integer> integers = query(sql, INTEGER_RESULT_PACKER, -1);
    if (!integers.isEmpty()) {
      return integers.get(0);
    }

    throw new SQLException("No records returned when querying for an integer", sql);
  }

  /** {@inheritDoc} */
  @Override
  public long queryLong(final String sql) throws SQLException {
    final List<Long> longs = query(sql, LONG_RESULT_PACKER, -1);
    if (!longs.isEmpty()) {
      return longs.get(0);
    }

    throw new SQLException("No records returned when querying for a long", sql);
  }

  /** {@inheritDoc} */
  @Override
  public void beginTransaction() {
    if (transactionOpen) {
      throw new IllegalStateException("Transaction already open");
    }

    LOG.debug("{}: begin transaction;", user.getUsername());
    logAccess("beginTransaction", new Object[0]);
    transactionOpen = true;
    logExit("beginTransaction", null);
  }

  /** {@inheritDoc} */
  @Override
  public void rollbackTransaction() {
    SQLException exception = null;
    try {
      if (!transactionOpen) {
        throw new IllegalStateException("Transaction is not open");
      }

      LOG.debug("{}: rollback transaction;", user.getUsername());
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

  /** {@inheritDoc} */
  @Override
  public void commitTransaction() {
    SQLException exception = null;
    try {
      if (!transactionOpen) {
        throw new IllegalStateException("Transaction is not open");
      }

      LOG.debug("{}: commit transaction;", user.getUsername());
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

  /** {@inheritDoc} */
  @Override
  public boolean isTransactionOpen() {
    return transactionOpen;
  }

  /** {@inheritDoc} */
  @Override
  public void commit() throws SQLException {
    if (transactionOpen) {
      throw new IllegalStateException("Can not perform a commit during an open transaction, use 'commitTransaction()'");
    }

    LOG.debug("{}: commit;", user.getUsername());
    SQLException exception = null;
    try {
      logAccess("commit", null);
      connection.commit();
    }
    catch (final SQLException e) {
      LOG.error("Exception during commit: " + user.getUsername(), e);
      exception = e;
      throw e;
    }
    finally {
      logExit("commit", exception);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void rollback() throws SQLException {
    if (transactionOpen) {
      throw new IllegalStateException("Can not perform a rollback during an open transaction, use 'rollbackTransaction()'");
    }

    LOG.debug("{}: rollback;", user.getUsername());
    logAccess("rollback", null);
    SQLException exception = null;
    try {
      connection.rollback();
    }
    catch (final SQLException e) {
      LOG.error("Exception during rollback: " + user.getUsername(), e);
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
  private <T> List<T> query(final String sql, final ResultPacker<T> resultPacker, final int fetchCount) throws SQLException {
    requireNonNull(resultPacker, "resultPacker");
    Databases.QUERY_COUNTER.count(requireNonNull(sql, "sql"));
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
      Databases.closeSilently(statement);
      Databases.closeSilently(resultSet);
      final MethodLogger.Entry logEntry = logExit("query", exception);
      if (LOG.isDebugEnabled()) {
        LOG.debug(Databases.createLogMessage(getUser(), sql, null, exception, logEntry));
      }
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
   * Disables auto-commit on the given connection and sets the internal connection.
   * @param connection the connection
   * @throws DatabaseException in case disabling auto-commit fails
   */
  private void initialize(final Connection connection) throws DatabaseException {
    if (isConnected()) {
      throw new IllegalStateException("Already connected");
    }

    try {
      connection.setAutoCommit(false);
      this.connection = connection;
    }
    catch (final SQLException e) {
      LOG.error("Unable to disable auto commit on connection, assuming invalid state", e);
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
      return new User(connection.getMetaData().getUserName(), null);
    }
    catch (final SQLException e) {
      throw new DatabaseException(e, "Exception while trying to retrieve username from meta data");
    }
  }
}