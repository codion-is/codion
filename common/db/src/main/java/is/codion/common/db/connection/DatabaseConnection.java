/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.connection;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.logging.MethodLogger;
import is.codion.common.user.User;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Manages a {@link Connection}, providing basic transaction control.
 * A factory class for DatabaseConnection instances.
 */
public interface DatabaseConnection extends AutoCloseable {

  /**
   * @return true if the connection has been established and is valid
   */
  boolean isConnected();

  /**
   * Returns the underlying connection object, null in case this connection has been closed.
   * Use {@link #isConnected()} to verify that the connection is not null and valid.
   * @return the underlying connection object, null in case this connection has been closed
   */
  Connection getConnection();

  /**
   * Sets the internal connection to use, note that no validation or transaction checking is performed
   * on the connection and auto-commit is assumed to be disabled. The connection is simply used 'as is'.
   * @param connection the JDBC connection
   */
  void setConnection(Connection connection);

  /**
   * Selects a single integer value using the given query.
   * @param sql the query must select at least a single number column, any other subsequent columns are disregarded
   * @return the first record in the result as an integer
   * @throws SQLException thrown if anything goes wrong during the execution or if no record is returned
   */
  int selectInteger(String sql) throws SQLException;

  /**
   * Selects a single long value using the given query.
   * @param sql the query must select at least a single number column, any other subsequent columns are disregarded
   * @return the first record in the result as a long
   * @throws SQLException thrown if anything goes wrong during the execution or if no record is returned
   */
  long selectLong(String sql) throws SQLException;

  /**
   * Begins a transaction on this connection, to end the transaction use {@link #commitTransaction()} or {@link #rollbackTransaction()}.
   * @throws IllegalStateException in case a transaction is already open
   */
  void beginTransaction();

  /**
   * @return true if a transaction is open
   */
  boolean isTransactionOpen();

  /**
   * Performs a commit and ends the current transaction
   * @throws IllegalStateException in case transaction is not open
   */
  void commitTransaction();

  /**
   * Performs a rollback and ends the current transaction
   * @throws IllegalStateException in case transaction is not open
   */
  void rollbackTransaction();

  /**
   * Performs a commit
   * @throws java.sql.SQLException thrown if anything goes wrong during the execution
   * @throws IllegalStateException in case a transaction is open
   */
  void commit() throws SQLException;

  /**
   * Performs a rollback
   * @throws SQLException thrown if anything goes wrong during the execution
   * @throws IllegalStateException in case a transaction is open
   */
  void rollback() throws SQLException;

  /**
   * Performs a rollback and disconnects this connection. The only way to resurrect a closed
   * connection is by using {@link #setConnection(Connection)}.
   * Calling this method renders this connection unusable, subsequent calls to methods
   * using the underlying connection result in a {@link IllegalStateException} being thrown,
   * that is, until a new {@link Connection} is set via {@link #setConnection(Connection)}.
   */
  void close();

  /**
   * @return the connection user
   */
  User user();

  /**
   * @return the database implementation this connection is based on
   */
  Database database();

  /**
   * @param methodLogger the MethodLogger to use, null to disable method logging
   */
  void setMethodLogger(MethodLogger methodLogger);

  /**
   * @return the MethodLogger being used, possibly null
   */
  MethodLogger getMethodLogger();

  /**
   * Constructs a new DatabaseConnection instance, based on the given Database and User
   * @param database the database
   * @param user the user for the db-connection
   * @return a new DatabaseConnection instance
   * @throws DatabaseException in case there is a problem connecting to the database
   */
  static DatabaseConnection databaseConnection(Database database, User user) throws DatabaseException {
    return new DefaultDatabaseConnection(database, user);
  }

  /**
   * Constructs a new DatabaseConnection instance, based on the given Connection object.
   * NB. auto commit is disabled on the Connection that is provided.
   * @param database the database
   * @param connection the Connection object to base this DatabaseConnection on
   * @return a new DatabaseConnection instance
   * @throws DatabaseException in case there is a problem with the connection
   */
  static DatabaseConnection databaseConnection(Database database, Connection connection) throws DatabaseException {
    return new DefaultDatabaseConnection(database, connection);
  }
}