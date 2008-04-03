package org.jminor.common.db;

import org.jminor.common.model.formats.LongDateFormat;
import org.jminor.common.model.formats.ShortDashDateFormat;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Date;

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
   * Derby database type
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
  public static final String DERBY_EMBEDDED_DRIVER = "org.apache.derby.jdbc.ClientDriver";

  /**
   * Specifies the database type
   * @see Database#DATABASE_TYPE_MYSQL
   * @see Database#DATABASE_TYPE_ORACLE
   * @see Database#DATABASE_TYPE_POSTGRESQL
   * @see Database#DATABASE_TYPE_SQL_SERVER
   * @see Database#DATABASE_TYPE_DERBY
   */
  public static final String DATABASE_TYPE_PROPERTY = "jminor.db.type";

  /**
   * Specifies the machine hosting the database
   */
  public static final String DATABASE_HOST_PROPERTY = "jminor.db.host";

  /**
   * Specifies the datbase sid (used for initial for MySQL connections)
   */
  public static final String DATABASE_SID_PROPERTY = "jminor.db.sid";

  /**
   * Specifies the database port
   */
  public static final String DATABASE_PORT_PROPERTY = "jminor.db.port";

  public static enum DbType {
    ORACLE, MYSQL, POSTGRESQL, SQLSERVER, DERBY, DERBY_EMBEDDED
  }

  public static final String TABLE_STATUS_MYSQL = "select count(*), greatest(max(snt), max(ifnull(sbt,snt))) last_change from ";
  public static final String TABLE_STATUS_ORACLE = "select count(*), greatest(max(snt), max(nvl(sbt,snt))) last_change from ";

  private static final DbType DB_TYPE = getType();

  public static boolean isPostgreSQL() {
    return DB_TYPE == DbType.POSTGRESQL;
  }

  public static boolean isMySQL() {
    return DB_TYPE == DbType.MYSQL;
  }

  public static boolean isOracle() {
    return DB_TYPE == DbType.ORACLE;
  }

  public static boolean isSQLServer() {
    return DB_TYPE == DbType.SQLSERVER;
  }

  public static boolean isDerby() {
    return DB_TYPE == DbType.DERBY;
  }

  public static boolean isDerbyEmbedded() {
    return DB_TYPE == DbType.DERBY_EMBEDDED;
  }

  public static void loadDriver() throws ClassNotFoundException {
    Class.forName(getDriverName());
  }

  public static String getURL() {
    final String host = System.getProperty(DATABASE_HOST_PROPERTY);
    if (host == null || host.length() == 0)
      throw new RuntimeException("Required property value not found: " + DATABASE_HOST_PROPERTY);
    final String port = System.getProperty(DATABASE_PORT_PROPERTY);
    if (port == null || port.length() == 0)
      throw new RuntimeException("Required property value not found: " + DATABASE_PORT_PROPERTY);
    final String sid = System.getProperty(DATABASE_SID_PROPERTY);
    if (sid == null || sid.length() == 0)
      throw new RuntimeException("Required property value not found: " + DATABASE_SID_PROPERTY);

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
        return "jdbc:derby://" + host + ":" + port + "/" + sid;
      case DERBY_EMBEDDED:
        return "jdbc:derby://" + host;//todo host should contain database name, document!
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
      case DERBY://todo hmm, no format string?
      case DERBY_EMBEDDED:
        return longDate ?
                "DATE('" + LongDateFormat.get().format(value) + "')" :
                "DATE('" + ShortDashDateFormat.get().format(value) + "')";
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

  public static String getTableStatusQueryString(String tableName) {
    switch (DB_TYPE) {
      case MYSQL:
        return TABLE_STATUS_MYSQL + tableName;
      case ORACLE:
        return TABLE_STATUS_ORACLE + tableName;
      default:
        throw new IllegalArgumentException("Database type does not support table status queries: " + DB_TYPE);
    }
  }

  public static DbType getType() {
    final String dbType = System.getProperty(DATABASE_TYPE_PROPERTY, DATABASE_TYPE_ORACLE);
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

  public static void onDisconnect() {
    if (isDerbyEmbedded()) {
      try {
        DriverManager.getConnection("jdbc:derby:" + System.getProperty(DATABASE_HOST_PROPERTY) + ";shutdown=true");
      }
      catch (SQLException e) {
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
}
