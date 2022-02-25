/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.database;

import is.codion.common.Configuration;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.pool.ConnectionFactory;
import is.codion.common.db.pool.ConnectionPoolFactory;
import is.codion.common.db.pool.ConnectionPoolWrapper;
import is.codion.common.user.User;
import is.codion.common.value.PropertyValue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;

/**
 * Defines DBMS specific functionality as well as basic database configuration settings.
 * @see DatabaseFactory#getDatabase()
 * @see DatabaseFactory#databaseFactory()
 * @see DatabaseFactory#databaseFactory(String)
 * @see DatabaseFactory#createDatabase(String)
 */
public interface Database extends ConnectionFactory {

  /**
   * The possible select for update support values.
   */
  enum SelectForUpdateSupport {
    /**
     * No support for select for update.
     */
    NONE,
    /**
     * Supports basic for update.
     */
    FOR_UPDATE,
    /**
     * Supports for update with the 'nowait' option.
     */
    FOR_UPDATE_NOWAIT
  }

  /**
   * Specifies the jdbc url of the database.
   */
  PropertyValue<String> DATABASE_URL = Configuration.stringValue("codion.db.url", null);

  /**
   * A comma separated list of paths to scripts to run when initializing the database, implementation specific
   */
  PropertyValue<String> DATABASE_INIT_SCRIPTS = Configuration.stringValue("codion.db.initScripts", null);

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
  PropertyValue<Boolean> QUERY_COUNTER_ENABLED = Configuration.booleanValue("codion.db.queryCounterEnabled", true);

  /**
   * Specifies whether 'select for update' should be NOWAIT, if supported by the database.<br>
   * A database implementation may disregard this.<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  PropertyValue<Boolean> SELECT_FOR_UPDATE_NOWAIT = Configuration.booleanValue("codion.db.selectForUpdateNowait", true);

  /**
   * Specifies the default login timeout (in seconds).
   * Value type: Integer<br>
   * Default value: 2
   */
  PropertyValue<Integer> LOGIN_TIMEOUT = Configuration.integerValue("codion.db.loginTimeout", 2);

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
  String getName();

  /**
   * Returns a query string for retrieving the last automatically generated id from the given id source
   * @param idSource the source for the id, for example a sequence name or in the case of Derby, the name of the table auto generating the value
   * @return a query string for retrieving the last auto-increment value from idSource
   * @throws NullPointerException in case {@code idSource} is required and is null
   */
  String getAutoIncrementQuery(String idSource);

  /**
   * Returns a query string for selecting the next value from the given sequence.
   * @param sequenceName the name of the sequence
   * @return a query for selecting the next value from the given sequence
   * @throws UnsupportedOperationException in case the underlying database does not support sequences
   * @throws NullPointerException in case {@code sequenceName} is null
   */
  String getSequenceQuery(String sequenceName);

  /**
   * This should shut down the database in case it is an embedded one
   * and if that is applicable, such as for Derby.
   */
  void shutdownEmbedded();

  /**
   * Returns a select for update clause, an empty string if not supported.
   * @return a select for update clause
   */
  String getSelectForUpdateClause();

  /**
   * Returns true if the dbms supports the Java 6 jdbc call {@link Connection#isValid(int)}.
   * @return true if the dbms supports the Java 6 jdbc call {@link Connection#isValid(int)}
   */
  boolean supportsIsValid();

  /**
   * Returns true if this database requires that subqueries by aliased.
   * @return true if subqueries require an alias
   */
  boolean subqueryRequiresAlias();

  /**
   * Returns a query to use when checking if the connection is valid,
   * this is used in cases where the dbms does not support the isValid() call.
   * Returning null is safe if isValid() is supported.
   * @return a check connection query
   * @see #supportsIsValid()
   */
  String getCheckConnectionQuery();

  /**
   * @return the timeout in seconds to use when checking connection validity
   */
  int getValidityCheckTimeout();

  /**
   * Returns a user-friendly error message for the given exception,
   * otherwise simply return the message from {@code exception}
   * @param exception the underlying SQLException
   * @return the message assigned to the given exception
   */
  String getErrorMessage(SQLException exception);

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
   * Counts this query, based on the first character.
   * @param query the query to count
   * @see #getStatistics()
   */
  void countQuery(String query);

  /**
   * Returns statistics collected via {@link #countQuery(String)}.
   * Note that calling this method resets the counter.
   * @return collected statistics.
   */
  Statistics getStatistics();

  /**
   * Initializes a connection pool for the given user in this database.
   * @param connectionPoolFactory the ConnectionPoolFactory implementation to use
   * @param poolUser the user to initialize connection pool for
   * @throws DatabaseException in case of a database exception
   */
  void initializeConnectionPool(ConnectionPoolFactory connectionPoolFactory, User poolUser) throws DatabaseException;

  /**
   * @param username the username
   * @return the connection pool for the given user, null if none exists
   */
  ConnectionPoolWrapper getConnectionPool(String username);

  /**
   * @return the usernames of all available connection pools
   */
  Collection<String> getConnectionPoolUsernames();

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
   * Closes the given ResultSet instance, suppressing any SQLExceptions that may occur.
   * @param resultSet the result set to close
   */
  static void closeSilently(final ResultSet resultSet) {
    try {
      if (resultSet != null) {
        resultSet.close();
      }
    }
    catch (SQLException ignored) {/*ignored*/}
  }

  /**
   * Closes the given Statement instance, suppressing any SQLExceptions that may occur.
   * @param statement the statement to close
   */
  static void closeSilently(final Statement statement) {
    try {
      if (statement != null) {
        statement.close();
      }
    }
    catch (SQLException ignored) {/*ignored*/}
  }

  /**
   * Closes the given Connection instance, suppressing any SQLExceptions that may occur.
   * @param connection the connection to close
   */
  static void closeSilently(final Connection connection) {
    try {
      if (connection != null) {
        connection.close();
      }
    }
    catch (SQLException ignored) {/*ignored*/}
  }

  /**
   * Encapsulates basic database usage statistics.
   */
  interface Statistics {

    /**
     * @return the number of queries being run per second
     */
    int getQueriesPerSecond();

    /**
     * @return the number of delete queries being run per second
     */
    int getDeletesPerSecond();

    /**
     * @return the number of insert queries being run per second
     */
    int getInsertsPerSecond();

    /**
     * @return the number of select queries being run per second
     */
    int getSelectsPerSecond();

    /**
     * @return the number of update queries being run per second
     */
    int getUpdatesPerSecond();

    /**
     * @return the timestamp of these statistics
     */
    long getTimestamp();
  }
}
