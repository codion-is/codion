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
 * Copyright (c) 2009 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.db.database;

import is.codion.common.Configuration;
import is.codion.common.db.connection.ConnectionFactory;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.pool.ConnectionPoolFactory;
import is.codion.common.db.pool.ConnectionPoolWrapper;
import is.codion.common.property.PropertyValue;
import is.codion.common.user.User;

import java.sql.SQLException;
import java.util.Collection;

/**
 * Defines DBMS specific functionality as well as basic database configuration settings.
 * @see Database#instance()
 * @see DatabaseFactory#instance()
 * @see DatabaseFactory#instance(String)
 * @see DatabaseFactory#createDatabase(String)
 */
public interface Database extends ConnectionFactory {

  /**
   * Specifies common database operations.
   */
  enum Operation {
    /**
     * Select
     */
    SELECT,
    /**
     * Update
     */
    UPDATE,
    /**
     * Insert
     */
    INSERT,
    /**
     * Delete
     */
    DELETE,
    /**
     * Other
     */
    OTHER
  }

  /**
   * Specifies the jdbc url of the database.
   */
  PropertyValue<String> DATABASE_URL = Configuration.stringValue("codion.db.url");

  /**
   * A comma separated list of paths to scripts to run when initializing the database, implementation specific
   */
  PropertyValue<String> DATABASE_INIT_SCRIPTS = Configuration.stringValue("codion.db.initScripts");

  /**
   * Specifies the timeout (in seconds) to use when checking if database connections are valid.
   * Value type: Integer<br>
   * Default value: 2
   */
  PropertyValue<Integer> CONNECTION_VALIDITY_CHECK_TIMEOUT = Configuration.integerValue("codion.db.validityCheckTimeout", 2);

  /**
   * Specifies whether database queries should be counted.<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  PropertyValue<Boolean> COUNT_QUERIES = Configuration.booleanValue("codion.db.countQueries", true);

  /**
   * Specifies whether 'select for update' should be NOWAIT, if supported by the database.<br>
   * A database implementation may disregard this.<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  PropertyValue<Boolean> SELECT_FOR_UPDATE_NOWAIT = Configuration.booleanValue("codion.db.selectForUpdateNowait", true);

  /**
   * Specifies the default login timeout (in seconds).<br>
   * Value type: Integer<br>
   * Default value: 2
   */
  PropertyValue<Integer> LOGIN_TIMEOUT = Configuration.integerValue("codion.db.loginTimeout", 2);

  /**
   * Specifies the transaction isolation to set for created connections.<br>
   * Value type: Integer<br>
   * Default value: null
   * @see java.sql.Connection#setTransactionIsolation(int)
   */
  PropertyValue<Integer> TRANSACTION_ISOLATION = Configuration.integerValue("codion.db.transactionIsolation");

  /**
   * The constant used to denote the username value in the connection properties
   */
  String USER_PROPERTY = "user";

  /**
   * The constant used to denote the password value in the connection properties
   */
  String PASSWORD_PROPERTY = "password";

  /**
   * @return a name identifying this database
   */
  String name();

  /**
   * Returns a query string for retrieving the last automatically generated id from the given id source
   * @param idSource the source for the id, for example a sequence name or in the case of Derby, the name of the table auto generating the value
   * @return a query string for retrieving the last auto-increment value from idSource
   * @throws NullPointerException in case {@code idSource} is required and is null
   */
  String autoIncrementQuery(String idSource);

  /**
   * Returns a query string for selecting the next value from the given sequence.
   * @param sequenceName the name of the sequence
   * @return a query for selecting the next value from the given sequence
   * @throws UnsupportedOperationException in case the underlying database does not support sequences
   * @throws NullPointerException in case {@code sequenceName} is null
   */
  String sequenceQuery(String sequenceName);

  /**
   * Returns a select for update clause, an empty string if not supported.
   * @return a select for update clause
   */
  String selectForUpdateClause();

  /**
   * Returns a limit/offset clause variation for this database, based on the given limit and offset values.
   * If both are null an empty string should be returned.
   * @param limit the limit
   * @param offset the offset
   * @return a limit/offset clause
   */
  String limitOffsetClause(Integer limit, Integer offset);

  /**
   * Returns true if this database requires that subqueries by aliased.
   * @return true if subqueries require an alias
   */
  boolean subqueryRequiresAlias();

  /**
   * Returns the maximum number of prepared statement parameters, supported by this database.
   * The default implementation simply returns {@link Integer#MAX_VALUE}, as in, no limit.
   * @return the maximum number of prepared statement parameters, supported by this database.
   */
  int maximumNumberOfParameters();

