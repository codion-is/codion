/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.database;

import org.jminor.common.Configuration;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.user.User;
import org.jminor.common.value.PropertyValue;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Defines DBMS specific functionality as well as basic database configuration settings.
 */
public interface Database {

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
  PropertyValue<String> DATABASE_URL = Configuration.stringValue("jminor.db.url", null);

  /**
   * A comma separated list of paths to scripts to run when initializing the database, implementation specific
   */
  PropertyValue<String> DATABASE_INIT_SCRIPT = Configuration.stringValue("jminor.db.initScript", null);

  /**
   * The constant used to denote the username value in the connection properties
   */
  String USER_PROPERTY = "user";

  /**
   * The constant used to denote the password value in the connection properties
   */
  String PASSWORD_PROPERTY = "password";

  /**
   * The default connection login timeout
   * @see java.sql.DriverManager#setLoginTimeout(int)
   */
  int DEFAULT_LOGIN_TIMEOUT = 2;

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
   * Returns the database url for this database.
   * @return the database url for this database
   */
  String getUrl();

  /**
   * In the case of embedded databases the user login info can be appended
   * to the connection url, this method should return that string in case
   * the dbms requires it and {@code connectionProperties} contains
   * the username and password info, otherwise it should be safe to return null.
   * This method is usually used in {@code getURL()} and {@code onDisconnect()}.
   * @param connectionProperties the connection properties
   * @return an authentication string to append to the connection url,
   * for example user=scott;password=tiger, null if none is required
   */
  String getAuthenticationInfo(Properties connectionProperties);

  /**
   * This should shutdown the database in case it is an embedded one
   * and if that is applicable, such as for Derby.
   * @param connectionProperties the connection properties
   */
  void shutdownEmbedded(Properties connectionProperties);

  /**
   * Returns the select for update support of the underlying database.
   * @return the select for update support.
   */
  SelectForUpdateSupport getSelectForUpdateSupport();

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
   * Returns a user friendly error message for the given exception,
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
   * Adds any dbms specific connection properties to the given properties map,
   * called each time a connection is created
   * @param properties the properties map to add to
   * @return the properties map
   */
  Properties addConnectionProperties(Properties properties);

  /**
   * Creates a connection for the given user.
   * @param user the user for which to create a connection
   * @return a Connection
   * @throws DatabaseException in case of a connection error
   * @throws org.jminor.common.db.exception.AuthenticationException in case of an authentication error
   */
  Connection createConnection(User user) throws DatabaseException;

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
