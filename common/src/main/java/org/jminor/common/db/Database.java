/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.User;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Defines DBMS specific functionality as well as basic database configuration settings.
 */
public interface Database {

  /**
   * The available database types
   */
  enum Type {
    DERBY, H2, HSQL, MYSQL, ORACLE, POSTGRESQL, SQLSERVER, OTHER
  }

  /**
   * Specifies the database type by name, this property is case insensitive
   * @see Database.Type#DERBY
   * @see Database.Type#HSQL
   * @see Database.Type#H2
   * @see Database.Type#MYSQL
   * @see Database.Type#ORACLE
   * @see Database.Type#POSTGRESQL
   * @see Database.Type#SQLSERVER
   */
  String DATABASE_TYPE = "jminor.db.type";

  /**
   * Specifies the machine hosting the database, in the case of embedded databases
   * this specifies the name of the database
   */
  String DATABASE_HOST = "jminor.db.host";

  /**
   * Specifies the database sid (used for dbname for MySQL, SQLServer and Derby server connections)
   */
  String DATABASE_SID = "jminor.db.sid";

  /**
   * Specifies the database port
   */
  String DATABASE_PORT = "jminor.db.port";

  /**
   * Specifies whether or not the database should be run in embedded mode, if applicable<br>
   * Values: "true"/"false"<br>
   * Default: "false"<br>
   */
  String DATABASE_EMBEDDED = "jminor.db.embedded";

  /**
   * Specifies whether or not the database should be run in in-memory mode<br>
   * Values: "true"/"false"<br>
   * Default: "false"<br>
   */
  String DATABASE_EMBEDDED_IN_MEMORY = "jminor.db.embeddedInMemory";

  /**
   * A script to run when initializing the database, implementation specific
   */
  String DATABASE_INIT_SCRIPT = "jminor.db.initScript";

  /**
   * Specifies the Database implementation class to use in case of a dbms that is not directly supported
   * @see Database
   */
  String DATABASE_IMPLEMENTATION_CLASS = "jminor.db.implementation";

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
   * @return the name of the dbms in use
   */
  Type getDatabaseType();

  /**
   * @return the database host name
   */
  String getHost();

  /**
   * @return the database port
   */
  String getPort();

  /**
   * @return the database service id
   */
  String getSid();

  /**
   * @return true if this database is an embedded one
   */
  boolean isEmbedded();

  /**
   * Returns a query string for retrieving the last automatically generated id from the given id source
   * @param idSource the source for the id, for example a sequence name or in the case of Derby, the name of the table auto generating the value
   * @return a query string for retrieving the last auto-increment value from idSource
   * @throws IllegalArgumentException in case <code>idSource</code> is required and is null
   */
  String getAutoIncrementValueSQL(final String idSource);

  /**
   * @param sequenceName the name of the sequence
   * @return a query for selecting the next value from the given sequence
   * @throws UnsupportedOperationException in case the underlying database does not support sequences
   * @throws IllegalArgumentException in case <code>sequenceName</code> is null
   */
  String getSequenceSQL(final String sequenceName);

  /**
   * @param connectionProperties the connection properties, used primarily to provide
   * embedded databases with user info for authentication purposes
   * @return the database url of the active database, based on system properties
   */
  String getURL(final Properties connectionProperties);

  /**
   * In the case of embedded databases the user login info can be appended
   * to the connection url, this method should return that string in case
   * the dbms requires it and <code>connectionProperties</code> contains
   * the username and password info, otherwise it should be safe to return null.
   * This method is usually used in <code>getURL()</code> and <code>onDisconnect()</code>.
   * @param connectionProperties the connection properties
   * @return an authentication string to append to the connection url,
   * f.ex. user=scott;password=tiger, null if none is required
   */
  String getAuthenticationInfo(final Properties connectionProperties);

  /**
   * This should shutdown the database in case it is an embedded one
   * and if that is applicable, such as for Derby.
   * @param connectionProperties the connection properties
   */
  void shutdownEmbedded(final Properties connectionProperties);

  /**
   * @return true if the dbms supports the Java 6 jdbc call Connection.isValid()
   */
  boolean supportsIsValid();

  /**
   * Returns true if the dbms supports the select for update NOWAIT option
   * @return true if NOWAIT is supported for select for update
   */
  boolean supportsNowait();

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
   * otherwise simply return the message from <code>exception</code>
   * @param exception the underlying SQLException
   * @return the message assigned to the given exception
   */
  String getErrorMessage(final SQLException exception);

  /**
   * @param exception the exception
   * @return true if this exception represents a login credentials failure
   */
  boolean isAuthenticationException(final SQLException exception);

  /**
   * Adds any dbms specific connection properties to the given properties map,
   * called each time a connection is created
   * @param properties the properties map to add to
   * @return the properties map
   */
  Properties addConnectionProperties(final Properties properties);

  /**
   * Creates a connection for the given user.
   * @param user the user for which to create a connection
   * @return a Connection
   * @throws DatabaseException in case of a connection error
   */
  Connection createConnection(final User user) throws DatabaseException;

  /**
   * @return the name of the driver class
   */
  String getDriverClassName();

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
