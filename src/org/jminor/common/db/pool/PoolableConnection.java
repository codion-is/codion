/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import org.jminor.common.db.Database;
import org.jminor.common.model.LogEntry;
import org.jminor.common.model.MethodLogger;
import org.jminor.common.model.User;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Defines a wrapper connection which can be pooled.
 */
public interface PoolableConnection {

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
   * @throws SQLException thrown if anything goes wrong during the execution
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
   * Disconnects this DbConnection
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
}
