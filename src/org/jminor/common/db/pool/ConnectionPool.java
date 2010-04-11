/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.pool;

import org.jminor.common.db.DbConnection;
import org.jminor.common.model.User;

import java.sql.SQLException;

/**
 * Defines a simple connection pool.
 * User: Bjorn Darri
 * Date: 28.3.2010
 * Time: 13:06:20
 */
public interface ConnectionPool {

  /**
   * Return the given connection to the pool.
   * @param dbConnection the database connection to return to the pool
   */
  void checkInConnection(final DbConnection dbConnection);

  /**
   * Fetches a connection from the pool.
   * @return a database connection retrieved from the pool
   * @throws ClassNotFoundException in case the JDBC driver class is not found
   * @throws SQLException in case of a database exception
   */
  DbConnection checkOutConnection() throws ClassNotFoundException, SQLException;

  /**
   * @return the connection pool setings
   */
  ConnectionPoolSettings getConnectionPoolSettings();

  /**
   * Sets the connection pool settings.
   * @param settings the connection pool settings
   */
  void setConnectionPoolSettings(final ConnectionPoolSettings settings);

  /**
   * @return the user this connection pool is based on.
   */
  User getUser();

  /**
   * Retrives usage statistics for the connection pool since time <code>since</code>.
   * @param since the time from which statistics should be retrieved
   * @return connection pool usage statistics
   */
  ConnectionPoolStatistics getConnectionPoolStatistics(final long since);

  /**
   * Resets the collected usage statistics
   */
  void resetPoolStatistics();

  /**
   * @return true if fine grained pool usage statistics should be collected.
   */
  boolean isCollectFineGrainedStatistics();

  /**
   * Specifies whether or not fine grained usage statistics should be collected.
   * @param value the value
   */
  void setCollectFineGrainedStatistics(final boolean value);
}
