/*
 * Copyright (c) 2009, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import java.sql.SQLException;
import java.util.Date;
import java.util.Properties;

/**
 * User: darri
 * Date: 16.6.2009
 * Time: 08:52:08
 */
public interface IDatabase {

  /**
   * Specifies the database type
   * @see IDatabase#DERBY
   * @see IDatabase#H2
   * @see IDatabase#MYSQL
   * @see IDatabase#ORACLE
   * @see IDatabase#POSTGRESQL
   * @see IDatabase#SQLSERVER
   */
  public static final String DATABASE_TYPE = "jminor.db.type";

  /**
   * Specifies the machine hosting the database, in the case of embedded databases
   * this specifies the name of the database
   */
  public static final String DATABASE_HOST = "jminor.db.host";

  /**
   * Specifies the database sid (used for dbname for MySQL, SQLServer and Derby server connections)
   */
  public static final String DATABASE_SID = "jminor.db.sid";

  /**
   * Specifies the database port
   */
  public static final String DATABASE_PORT = "jminor.db.port";

  /**
   * Specifies whether or not the database should be run in embedded mode, if applicable
   * Values: true/false
   * Default: false
   */
  public static final String DATABASE_EMBEDDED = "jminor.db.embedded";

  /**
   * Specifies the IDatabase implementation class to use in case of a dbms that is not directly supported
   * @see IDatabase
   */
  public static final String DATABASE_IMPLEMENTATION_CLASS = "jminor.db.implementation";

  /**
   * The constant used to denote the Oracle database type
   * @see org.jminor.common.db.dbms.IDatabase#DATABASE_TYPE
   */
  public static final String ORACLE = "oracle";

  /**
   * The constant used to denote the MySQL database type
   * @see org.jminor.common.db.dbms.IDatabase#DATABASE_TYPE
   */
  public static final String MYSQL = "mysql";

  /**
   * The constant used to denote the PostgreSQL database type
   * @see org.jminor.common.db.dbms.IDatabase#DATABASE_TYPE
   */
  public static final String POSTGRESQL = "postgresql";

  /**
   * The constant used to denote the Microsoft SQL Server database type
   * @see org.jminor.common.db.dbms.IDatabase#DATABASE_TYPE
   */
  public static final String SQLSERVER = "sqlserver";

  /**
   * The constant used to denote the Derby database type
   * @see org.jminor.common.db.dbms.IDatabase#DATABASE_TYPE
   */
  public static final String DERBY = "derby";

  /**
   * The constant used to denote the H2 database type
   * @see org.jminor.common.db.dbms.IDatabase#DATABASE_TYPE
   */
  public static final String H2 = "h2";

  /**
   * The constant used to denote the HSQL database type
   * @see org.jminor.common.db.dbms.IDatabase#DATABASE_TYPE
   */
  public static final String HSQL = "hsql";

  /**
   * Loads the database driver
   * @throws ClassNotFoundException in case the driver class in not found
   */
  public void loadDriver() throws ClassNotFoundException;

  /**
   * @return the name of the dbms in use
   */
  public String getDatabaseType();

  /**
   * Returns a query string for retrieving the last automatically generated id from the given id source
   * @param idSource the source for the id, for example a sequence name or in the case of Derby, the name of the table
   * @return a query string for retrieving the last auto-increment value from idSource
   */
  public String getAutoIncrementValueSQL(final String idSource);

  /**
   * @param sequenceName the name of the sequence
   * @return a query for selecting the next value from the given sequence
   */
  public String getSequenceSQL(final String sequenceName);

  /**
   * @param value the date value
   * @param longDate if true then a long date including time is expected
   * @return a sql string for inserting the given date
   */
  public String getSQLDateString(final Date value, final boolean longDate);

  /**
   * @param connectionProperties the connection properties, used primarily to provide
   * a derby database with user info for authentication purposes
   * @return the database url of the active database, based on system properties
   */
  public String getURL(final Properties connectionProperties);

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
  public String getAuthenticationInfo(final Properties connectionProperties);

  /**
   * @return true if this database is an embedded one
   */
  public boolean isEmbedded();

  /**
   * This should shutdown the database in case it is an embedded one
   * and if that is applicable, such as for Derby.
   * @param connectionProperties the connection properties
   */
  public void shutdownEmbedded(final Properties connectionProperties);

  /**
   * @return true if the dbms supports select for update with the nowait option
   */
  public boolean supportsNoWait();

  /**
   * @return true if the dbms supports the Java 6 jdbc call Connection.isValid()
   */
  public boolean supportsIsValid();

  /**
   * Returns a user friendly error message for the given exception
   * @param exception the underlying SQLException
   * @return the message assigned to the given exception
   */
  public String getErrorMessage(final SQLException exception);
}
