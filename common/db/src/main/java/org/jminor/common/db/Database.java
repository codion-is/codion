/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.Configuration;
import org.jminor.common.PropertyValue;
import org.jminor.common.User;
import org.jminor.common.db.exception.DatabaseException;

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
    DERBY, H2, HSQL, MYSQL, ORACLE, POSTGRESQL, SQLSERVER, SQLITE, OTHER
  }

  /**
   * Specifies the database type by name, this property is case insensitive
   * @see Type#DERBY
   * @see Type#HSQL
   * @see Type#H2
   * @see Type#MYSQL
   * @see Type#ORACLE
   * @see Type#POSTGRESQL
   * @see Type#SQLSERVER
   * @see Type#SQLITE
   */
  PropertyValue<String> DATABASE_TYPE = Configuration.stringValue("jminor.db.type", null);

  /**
   * Specifies the machine hosting the database, in the case of embedded databases
   * this specifies the name of the database
   */
  PropertyValue<String> DATABASE_HOST = Configuration.stringValue("jminor.db.host", null);

  /**
   * Specifies the database sid (used for dbname for MySQL, SQLServer and Derby server connections)
   */
  PropertyValue<String> DATABASE_SID = Configuration.stringValue("jminor.db.sid", null);

  /**
   * Specifies the database port
   */
  PropertyValue<Integer> DATABASE_PORT = Configuration.integerValue("jminor.db.port", null);

  /**
   * Specifies whether or not the database should be run in embedded mode, if applicable<br>
   * Values: "true"/"false"<br>
   * Default: "false"<br>
   */
  PropertyValue<Boolean> DATABASE_EMBEDDED = Configuration.booleanValue("jminor.db.embedded", false);

  /**
   * Specifies whether or not the database should be run in in-memory mode<br>
   * Values: "true"/"false"<br>
   * Default: "false"<br>
   */
  PropertyValue<Boolean> DATABASE_EMBEDDED_IN_MEMORY = Configuration.booleanValue("jminor.db.embeddedInMemory", false);

  /**
   * A comma separated list of paths to scripts to run when initializing the database, implementation specific
   */
  PropertyValue<String> DATABASE_INIT_SCRIPT = Configuration.stringValue("jminor.db.initScript", null);

  /**
   * Specifies the Database implementation class to use in case of a dbms that is not directly supported
   * @see Database
   */
  PropertyValue<String> DATABASE_IMPLEMENTATION_CLASS = Configuration.stringValue("jminor.db.implementation", getDatabaseClassName());

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
  Type getType();

  /**
   * @return the database host name
   */
  String getHost();

  /**
   * @return the database port
   */
  Integer getPort();

  /**
   * @return the database service id
   */
  String getSid();

  /**
   * @return true if this database is an embedded one
   */
  boolean isEmbedded();

  /**
   * @param urlAppend a string to append to the connection URL
   */
  void setUrlAppend(final String urlAppend);

  /**
   * @return the string to append to the connection URL
   */
  String getUrlAppend();

  /**
   * Returns a query string for retrieving the last automatically generated id from the given id source
   * @param idSource the source for the id, for example a sequence name or in the case of Derby, the name of the table auto generating the value
   * @return a query string for retrieving the last auto-increment value from idSource
   * @throws NullPointerException in case {@code idSource} is required and is null
   */
  String getAutoIncrementQuery(final String idSource);

  /**
   * @param sequenceName the name of the sequence
   * @return a query for selecting the next value from the given sequence
   * @throws UnsupportedOperationException in case the underlying database does not support sequences
   * @throws NullPointerException in case {@code sequenceName} is null
   */
  String getSequenceQuery(final String sequenceName);

  /**
   * @param connectionProperties the connection properties, used primarily to provide
   * embedded databases with user info for authentication purposes
   * @return the database url of the active database, based on system properties
   */
  String getURL(final Properties connectionProperties);

  /**
   * In the case of embedded databases the user login info can be appended
   * to the connection url, this method should return that string in case
   * the dbms requires it and {@code connectionProperties} contains
   * the username and password info, otherwise it should be safe to return null.
   * This method is usually used in {@code getURL()} and {@code onDisconnect()}.
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
   * @return true if the dbms supports the 'select for update' syntax
   */
  boolean supportsSelectForUpdate();

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
   * otherwise simply return the message from {@code exception}
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
   * @param exception the exception
   * @return true if this exception is a referential integrity exception
   */
  boolean isReferentialIntegrityException(final SQLException exception);

  /**
   * @param exception the exception
   * @return true if this exception is a unique key exception
   */
  boolean isUniqueConstraintException(final SQLException exception);

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
   * @throws org.jminor.common.db.exception.AuthenticationException in case of an authentication error
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

  /**
   * @return true if the configuration value {@link Database#DATABASE_TYPE} is available as a system property
   */
  static boolean isDatabaseTypeSpecified() {
    return Database.DATABASE_TYPE.get() != null;
  }

  /**
   * @return the database implementation class name associated with the specified database type, null if none is specified
   */
  static String getDatabaseClassName() {
    if (!isDatabaseTypeSpecified()) {
      return null;
    }
    final Database.Type dbType = Database.getDatabaseType();
    switch (dbType) {
      case POSTGRESQL:
        return "org.jminor.dbms.postgresql.PostgreSQLDatabase";
      case MYSQL:
        return "org.jminor.dbms.mysql.MySQLDatabase";
      case ORACLE:
        return "org.jminor.dbms.oracle.OracleDatabase";
      case SQLSERVER:
        return "org.jminor.dbms.sqlserver.SQLServerDatabase";
      case DERBY:
        return "org.jminor.dbms.derby.DerbyDatabase";
      case H2:
        return "org.jminor.dbms.h2database.H2Database";
      case HSQL:
        return "org.jminor.dbms.hsqldb.HSQLDatabase";
      case SQLITE:
        return "org.jminor.dbms.sqlite.SQLiteDatabase";
      case OTHER:
        throw new IllegalArgumentException("Database type OTHER does not have an implementation");
      default:
        throw new IllegalArgumentException("Unknown database type: " + dbType);
    }
  }

  /**
   * @return the database type string as specified by the DATABASE_TYPE system property
   * @see Database#DATABASE_TYPE
   */
  static Database.Type getDatabaseType() {
    final String dbType = Database.DATABASE_TYPE.get();
    if (dbType == null) {
      throw new IllegalArgumentException("Required system property missing: " + Database.DATABASE_TYPE);
    }

    return Database.Type.valueOf(dbType.trim().toUpperCase());
  }
}