  /**
   * Returns a user-friendly error message for the given exception,
   * otherwise simply return the message from {@code exception}
   * @param exception the underlying SQLException
   * @param operation the operation resulting in the exception
   * @return the message assigned to the given exception
   */
  String errorMessage(SQLException exception, Operation operation);

  /**
   * Maps the given SQLException to a DatabaseException.
   * @param exception the SQL exception
   * @param operation the current operation
   * @return a DatabaseException
   * @see #errorMessage(SQLException, Operation)
   */
  DatabaseException databaseException(SQLException exception, Operation operation);

  /**
   * Returns true if this exception represents a login credentials failure
   * @param exception the exception
   * @return true if this exception represents a login credentials failure
   */
  boolean isAuthenticationException(SQLException exception);

  /**
   * Returns true if this exception is a referential integrity exception
   * @param exception the exception
   * @return true if this exception is a referential integrity exception
   */
  boolean isReferentialIntegrityException(SQLException exception);

  /**
   * Returns true if this exception is a unique key exception
   * @param exception the exception
   * @return true if this exception is a unique key exception
   */
  boolean isUniqueConstraintException(SQLException exception);

  /**
   * Returns true if this exception is a timeout exception
   * @param exception the exception
   * @return true if this exception is a timeout exception
   */
  boolean isTimeoutException(SQLException exception);

  /**
   * @return the {@link QueryCounter} for collecting query statistics
   */
  QueryCounter queryCounter();

  /**
   * Returns statistics collected via {@link #queryCounter()}.
   * Note that calling this method resets the counter.
   * @return collected statistics.
   */
  Statistics statistics();

  /**
   * Creates a connection pool for the given user in this database.
   * @param connectionPoolFactory the ConnectionPoolFactory implementation to use
   * @param poolUser the user for which to create a connection pool
   * @throws DatabaseException in case of a database exception
   */
  void createConnectionPool(ConnectionPoolFactory connectionPoolFactory, User poolUser) throws DatabaseException;

  /**
   * @param username the username
   * @return true if a connection pool exists for the given username
   */
  boolean containsConnectionPool(String username);

  /**
   * @param username the username
   * @return the connection pool for the given user, null if none exists
   * @throws IllegalArgumentException in case no connection pool exists for the given user
   */
  ConnectionPoolWrapper connectionPool(String username);

  /**
   * @return the usernames of all available connection pools
   */
  Collection<String> connectionPoolUsernames();

  /**
   * Closes and removes the pool associated with the given user
   * @param username the username of the pool that should be removed
   */
  void closeConnectionPool(String username);

  /**
   * Closes and removes all available connection pools
   */
  void closeConnectionPools();

  /**
   * Sets the {@link ConnectionProvider} instance used when creating connections.
   * @param connectionProvider the connection provider
   */
  void setConnectionProvider(ConnectionProvider connectionProvider);

  /**
   * Returns a {@link Database} instance based on the currently configured JDBC URL ({@link Database#DATABASE_URL}).
   * Subsequent calls to this method return the same instance, until the JDBC URL changes, then a new instance is created.
   * @return a Database instance based on the current jdbc url
   * @throws IllegalArgumentException in case an unsupported database type is specified
   * @throws RuntimeException in case of an exception occurring while instantiating the database implementation
   * @see Database#DATABASE_URL
   */
  static Database instance() {
    return AbstractDatabase.instance();
  }

  /**
   * Counts queries for statistics.
   */
  interface QueryCounter {

    /**
     * Count one select query
     */
    void select();

    /**
     * Count one insert query
     */
    void insert();

    /**
     * Count one update query
     */
    void update();

    /**
     * Count one delete query
     */
    void delete();

    /**
     * Count one query which does not fall under select, insert, delete or update
     */
    void other();
  }

  /**
   * Encapsulates basic database usage statistics.
   */
  interface Statistics {

    /**
     * @return the number of queries being run per second
     */
    int queriesPerSecond();

    /**
     * @return the number of delete queries being run per second
     */
    int deletesPerSecond();

    /**
     * @return the number of insert queries being run per second
     */
    int insertsPerSecond();

    /**
     * @return the number of select queries being run per second
     */
    int selectsPerSecond();

    /**
     * @return the number of update queries being run per second
     */
    int updatesPerSecond();

    /**
     * @return the number of queries, not falling under select, insert, update or delete, being run per second
     */
    int otherPerSecond();

    /**
     * @return the timestamp of these statistics
     */
    long timestamp();
  }
}
