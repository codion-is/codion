/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.common.db.connection;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.logging.MethodLogger;
import is.codion.common.user.User;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Manages a {@link Connection} instance, providing basic transaction control.
 * A factory class for DatabaseConnection instances.
 */
public interface DatabaseConnection extends AutoCloseable {

  /**
   * SQLException state indicating that a query did not return a result
   */
  String SQL_STATE_NO_DATA = "02000";

  /**
   * @return true if the connection has been established and is valid
   */
  boolean connected();

  /**
   * Returns the underlying connection object, null in case this connection has been closed.
   * Use {@link #connected()} to verify that the connection is not null and valid.
   * @return the underlying connection object, null in case this connection has been closed
   */
  Connection getConnection();

  /**
   * Sets the internal connection to use, note that no validation or transaction checking is performed
   * on the connection and auto-commit is assumed to be disabled. The connection is simply used 'as is'.
   * Note that setting the connection to null causes all methods requiring it to throw a {@link IllegalStateException}
   * until a non-null connection is set.
   * @param connection the JDBC connection
   */
  void setConnection(Connection connection);

  /**
   * Begins a transaction on this connection, to end the transaction use {@link #commitTransaction()} or {@link #rollbackTransaction()}.
   * @throws IllegalStateException in case a transaction is already open
   */
  void beginTransaction();

  /**
   * @return true if a transaction is open
   */
  boolean transactionOpen();

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