/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.model.formats.LongDateFormat;
import org.jminor.common.model.formats.ShortDashDateFormat;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * A class encapsulating database specific code, such as retrieval of auto increment values,
 * string to date conversions and driver class loading.
 * User: Björn Darri
 * Date: 2.2.2008
 * Time: 12:53:14
 */
public class Database {
  /**
   * Oracle database type
   * @see #DATABASE_TYPE_PROPERTY
   */
  public static final String DATABASE_TYPE_ORACLE = "oracle";

  /**
   * MySQL database type
   * @see #DATABASE_TYPE_PROPERTY
   */
  public static final String DATABASE_TYPE_MYSQL = "mysql";

  /**
   * PostgreSQL database type
   * @see #DATABASE_TYPE_PROPERTY
   */
  public static final String DATABASE_TYPE_POSTGRESQL = "postgresql";

  /**
   * Microsoft SQL Server database type
   * @see #DATABASE_TYPE_PROPERTY
   */
  public static final String DATABASE_TYPE_SQL_SERVER = "sqlserver";

  /**
   * Derby database type
   * @see #DATABASE_TYPE_PROPERTY
   */
  public static final String DATABASE_TYPE_DERBY = "derby";

  /**
   * Derby embedded database type
   * @see #DATABASE_TYPE_PROPERTY
   */
  public static final String DATABASE_TYPE_EMBEDDED_DERBY = "derby_embedded";

  /**
   * The driver class name use for Oracle connections
   */
  public static final String ORACLE_DRIVER = "oracle.jdbc.driver.OracleDriver";

  /**
   * The driver class name used for MySQL connections
   */
  public static final String MYSQL_DRIVER = "com.mysql.jdbc.Driver";

  /**
   * The driver class name used for PostgreSQL connections
   */
  public static final String POSTGRESQL_DRIVER = "org.postgresql.Driver";

  /**
   * The driver class name used for SQL Server connections
   */
  public static final String SQL_SERVER_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";

  /**
   * The driver class name used for connections to a networked Derby database (as opposed to embedded)
   */
  public static final String DERBY_DRIVER = "org.apache.derby.jdbc.ClientDriver";

  /**
   * The driver class name used for connections to a embedded Derby database
   */
  public static final String DERBY_EMBEDDED_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";

  /**
   * Specifies the database type
   * @see Database#DATABASE_TYPE_MYSQL
   * @see Database#DATABASE_TYPE_ORACLE
   * @see Database#DATABASE_TYPE_POSTGRESQL
   * @see Database#DATABASE_TYPE_SQL_SERVER
   * @see Database#DATABASE_TYPE_DERBY
   * @see Database#DATABASE_TYPE_EMBEDDED_DERBY
   */
  public static final String DATABASE_TYPE_PROPERTY = "jminor.db.type";

  /**
   * Specifies the machine hosting the database, in the case of embedded Derby databases
   * this specifies the name of the database
   */
  public static final String DATABASE_HOST_PROPERTY = "jminor.db.host";

  /**
   * Specifies the database sid (used for dbname for MySQL connections and Derby server connections)
   */
  public static final String DATABASE_SID_PROPERTY = "jminor.db.sid";

  /**
   * Specifies the database port
   */
  public static final String DATABASE_PORT_PROPERTY = "jminor.db.port";

  /**
   * Represents the supported database types
   */
  public static enum DbType {
    ORACLE, MYSQL, POSTGRESQL, SQLSERVER, DERBY, DERBY_EMBEDDED
  }

  /**
   * The date format used for Derby
   */
  private static DateFormat DERBY_SHORT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

  /**
   * The date format for long dates (timestamps) used by Derby
   */
  private static DateFormat DERBY_LONG_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  /**
   * The active database type
   */
  private static final DbType DB_TYPE = getType();

  /**
   * @return true if the active database is PostgreSQL
   */
  public static boolean isPostgreSQL() {
    return DB_TYPE == DbType.POSTGRESQL;
  }

  /**
   * @return true if the active database is MySQL
   */
  public static boolean isMySQL() {
    return DB_TYPE == DbType.MYSQL;
  }

