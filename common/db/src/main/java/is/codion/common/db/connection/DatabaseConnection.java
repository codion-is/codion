/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.common.db.connection;

import dev.codion.common.MethodLogger;
import dev.codion.common.db.database.Database;
import dev.codion.common.user.User;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Manages a {@link Connection}, providing basic transaction control.
 */
public interface DatabaseConnection extends AutoCloseable {

  /**
   * @return true if the connection has been established and is valid
   */
  boolean isConnected();

  /**
   * Returns the underlying connection object, use {@link #isConnected()} to verify
   * that the connection is not null and valid.
   * @return the underlying connection object
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
   * @return the first record in the result as a integer
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
   * Performs a rollback and disconnects this connection
   */
  void disconnect();

  /**
   * @return the connection user
   */
  User getUser();

  /**
   * @return the database implementation this connection is based on
   */
  Database getDatabase();

  /**
   * @param methodLogger the MethodLogger to use, null to disable method logging
   */
  void setMethodLogger(MethodLogger methodLogger);

  /**
   * @return the MethodLogger being used, possibly null
   */
  MethodLogger getMethodLogger();
}