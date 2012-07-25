/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.LogEntry;
import org.jminor.common.model.MethodLogger;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;

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
public class DatabaseConnectionImpl implements DatabaseConnection {

  private static final Logger LOG = LoggerFactory.getLogger(DatabaseConnection.class);

  private final User user;
  private final Database database;

  private Connection connection;
  private Statement checkConnectionStatement;
  private boolean transactionOpen = false;

  private long poolTime = -1;
  private int poolRetryCount = 0;

  /**
   * The object containing the method call log
   */
  private final MethodLogger methodLogger = new MethodLogger(100, true);

  /**
   * Constructs a new DatabaseConnectionImpl instance, initialized and ready for usage
   * @param database the database
   * @param user the user to base this database connection on
   * @throws DatabaseException in case there is a problem connecting to the database
   * @throws ClassNotFoundException in case the database driver was not found
   */
  public DatabaseConnectionImpl(final Database database, final User user) throws ClassNotFoundException, DatabaseException {
    Util.rejectNullValue(database, "database");
    Util.rejectNullValue(user, "user");
    this.database = database;
    this.user = user;
    setConnection(database.createConnection(user));
  }

  /**
   * Constructs a new DatabaseConnectionImpl instance, based on the given Connection object.
   * NB. auto commit is disabled on the Connection that is provided.
   * @param database the database
   * @param connection the Connection object to base this DatabaseConnectionImpl on
   * @throws IllegalArgumentException in case the given connection is invalid or disconnected
   * @throws DatabaseException in case of an exception while retrieving the username from the connection
   * meta data or if a validation statement is required and creating it fails
   */
  public DatabaseConnectionImpl(final Database database, final Connection connection) throws DatabaseException {
    Util.rejectNullValue(database, "database");
    this.database = database;
    if (!isValid(database, connection, null)) {
      throw new IllegalArgumentException("Connection invalid during instantiation");
    }
    this.user = getUser(connection);
    setConnection(connection);
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
  public final void setLoggingEnabled(final boolean enabled) {
    methodLogger.setEnabled(enabled);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isLoggingEnabled() {
    return methodLogger.isEnabled();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isValid() {
    return isValid(database, connection, checkConnectionStatement);
  }

  /** {@inheritDoc} */
  @Override
  public final void disconnect() {
    if (connection == null) {
      return;
    }

    DbUtil.closeSilently(checkConnectionStatement);
    try {
      if (!connection.isClosed()) {
        connection.rollback();
        connection.close();
      }
    }
    catch (SQLException ex) {
      LOG.error(ex.getMessage(), ex);
    }
    connection = null;
    checkConnectionStatement = null;
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
  public final void beginTransaction() {
    if (transactionOpen) {
      throw new IllegalStateException("Transaction already open");
    }

    methodLogger.logAccess("beginTransaction", new Object[0]);
    transactionOpen = true;
    methodLogger.logExit("beginTransaction", null, null);
  }

  /** {@inheritDoc} */
  @Override
  public final void rollbackTransaction(){
    SQLException exception = null;
    try {
      if (!transactionOpen) {
        throw new IllegalStateException("Transaction is not open");
      }

      LOG.debug("{}: rollback transaction;", user.getUsername());
      methodLogger.logAccess("rollbackTransaction", new Object[0]);
      connection.rollback();
    }
    catch (SQLException e) {
      exception = e;
    }
    finally {
      transactionOpen = false;
      methodLogger.logExit("rollbackTransaction", exception, null);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void commitTransaction(){
    SQLException exception = null;
    try {
      if (!transactionOpen) {
        throw new IllegalStateException("Transaction is not open");
      }

      LOG.debug("{}: commit transaction;", user.getUsername());
      methodLogger.logAccess("commitTransaction", new Object[0]);
      connection.commit();
    }
    catch (SQLException e) {
      exception = e;
    }
    finally {
      transactionOpen = false;
      methodLogger.logExit("commitTransaction", exception, null);
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
    methodLogger.logAccess("commit", new Object[0]);
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
      methodLogger.logExit("commit", exception, null);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void rollback() throws SQLException {
    if (transactionOpen) {
      throw new IllegalStateException("Can not perform a rollback during an open transaction");
    }

    LOG.debug("{}: rollback;", user.getUsername());
    methodLogger.logAccess("rollback", new Object[0]);
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
      methodLogger.logExit("rollback", exception, null);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final List<LogEntry> getLogEntries() {
    return methodLogger.getLogEntries();
  }

  /** {@inheritDoc} */
  @Override
  public final MethodLogger getMethodLogger() {
    return methodLogger;
  }

  private void setConnection(final Connection connection) throws DatabaseException {
    if (isConnected()) {
      throw new IllegalStateException("Already connected");
    }

    this.connection = connection;
    try {
      connection.setAutoCommit(false);
    }
    catch (SQLException e) {
      throw new DatabaseException(e, "Unable to disable auto commit on the given connection");
    }
    if (!database.supportsIsValid()) {
      try {
        this.checkConnectionStatement = connection.createStatement();
      }
      catch (SQLException e) {
        throw new DatabaseException(e, "Unable to create a statement for validity checking");
      }
    }
  }

  private static boolean isValid(final Database database, final Connection connection, final Statement checkConnectionStatement) {
    if (connection == null) {
      return false;
    }
    Statement temporaryStatement = null;
    try {
      if (database.supportsIsValid()) {
        return connection.isValid(0);
      }

      if (checkConnectionStatement == null) {
        temporaryStatement = connection.createStatement();
      }

      return checkConnection(database, checkConnectionStatement == null ? temporaryStatement : checkConnectionStatement);
    }
    catch (SQLException e) {
      LOG.error(e.getMessage(), e);
      return false;
    }
    finally {
      DbUtil.closeSilently(temporaryStatement);
    }
  }

  private static boolean checkConnection(final Database database, final Statement checkConnectionStatement) {
    ResultSet rs = null;
    try {
      rs = checkConnectionStatement.executeQuery(database.getCheckConnectionQuery());
      return true;
    }
    catch (SQLException e) {
      return false;
    }
    finally {
      DbUtil.closeSilently(rs);
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
   * A base OperationImpl implementation
   */
  public static class OperationImpl implements DatabaseConnection.Operation {

    private final String id;
    private final String name;

    /**
     * Instantiates a new OperationImpl
     * @param id a unique operation ID
     * @param name the operation name
     */
    public OperationImpl(final String id, final String name) {
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