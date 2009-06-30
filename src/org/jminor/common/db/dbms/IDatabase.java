/*
 * Copyright (c) 2009, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

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
   * @see IDatabase#DATABASE_TYPE_DERBY
   * @see IDatabase#DATABASE_TYPE_H2
   * @see IDatabase#DATABASE_TYPE_MYSQL
   * @see IDatabase#DATABASE_TYPE_ORACLE
   * @see IDatabase#DATABASE_TYPE_POSTGRESQL
   * @see IDatabase#DATABASE_TYPE_SQLSERVER
   */
  public String DATABASE_TYPE_PROPERTY = "jminor.db.type";

  /**
   * Specifies the machine hosting the database, in the case of embedded databases
   * this specifies the name of the database
   */
  public String DATABASE_HOST_PROPERTY = "jminor.db.host";

  /**
   * Specifies the database sid (used for dbname for MySQL, SQLServer and Derby server connections)
   */
  public String DATABASE_SID_PROPERTY = "jminor.db.sid";

  /**
   * Specifies the database port
   */
  public String DATABASE_PORT_PROPERTY = "jminor.db.port";

  /**
   * Specifies whether or not the database should be run in embedded mode, if applicable
   * Values: true/false
   * Default: false
   */
  public String DATABASE_EMBEDDED = "jminor.db.embedded";

  /**
   * Specifies the IDatabase implementation class to use in case of a dbms that is not directly supported
   * @see IDatabase
   */
  public String DATABASE_IMPLEMENTATION_CLASS = "jminor.db.implementation";

  /**
   * Oracle database type
   * @see org.jminor.common.db.dbms.IDatabase#DATABASE_TYPE_PROPERTY
   */
  public String DATABASE_TYPE_ORACLE = "oracle";

  /**
   * MySQL database type
   * @see org.jminor.common.db.dbms.IDatabase#DATABASE_TYPE_PROPERTY
   */
  public String DATABASE_TYPE_MYSQL = "mysql";

  /**
   * PostgreSQL database type
   * @see org.jminor.common.db.dbms.IDatabase#DATABASE_TYPE_PROPERTY
   */
  public String DATABASE_TYPE_POSTGRESQL = "postgresql";

  /**
   * Microsoft SQL Server database type
   * @see org.jminor.common.db.dbms.IDatabase#DATABASE_TYPE_PROPERTY
   */
  public String DATABASE_TYPE_SQLSERVER = "sqlserver";

  /**
   * Derby database type
   * @see org.jminor.common.db.dbms.IDatabase#DATABASE_TYPE_PROPERTY
   */
  public String DATABASE_TYPE_DERBY = "derby";

  /**
   * H2 database type
   * @see org.jminor.common.db.dbms.IDatabase#DATABASE_TYPE_PROPERTY
   */
  public String DATABASE_TYPE_H2 = "h2";

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
   * @return a identification string to append to the connection url
   */
  public String getUserInfoString(final Properties connectionProperties);

  /**
   * @return true if this database is an embedded one
   */
  public boolean isEmbedded();

  /**
   * This should shutdown the database in case it is an embedded one
   * and if that is applicaple, such as for Derby.
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
}
