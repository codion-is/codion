/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * User: Björn Darri
 * Date: 18.7.2010
 * Time: 19:49:49
 */
public interface PoolableConnection {

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
  void setPoolRetryCount(final int retryCount);

  /**
   * @return the number of retries required to retrieve this connection from the pool
   */
  int getPoolRetryCount();

  /**
   * @return true if the connection is valid
   */
  boolean isConnectionValid();

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
   * @throws SQLException in case anything goes wrong during the commit action
   * @throws IllegalStateException in case transaction is not open
   */
  void commitTransaction() throws SQLException;

  /**
   * Performs a rollback and ends the current transaction
   * @throws SQLException in case anything goes wrong during the rollback action
   * @throws IllegalStateException in case transaction is not open
   */
  void rollbackTransaction() throws SQLException;

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
}
