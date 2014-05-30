/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.model.tools.MethodLogger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * A default DatabaseConnection implementation, which wraps a standard JDBC Connection object.
 */
public class DefaultDatabaseConnection implements DatabaseConnection {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultDatabaseConnection.class);

  private final User user;
  private final Database database;
  private final int validityCheckTimeout;

  private Connection connection;
  private boolean transactionOpen = false;

  private long poolTime = -1;
  private int poolRetryCount = 0;

  private MethodLogger methodLogger;

  /**
   * Constructs a new DefaultDatabaseConnection instance, initialized and ready for usage
   * @param database the database
   * @param user the user to base this database connection on
   * @throws DatabaseException in case there is a problem connecting to the database
   */
  public DefaultDatabaseConnection(final Database database, final User user) throws DatabaseException {
    this(database, user, 0);
  }

  /**
   * Constructs a new DefaultDatabaseConnection instance, initialized and ready for usage
   * @param database the database
   * @param user the user to base this database connection on
   * @param validityCheckTimeout the number of seconds specified when checking if this connection is valid
   * @throws DatabaseException in case there is a problem connecting to the database
   */
  public DefaultDatabaseConnection(final Database database, final User user,
                                   final int validityCheckTimeout) throws DatabaseException {
    Util.rejectNullValue(database, "database");
    Util.rejectNullValue(user, "user");
    this.database = database;
    this.user = user;
    this.validityCheckTimeout = validityCheckTimeout;
    initializeAndValidate(database.createConnection(user));
  }

  /**
   * Constructs a new DefaultDatabaseConnection instance, based on the given Connection object.
   * NB. auto commit is disabled on the Connection that is provided.
   * @param database the database
   * @param connection the Connection object to base this DefaultDatabaseConnection on
   * @throws IllegalArgumentException in case the given connection is invalid or disconnected
   * @throws DatabaseException in case of an exception while retrieving the username from the connection
   * meta data or if a validation statement is required and creating it fails
   */
  public DefaultDatabaseConnection(final Database database, final Connection connection) throws DatabaseException {
    this(database, connection, 0);
  }

  /**
   * Constructs a new DefaultDatabaseConnection instance, based on the given Connection object.
   * NB. auto commit is disabled on the Connection that is provided.
   * @param database the database
   * @param connection the Connection object to base this DefaultDatabaseConnection on
   * @param validityCheckTimeout the number of seconds specified when checking if this connection is valid
   * @throws IllegalArgumentException in case the given connection is invalid or disconnected
   * @throws DatabaseException in case of an exception while retrieving the username from the connection
   * meta data or if a validation statement is required and creating it fails
   */
  public DefaultDatabaseConnection(final Database database, final Connection connection,
                                   final int validityCheckTimeout) throws DatabaseException {
    Util.rejectNullValue(database, "database");
    Util.rejectNullValue(connection, "connection");
    this.database = database;
    this.validityCheckTimeout = validityCheckTimeout;
    initializeAndValidate(connection);
    this.user = getUser(connection);
  }

  /** {@inheritDoc} */
  @Override
  public final void setPoolTime(final long time) {
    this.poolTime = time;
  }

  /** {@inheritDoc} */
  @Override
  public final long getPoolTime() {
    return poolTime;
  }

  /** {@inheritDoc} */
  @Override
  public final void setRetryCount(final int retryCount) {
    this.poolRetryCount = retryCount;
  }

  /** {@inheritDoc} */
  @Override
  public final int getRetryCount() {
    return poolRetryCount;
  }

  /** {@inheritDoc} */
  @Override
  public final String toString() {
    return getClass().getSimpleName() + ": " + user.getUsername();
  }

  /** {@inheritDoc} */
  @Override
  public final User getUser() {
    return user;
  }

  /** {@inheritDoc} */
  @Override
  public final void setMethodLogger(final MethodLogger methodLogger) {
    this.methodLogger = methodLogger;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isValid() {
    return connection != null && DatabaseUtil.isValid(connection, database, validityCheckTimeout);
  }

  /** {@inheritDoc} */
  @Override
  public final void disconnect() {
    try {
      if (connection != null && !connection.isClosed()) {
        connection.rollback();
      }
    }
    catch (SQLException ex) {
      LOG.warn("DefaultDatabaseConnection.disconnect(), connection invalid", ex);
    }
    DatabaseUtil.closeSilently(connection);
    connection = null;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isConnected() {
    return connection != null;
  }

  /**
   * @return the underlying Connection object
   */
  @Override
  public final Connection getConnection() {
    if (!isConnected()) {
      throw new IllegalStateException("Not connected");
    }

    return connection;
  }

  /** {@inheritDoc} */
  @Override
  public final Database getDatabase() {
    return database;
  }

  /** {@inheritDoc} */
  @Override
  public final List query(final String sql, final ResultPacker resultPacker, final int fetchCount) throws SQLException {
    DatabaseUtil.QUERY_COUNTER.count(sql);
    Statement statement = null;
    SQLException exception = null;
    ResultSet resultSet = null;
    try {
      logAccess("query", new Object[] {sql});
      statement = getConnection().createStatement();
      resultSet = statement.executeQuery(sql);

      return resultPacker.pack(resultSet, fetchCount);
    }
    catch (SQLException e) {
      exception = e;
      throw e;
    }
    finally {
      DatabaseUtil.closeSilently(statement);
      DatabaseUtil.closeSilently(resultSet);
      final MethodLogger.Entry logEntry = logExit("query", exception, null);
      if (LOG != null && LOG.isDebugEnabled()) {
        LOG.debug(DatabaseUtil.createLogMessage(getUser(), sql, null, exception, logEntry));
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void beginTransaction() {
    if (transactionOpen) {
      throw new IllegalStateException("Transaction already open");
    }

    LOG.debug("{}: begin transaction;", user.getUsername());
    logAccess("beginTransaction", new Object[0]);
    transactionOpen = true;
    logExit("beginTransaction", null, null);
  }

  /** {@inheritDoc} */
  @Override
  public final void rollbackTransaction() {
    SQLException exception = null;
    try {
      if (!transactionOpen) {
        throw new IllegalStateException("Transaction is not open");
      }

      LOG.debug("{}: rollback transaction;", user.getUsername());
      logAccess("rollbackTransaction", new Object[0]);
      connection.rollback();
    }
    catch (SQLException e) {
      exception = e;
    }
    finally {
      transactionOpen = false;
      logExit("rollbackTransaction", exception, null);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void commitTransaction() {
    SQLException exception = null;
    try {
      if (!transactionOpen) {
        throw new IllegalStateException("Transaction is not open");
      }

      LOG.debug("{}: commit transaction;", user.getUsername());
      logAccess("commitTransaction", new Object[0]);
      connection.commit();
    }
    catch (SQLException e) {
      exception = e;
    }
    finally {
      transactionOpen = false;
      logExit("commitTransaction", exception, null);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isTransactionOpen() {
    return transactionOpen;
  }

  /** {@inheritDoc} */
  @Override
  public final void commit() throws SQLException {
    if (transactionOpen) {
      throw new IllegalStateException("Can not perform a commit during an open transaction");
    }

    LOG.debug("{}: commit;", user.getUsername());
    logAccess("commit", new Object[0]);
    SQLException exception = null;
    try {
      connection.commit();
    }
    catch (SQLException e) {
      LOG.error("Exception during commit: " + user.getUsername(), e);
      exception = e;
      throw e;
    }
    finally {
      logExit("commit", exception, null);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void rollback() throws SQLException {
    if (transactionOpen) {
      throw new IllegalStateException("Can not perform a rollback during an open transaction");
    }

    LOG.debug("{}: rollback;", user.getUsername());
    logAccess("rollback", new Object[0]);
    SQLException exception = null;
    try {
      connection.rollback();
    }
    catch (SQLException e) {
      LOG.error("Exception during rollback: " + user.getUsername(), e);
      exception = e;
      throw e;
    }
    finally {
      logExit("rollback", exception, null);
    }
  }

  /**
   * Sets the internal connection to use, note that no validation or
   * transaction checking is performed, it is simply used 'as is'
   * @param connection the connection
   */
  public final void setConnection(final Connection connection) {
    this.connection = connection;
  }

  protected final MethodLogger.Entry logExit(final String method, final Throwable exception, final String exitMessage) {
    if (methodLogger != null) {
      return methodLogger.logExit(method, exception, exitMessage);
    }

    return null;
  }

  protected final void logAccess(final String method, final Object[] arguments) {
    if (methodLogger != null) {
      methodLogger.logAccess(method, arguments);
    }
  }

  private void initializeAndValidate(final Connection connection) throws DatabaseException {
    if (isConnected()) {
      throw new IllegalStateException("Already connected");
    }

    try {
      if (connection.isClosed()) {
        throw new IllegalArgumentException("Connection closed");
      }
      this.connection = connection;
      if (!isValid()) {
        throw new IllegalArgumentException("Connection invalid during instantiation");
      }
      connection.setAutoCommit(false);
    }
    catch (SQLException e) {
      throw new DatabaseException(e, "Unable to disable auto commit on the given connection");
    }
  }

  /**
   * Returns a User with the username from the meta data retrieved from the given connection
   * @param connection the connection
   * @return a user based on the information gleamed from the given connection
   * @throws DatabaseException in case of an exception while retrieving
   * the username from the connection meta data
   * @see java.sql.DatabaseMetaData#getUserName()
   */
  private static User getUser(final Connection connection) throws DatabaseException {
    try {
      return new User(connection.getMetaData().getUserName(), null);
    }
    catch (SQLException e) {
      throw new DatabaseException(e, "Exception while trying to retrieve username from meta data");
    }
  }

  /**
   * A base Operation implementation
   */
  public static class DefaultOperation implements DatabaseConnection.Operation {

    private final String id;
    private final String name;

    /**
     * Instantiates a new DefaultOperation
     * @param id a unique operation ID
     * @param name the operation name
     */
    public DefaultOperation(final String id, final String name) {
      this.id = id;
      this.name = name;
    }

    /** {@inheritDoc} */
    @Override
    public final String getID() {
      return id;
    }

    /** {@inheritDoc} */
    @Override
    public final String getName() {
      return this.name;
    }
  }
}