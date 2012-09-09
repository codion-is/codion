/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.User;
import org.jminor.common.model.tools.LogEntry;
import org.jminor.common.model.tools.MethodLogger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Specifies a database connection, providing basic transaction control and pooling info
 */
public interface DatabaseConnection {

  /**
   * @return true if the connection is connected
   */
  boolean isConnected();

  /**
   * @return the underlying connection object
   */
  Connection getConnection();

  /**
   * @return the time at which this connection was pooled
   */
  long getPoolTime();

  /**
   * Sets the time this connection was checked into a connection pool
   * @param time the time this connection was pooled
   */
  void setPoolTime(final long time);

  /**
   * @param retryCount the number of retries used to retrieve this connection from the pool
   */
  void setRetryCount(final int retryCount);

  /**
   * @return the number of retries required to retrieve this connection from the pool
   */
  int getRetryCount();

  /**
   * @return true if the connection is valid
   */
  boolean isValid();

  /**
   * Begins a transaction on this connection
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
   * @return the log entries
   */
  List<LogEntry> getLogEntries();

  /**
   * @return the MethodLogger being used by this db connection
   */
  MethodLogger getMethodLogger();

  /**
   * @param enabled true to enable logging on this connection, false to disable
   */
  void setLoggingEnabled(final boolean enabled);

  /**
   * @return true if logging is enabled, false otherwise
   */
  boolean isLoggingEnabled();

  /**
   * @return the connection user
   */
  User getUser();

  /**
   * @return the database implementation this connection is based on
   */
  Database getDatabase();

  /**
   * A database operation
   */
  interface Operation {

    /**
     * @return this operation's ID, unique for the Database instance being used
     */
    String getID();

    /**
     * @return the name of this operation
     */
    String getName();
  }

  /**
   * A database procedure
   */
  interface Procedure extends Operation {

    /**
     * Executes this procedure with the given connection
     * @param connection the db connection to use when executing
     * @param arguments the procedure arguments, if any
     * @throws DatabaseException in case of an exception during the execution
     */
    void execute(final DatabaseConnection connection, final Object... arguments) throws DatabaseException;
  }

  /**
   * A database function
   */
  interface Function extends Operation {

    /**
     * Executes this function with the given connection
     * @param connection the db connection to use when executing
     * @param arguments the function arguments, if any
     * @return the function return arguments
     * @throws DatabaseException in case of an exception during the execution
     */
    List<Object> execute(final DatabaseConnection connection, final Object... arguments) throws DatabaseException;
  }
}