/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.Configuration;
import org.jminor.common.MethodLogger;
import org.jminor.common.user.User;
import org.jminor.common.value.PropertyValue;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Manages a {@link Connection}, providing basic transaction control.
 */
public interface DatabaseConnection extends AutoCloseable {

  /**
   * Specifies the timeout (in seconds) to use when checking if database connections are valid.
   * Value type: Integer<br>
   * Default value: 0
   */
  PropertyValue<Integer> CONNECTION_VALIDITY_CHECK_TIMEOUT = Configuration.integerValue("jminor.db.validityCheckTimeout", 0);

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
   * Performs the given query and returns the result as an integer
   * @param sql the query must select at least a single number column, any other subsequent columns are disregarded
   * @return the first record in the result as a integer
   * @throws SQLException thrown if anything goes wrong during the execution or if no record is returned
   */
  int queryInteger(String sql) throws SQLException;

  /**
   * Performs the given query and returns the result as a long
   * @param sql the query must select at least a single number column, any other subsequent columns are disregarded
   * @return the first record in the result as a long
   * @throws SQLException thrown if anything goes wrong during the execution or if no record is returned
   */
  long queryLong(String sql) throws SQLException;

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
   * @param methodLogger the MethodLogger to use
   */
  void setMethodLogger(MethodLogger methodLogger);

  /**
   * @return the MethodLogger being used
   */
  MethodLogger getMethodLogger();
}