  /**
   * @return true if the active database is Oracle
   */
  public static boolean isOracle() {
    return DB_TYPE == DbType.ORACLE;
  }

  /**
   * @return true if the active database is SQLServer
   */
  public static boolean isSQLServer() {
    return DB_TYPE == DbType.SQLSERVER;
  }

  /**
   * @return true if the active database is Derby
   */
  public static boolean isDerby() {
    return DB_TYPE == DbType.DERBY;
  }

  /**
   * @return true if the active database is embedded Derby
   */
  public static boolean isDerbyEmbedded() {
    return DB_TYPE == DbType.DERBY_EMBEDDED;
  }

  /**
   * Loads the driver for the active database
   * @throws ClassNotFoundException in case the driver class was not found in the class path
   */
  public static void loadDriver() throws ClassNotFoundException {
    Class.forName(getDriverName());
  }

  /**
   * @param connectionProperties the connection properties, used primarily to provide
   * a derby database with user info for authentication purposes
   * @return the database url of the active database, based on system properties
   */
  public static String getURL(final Properties connectionProperties) {
    final String host = System.getProperty(DATABASE_HOST_PROPERTY);
    if (host == null || host.length() == 0)
      throw new RuntimeException("Required system property missing: " + DATABASE_HOST_PROPERTY);
    final String port = System.getProperty(DATABASE_PORT_PROPERTY);
    if (DB_TYPE != DbType.DERBY_EMBEDDED)
      if (port == null || port.length() == 0)
        throw new RuntimeException("Required system property missing: " + DATABASE_PORT_PROPERTY);
    final String sid = System.getProperty(DATABASE_SID_PROPERTY);
    if (DB_TYPE != DbType.DERBY_EMBEDDED)
      if (sid == null || sid.length() == 0)
        throw new RuntimeException("Required system property missing: " + DATABASE_SID_PROPERTY);

    switch (DB_TYPE) {
      case MYSQL:
        return "jdbc:mysql://" + host + ":" + port + "/" + sid;
      case POSTGRESQL:
        return "jdbc:postgresql://" + host + ":" + port + "/" + sid;
      case ORACLE:
        return "jdbc:oracle:thin:@" + host + ":" + port + ":" + sid;
      case SQLSERVER:
        return "jdbc:sqlserver://" + host + ":" + port + ";databaseName=" + sid;
      case DERBY:
        return "jdbc:derby://" + host + ":" + port + "/" + sid + getUserInfoString(connectionProperties);
      case DERBY_EMBEDDED:
        return "jdbc:derby:" + host + getUserInfoString(connectionProperties);
      default:
        throw new IllegalArgumentException("Database type not supported: " + DB_TYPE);
    }
  }

  /**
   * Returns a query string for retrieving the last automatically generated id from the given id source,
   * in Oracle/PostgreSQL this means the value from a defined sequence, in MySQL the value fetched from last_inserted_id(),
   * in SQL server the last generated IDENTITY value and in Derby the result from IDENTITY_VAL_LOCAL()
   * @param idSource the source for the id, for example a sequence name or in the case of Derby, the name of the table
   * @return a query string for retrieving the last auto-increment value from idSource
   */
  public static String getAutoIncrementValueSQL(final String idSource) {
    switch (DB_TYPE) {
      case MYSQL:
        return "select last_insert_id() from dual";
      case POSTGRESQL:
        return "select currval(" + idSource + ")";
      case ORACLE:
        return "select " + idSource + ".currval from dual";
      case SQLSERVER:
        return "select @@IDENTITY";
      case DERBY:
      case DERBY_EMBEDDED:
        return "select IDENTITY_VAL_LOCAL() from " + idSource;
      default :
        throw new IllegalArgumentException("Database type not supported: " + DB_TYPE);
    }
  }

