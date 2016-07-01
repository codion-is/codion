/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.User;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.tools.MethodLogger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/**
 * A default DatabaseConnection implementation, which wraps a standard JDBC Connection object.
 */
final class DefaultDatabaseConnection implements DatabaseConnection {

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
    this.database = Objects.requireNonNull(database, "database");
    this.user = Objects.requireNonNull(user, "user");
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
  public DefaultDatabaseConnection(final Database database, final Connection connection) throws DatabaseException {
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
  public DefaultDatabaseConnection(final Database database, final Connection connection,
                                   final int validityCheckTimeout) throws DatabaseException {
    this.database = Objects.requireNonNull(database, "database");
    this.validityCheckTimeout = validityCheckTimeout;
    initialize(Objects.requireNonNull(connection, "connection"));
    this.user = getUser(connection);
  }

  /** {@inheritDoc} */
  @Override
  public void setPoolTime(final long time) {
    this.poolTime = time;
  }

  /** {@inheritDoc} */
  @Override
  public long getPoolTime() {
    return poolTime;
  }

  /** {@inheritDoc} */
  @Override
  public void setRetryCount(final int retryCount) {
    this.poolRetryCount = retryCount;
  }

  /** {@inheritDoc} */
  @Override
  public int getRetryCount() {
    return poolRetryCount;
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
    DatabaseUtil.closeSilently(connection);
    connection = null;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isConnected() {
    return connection != null && DatabaseUtil.isValid(connection, database, validityCheckTimeout);
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
  public void beginTransaction() {
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
  public void rollbackTransaction() {
    SQLException exception = null;
    try {
      if (!transactionOpen) {
        throw new IllegalStateException("Transaction is not open");
      }

      LOG.debug("{}: rollback transaction;", user.getUsername());
      logAccess("rollbackTransaction", new Object[0]);
      connection.rollback();
    }
    catch (final SQLException e) {
      exception = e;
    }
    finally {
      transactionOpen = false;
      logExit("rollbackTransaction", exception, null);
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
      logAccess("commitTransaction", new Object[0]);
      connection.commit();
    }
    catch (final SQLException e) {
      exception = e;
    }
    finally {
      transactionOpen = false;
      logExit("commitTransaction", exception, null);
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
    logAccess("commit", new Object[0]);
    SQLException exception = null;
    try {
      connection.commit();
    }
    catch (final SQLException e) {
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
  public void rollback() throws SQLException {
    if (transactionOpen) {
      throw new IllegalStateException("Can not perform a rollback during an open transaction, use 'rollbackTransaction()'");
    }

    LOG.debug("{}: rollback;", user.getUsername());
    logAccess("rollback", new Object[0]);
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
      logExit("rollback", exception, null);
    }
  }

  private void logExit(final String method, final Throwable exception, final String exitMessage) {
    if (methodLogger != null && methodLogger.isEnabled()) {
      methodLogger.logExit(method, exception, exitMessage);
    }
  }

  private void logAccess(final String method, final Object[] arguments) {
    if (methodLogger != null && methodLogger.isEnabled()) {
      methodLogger.logAccess(method, arguments);
    }
  }

  private void initialize(final Connection connection) {
    if (isConnected()) {
      throw new IllegalStateException("Already connected");
    }

    try {
      connection.setAutoCommit(false);
      this.connection = connection;
    }
    catch (final SQLException e) {
      LOG.error("Unable to disable auto commit on connection, assuming invalid state", e);
      throw new IllegalArgumentException("Connection invalid during instantiation", e);
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
    catch (final SQLException e) {
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