  public static String getSQLDateString(final Date value, final boolean longDate) {
    switch (DB_TYPE) {
      case MYSQL:
        return longDate ?
                "str_to_date('" + LongDateFormat.get().format(value) + "', '%d-%m-%Y %H:%i')" :
                "str_to_date('" + ShortDashDateFormat.get().format(value) + "', '%d-%m-%Y')";
      case POSTGRESQL:
        return longDate ?
                "to_date('" + LongDateFormat.get().format(value) + "', 'DD-MM-YYYY HH24:MI')" :
                "to_date('" + ShortDashDateFormat.get().format(value) + "', 'DD-MM-YYYY')";
      case ORACLE:
        return longDate ?
                "to_date('" + LongDateFormat.get().format(value) + "', 'DD-MM-YYYY HH24:MI')" :
                "to_date('" + ShortDashDateFormat.get().format(value) + "', 'DD-MM-YYYY')";
      case SQLSERVER:
        return longDate ?
                "convert(datetime, '" + LongDateFormat.get().format(value) + "')" :
                "convert(datetime, '" + ShortDashDateFormat.get().format(value) + "')";
      case DERBY:
      case DERBY_EMBEDDED:
        return longDate ?
                "DATE('" + DERBY_LONG_DATE_FORMAT.format(value) + "')" :
                "DATE('" + DERBY_SHORT_DATE_FORMAT.format(value) + "')";
      default:
        throw new IllegalArgumentException("Database type not supported: " + DB_TYPE);
    }
  }

  public static String getSequenceSQL(final String sequenceName) {
    switch (DB_TYPE) {
      case POSTGRESQL:
        return "select nextval(" + sequenceName + ")";
      case ORACLE:
        return "select " + sequenceName + ".nextval from dual";
      default:
        throw new IllegalArgumentException("Sequence support is not implemented for database type: " + DB_TYPE);
    }
  }

  public static DbType getType() {
    final String dbType = System.getProperty(DATABASE_TYPE_PROPERTY);
    if (dbType == null)
      throw new IllegalArgumentException("Required system property missing: " + DATABASE_TYPE_PROPERTY);

    if (dbType.equals(DATABASE_TYPE_POSTGRESQL))
      return DbType.POSTGRESQL;
    else if (dbType.equals(DATABASE_TYPE_MYSQL))
      return DbType.MYSQL;
    else if (dbType.equals(DATABASE_TYPE_ORACLE))
      return DbType.ORACLE;
    else if (dbType.equals(DATABASE_TYPE_SQL_SERVER))
      return DbType.SQLSERVER;
    else if (dbType.equals(DATABASE_TYPE_DERBY))
      return DbType.DERBY;
    else if (dbType.equals(DATABASE_TYPE_EMBEDDED_DERBY))
      return DbType.DERBY_EMBEDDED;

    throw new IllegalArgumentException("Unknown database type: " + dbType);
  }

  public static void onDisconnect(final Properties connectionProperties) {
    if (isDerbyEmbedded()) {
      try {
        DriverManager.getConnection("jdbc:derby:" + System.getProperty(DATABASE_HOST_PROPERTY) + ";shutdown=true"
                + getUserInfoString(connectionProperties));
      }
      catch (SQLException e) {
        if (e.getSQLState().equals("08006"))//08006 is expected on Derby shutdown
          System.out.println("Embedded Derby database successfully shut down!");
        else
          e.printStackTrace();
      }
    }
  }

  private static String getDriverName() {
    switch (DB_TYPE) {
      case MYSQL:
        return MYSQL_DRIVER;
      case POSTGRESQL:
        return POSTGRESQL_DRIVER;
      case ORACLE:
        return ORACLE_DRIVER;
      case SQLSERVER:
        return SQL_SERVER_DRIVER;
      case DERBY:
        return DERBY_DRIVER;
      case DERBY_EMBEDDED:
        return DERBY_EMBEDDED_DRIVER;
      default:
        throw new IllegalArgumentException("Database type not supported: " + DB_TYPE);
    }
  }

  private static String getUserInfoString(final Properties connectionProperties) {
    if (connectionProperties != null) {
      final String username = (String) connectionProperties.get("user");
      final String password = (String) connectionProperties.get("password");
      if (username != null && username.length() > 0 && password != null && password.length() > 0)
        return ";" + "user=" + username + ";" + "password=" + password;
    }

    return "";
  }
}
