package org.jminor.common.db;

import org.jminor.common.model.formats.LongDateFormat;
import org.jminor.common.model.formats.ShortDashDateFormat;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public abstract class Database {
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
   * H2 database type
   * @see #DATABASE_TYPE_PROPERTY
   */
  public static final String DATABASE_TYPE_H2 = "h2";

  /**
   * H2 embedded database type
   * @see #DATABASE_TYPE_PROPERTY
   */
  public static final String DATABASE_TYPE_EMBEDDED_H2 = "h2_embedded";
  /**
   * Specifies the database type
   * @see Database#DATABASE_TYPE_MYSQL
   * @see Database#DATABASE_TYPE_ORACLE
   * @see Database#DATABASE_TYPE_POSTGRESQL
   * @see Database#DATABASE_TYPE_SQL_SERVER
   * @see Database#DATABASE_TYPE_DERBY
   * @see Database#DATABASE_TYPE_EMBEDDED_DERBY
   * @see Database#DATABASE_TYPE_H2
   * @see Database#DATABASE_TYPE_EMBEDDED_H2
   */
  public static final String DATABASE_TYPE_PROPERTY = "jminor.db.type";

  /**
   * Represents the supported database types
   */
  public static enum DbType {
    ORACLE, MYSQL, POSTGRESQL, SQLSERVER, DERBY, DERBY_EMBEDDED, H2, H2_EMBEDDED
  }

  private static Database instance;

  public static Database get() {
    if (instance == null) {
      final String dbType = System.getProperty(DATABASE_TYPE_PROPERTY);
      if (dbType == null)
        throw new IllegalArgumentException("Required system property missing: " + DATABASE_TYPE_PROPERTY);

      if (dbType.equals(DATABASE_TYPE_POSTGRESQL))
        instance = new PostgreSQLDatabase();
      else if (dbType.equals(DATABASE_TYPE_MYSQL))
        instance = new MySQLDatabase();
      else if (dbType.equals(DATABASE_TYPE_ORACLE))
        instance = new OracleDatabase();
      else if (dbType.equals(DATABASE_TYPE_SQL_SERVER))
        instance = new SQLServerDatabase();
      else if (dbType.equals(DATABASE_TYPE_DERBY))
        instance = new DerbyDatabase();
      else if (dbType.equals(DATABASE_TYPE_EMBEDDED_DERBY))
        instance = new DerbyEmbeddedDatabase();
      else if (dbType.equals(DATABASE_TYPE_H2))
        instance = new H2Database();
      else if (dbType.equals(DATABASE_TYPE_EMBEDDED_H2))
        instance = new H2EmbeddedDatabase();
      else
        throw new IllegalArgumentException("Unknown database type: " + dbType);
    }

    return instance;
  }

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
   * @param connectionProperties the connection properties, used primarily to provide
   * a derby database with user info for authentication purposes
   * @return the database url of the active database, based on system properties
   */
  public abstract String getURL(final Properties connectionProperties);

  /**
   * Returns a query string for retrieving the last automatically generated id from the given id source
   * @param idSource the source for the id, for example a sequence name or in the case of Derby, the name of the table
   * @return a query string for retrieving the last auto-increment value from idSource
   */
  public abstract String getAutoIncrementValueSQL(final String idSource);

  /**
   * @param value the date value
   * @param longDate if true then a long date including time is expected
   * @return a sql string for inserting the given date
   */
  public abstract String getSQLDateString(final Date value, final boolean longDate);

  /**
   * @param sequenceName the name of the sequence
   * @return a query for selecting the next value from the given sequence
   */
  public abstract String getSequenceSQL(final String sequenceName);

  /**
   * @return the database type
   */
  public abstract DbType getType();

  /**
   * @return the JDBC driver name
   */
  public abstract String getDriverName();

  /**
   * @return true if the database is an embedded one
   */
  public boolean isEmbedded() {
    return false;
  }

  /**
   * @param connectionProperties the database connection properties
   * @return a string containing the user info found in <code>connectionProperties</code>
   * to append to the database URL if that applies
   */
  public String getUserInfoString(final Properties connectionProperties) {
    return "";
  }

  /**
   * This method is called when <code>disconnect</code> is called on the database connection
   * @param connectionProperties the database connection properties
   */
  public void onDisconnect(final Properties connectionProperties) {}

  /**
   * Loads the driver
   * @throws ClassNotFoundException in case the driver class in not found
   */
  public void loadDriver() throws ClassNotFoundException {
    Class.forName(getDriverName());
  }

  public static class OracleDatabase extends Database {

    @Override
    public String getAutoIncrementValueSQL(final String idSource) {
      return "select " + idSource + ".currval from dual";
    }

    @Override
    public String getSequenceSQL(final String sequenceName) {
      return "select " + sequenceName + ".nextval from dual";
    }

    @Override
    public String getSQLDateString(final Date value, final boolean longDate) {
      return longDate ?
              "to_date('" + LongDateFormat.get().format(value) + "', 'DD-MM-YYYY HH24:MI')" :
              "to_date('" + ShortDashDateFormat.get().format(value) + "', 'DD-MM-YYYY')";
    }

    @Override
    public DbType getType() {
      return DbType.ORACLE;
    }

    @Override
    public String getURL(final Properties connectionProperties) {
      final String host = System.getProperty(DATABASE_HOST_PROPERTY);
      if (host == null || host.length() == 0)
        throw new RuntimeException(DATABASE_HOST_PROPERTY + " is required for database type " + getType());
      final String port = System.getProperty(DATABASE_PORT_PROPERTY);
      if (port == null || port.length() == 0)
        throw new RuntimeException(DATABASE_PORT_PROPERTY + " is required for database type " + getType());
      final String sid = System.getProperty(DATABASE_SID_PROPERTY);
      if (sid == null || sid.length() == 0)
        throw new RuntimeException(DATABASE_SID_PROPERTY + " is required for database type " + getType());

      return "jdbc:oracle:thin:@" + host + ":" + port + ":" + sid;
    }

    @Override
    public String getDriverName() {
      return "oracle.jdbc.driver.OracleDriver";
    }
  }

  public static class MySQLDatabase extends Database {

    @Override
    public String getAutoIncrementValueSQL(final String idSource) {
      return "select last_insert_id() from dual";
    }

    @Override
    public String getDriverName() {
      return "com.mysql.jdbc.Driver";
    }

    @Override
    public String getSequenceSQL(final String sequenceName) {
      throw new IllegalArgumentException("Sequence support is not implemented for database type: " + getType());
    }

    @Override
    public String getSQLDateString(final Date value, final boolean longDate) {
      return longDate ?
              "str_to_date('" + LongDateFormat.get().format(value) + "', '%d-%m-%Y %H:%i')" :
              "str_to_date('" + ShortDashDateFormat.get().format(value) + "', '%d-%m-%Y')";
    }

    @Override
    public DbType getType() {
      return DbType.MYSQL;
    }

    @Override
    public String getURL(final Properties connectionProperties) {
      final String host = System.getProperty(DATABASE_HOST_PROPERTY);
      if (host == null || host.length() == 0)
        throw new RuntimeException(DATABASE_HOST_PROPERTY + " is required for database type " + getType());
      final String port = System.getProperty(DATABASE_PORT_PROPERTY);
      if (port == null || port.length() == 0)
        throw new RuntimeException(DATABASE_PORT_PROPERTY + " is required for database type " + getType());
      final String sid = System.getProperty(DATABASE_SID_PROPERTY);
      if (sid == null || sid.length() == 0)
        throw new RuntimeException(DATABASE_SID_PROPERTY + " is required for database type " + getType());

      return "jdbc:mysql://" + host + ":" + port + "/" + sid;
    }
  }

  public static class PostgreSQLDatabase extends Database {

    @Override
    public String getAutoIncrementValueSQL(final String idSource) {
      return "select currval(" + idSource + ")";
    }

    @Override
    public String getDriverName() {
      return "org.postgresql.Driver";
    }

    @Override
    public String getSequenceSQL(final String sequenceName) {
      return "select nextval(" + sequenceName + ")";
    }

    @Override
    public String getSQLDateString(final Date value, final boolean longDate) {
      return longDate ?
              "to_date('" + LongDateFormat.get().format(value) + "', 'DD-MM-YYYY HH24:MI')" :
              "to_date('" + ShortDashDateFormat.get().format(value) + "', 'DD-MM-YYYY')";
    }

    @Override
    public DbType getType() {
      return DbType.POSTGRESQL;
    }

    @Override
    public String getURL(final Properties connectionProperties) {
      final String host = System.getProperty(DATABASE_HOST_PROPERTY);
      if (host == null || host.length() == 0)
        throw new RuntimeException(DATABASE_HOST_PROPERTY + " is required for database type " + getType());
      final String port = System.getProperty(DATABASE_PORT_PROPERTY);
      if (port == null || port.length() == 0)
        throw new RuntimeException(DATABASE_PORT_PROPERTY + " is required for database type " + getType());
      final String sid = System.getProperty(DATABASE_SID_PROPERTY);
      if (sid == null || sid.length() == 0)
        throw new RuntimeException(DATABASE_SID_PROPERTY + " is required for database type " + getType());

      return "jdbc:postgresql://" + host + ":" + port + "/" + sid;
    }
  }

  public static class SQLServerDatabase extends Database {

    @Override
    public String getAutoIncrementValueSQL(final String idSource) {
      return "select @@IDENTITY";
    }

    @Override
    public String getDriverName() {
      return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    }

    @Override
    public String getSequenceSQL(final String sequenceName) {
      throw new IllegalArgumentException("Sequence support is not implemented for database type: " + getType());
    }

    @Override
    public String getSQLDateString(final Date value, final boolean longDate) {
      return longDate ?
              "convert(datetime, '" + LongDateFormat.get().format(value) + "')" :
              "convert(datetime, '" + ShortDashDateFormat.get().format(value) + "')";
    }

    @Override
    public DbType getType() {
      return DbType.SQLSERVER;
    }

    @Override
    public String getURL(final Properties connectionProperties) {
      final String host = System.getProperty(DATABASE_HOST_PROPERTY);
      if (host == null || host.length() == 0)
        throw new RuntimeException(DATABASE_HOST_PROPERTY + " is required for database type " + getType());
      final String port = System.getProperty(DATABASE_PORT_PROPERTY);
      if (port == null || port.length() == 0)
        throw new RuntimeException(DATABASE_PORT_PROPERTY + " is required for database type " + getType());
      final String sid = System.getProperty(DATABASE_SID_PROPERTY);
      if (sid == null || sid.length() == 0)
        throw new RuntimeException(DATABASE_SID_PROPERTY + " is required for database type " + getType());

      return "jdbc:sqlserver://" + host + ":" + port + ";databaseName=" + sid;
    }
  }

  public static class DerbyDatabase extends Database {
    /**
     * The date format used for Derby
     */
    private DateFormat DERBY_SHORT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * The date format for long dates (timestamps) used by Derby
     */
    private DateFormat DERBY_LONG_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public String getAutoIncrementValueSQL(final String idSource) {
      return "select IDENTITY_VAL_LOCAL() from " + idSource;
    }

    @Override
    public String getDriverName() {
      return "org.apache.derby.jdbc.ClientDriver";
    }

    @Override
    public String getSequenceSQL(final String sequenceName) {
      throw new IllegalArgumentException("Sequence support is not implemented for database type: " + getType());
    }

    @Override
    public String getSQLDateString(final Date value, final boolean longDate) {
      return longDate ?
              "DATE('" + DERBY_LONG_DATE_FORMAT.format(value) + "')" :
              "DATE('" + DERBY_SHORT_DATE_FORMAT.format(value) + "')";
    }

    @Override
    public DbType getType() {
      return DbType.DERBY;
    }

    @Override
    public String getURL(final Properties connectionProperties) {
      final String host = System.getProperty(DATABASE_HOST_PROPERTY);
      if (host == null || host.length() == 0)
        throw new RuntimeException(DATABASE_HOST_PROPERTY + " is required for database type " + getType());
      final String port = System.getProperty(DATABASE_PORT_PROPERTY);
      if (port == null || port.length() == 0)
        throw new RuntimeException(DATABASE_PORT_PROPERTY + " is required for database type " + getType());
      final String sid = System.getProperty(DATABASE_SID_PROPERTY);
      if (sid == null || sid.length() == 0)
        throw new RuntimeException(DATABASE_SID_PROPERTY + " is required for database type " + getType());

      return "jdbc:derby://" + host + ":" + port + "/" + sid + getUserInfoString(connectionProperties);
    }

    @Override
    public String getUserInfoString(final Properties connectionProperties) {
      if (connectionProperties != null) {
        final String username = (String) connectionProperties.get("user");
        final String password = (String) connectionProperties.get("password");
        if (username != null && username.length() > 0 && password != null && password.length() > 0)
          return ";" + "user=" + username + ";" + "password=" + password;
      }

      return "";
    }
  }

  public static class DerbyEmbeddedDatabase extends Database {
    /**
     * The date format used for Derby
     */
    private DateFormat DERBY_SHORT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * The date format for long dates (timestamps) used by Derby
     */
    private DateFormat DERBY_LONG_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public String getAutoIncrementValueSQL(final String idSource) {
      return "select IDENTITY_VAL_LOCAL() from " + idSource;
    }

    @Override
    public String getDriverName() {
      return "org.apache.derby.jdbc.EmbeddedDriver";
    }

    @Override
    public String getSequenceSQL(final String sequenceName) {
      throw new IllegalArgumentException("Sequence support is not implemented for database type: " + getType());
    }

    @Override
    public String getSQLDateString(final Date value, final boolean longDate) {
      return longDate ?
              "DATE('" + DERBY_LONG_DATE_FORMAT.format(value) + "')" :
              "DATE('" + DERBY_SHORT_DATE_FORMAT.format(value) + "')";
    }

    @Override
    public DbType getType() {
      return DbType.DERBY_EMBEDDED;
    }

    @Override
    public boolean isEmbedded() {
      return true;
    }

    @Override
    public String getURL(final Properties connectionProperties) {
      final String host = System.getProperty(DATABASE_HOST_PROPERTY);
      if (host == null || host.length() == 0)
        throw new RuntimeException(DATABASE_HOST_PROPERTY + " is required for database type " + getType());

      return "jdbc:derby:" + host + getUserInfoString(connectionProperties);
    }

    @Override
    public void onDisconnect(final Properties connectionProperties) {
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

    @Override
    public String getUserInfoString(final Properties connectionProperties) {
      if (connectionProperties != null) {
        final String username = (String) connectionProperties.get("user");
        final String password = (String) connectionProperties.get("password");
        if (username != null && username.length() > 0 && password != null && password.length() > 0)
          return ";" + "user=" + username + ";" + "password=" + password;
      }

      return "";
    }
  }

  public static class H2Database extends Database {

    /**
     * The date format used
     */
    private DateFormat SHORT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * The date format for long dates (timestamps)
     */
    private DateFormat LONG_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public String getAutoIncrementValueSQL(final String idSource) {
      return "CALL IDENTITY()";
    }

    @Override
    public String getDriverName() {
      return "org.h2.Driver";
    }

    @Override
    public String getSequenceSQL(final String sequenceName) {
      return "select next value for " + sequenceName;
    }

    @Override
    public String getSQLDateString(final Date value, final boolean longDate) {
      return longDate ?
              "PARSEDATETIME('" + LONG_DATE_FORMAT.format(value) + "','yyyy-MM-dd HH:mm:ss')" :
              "PARSEDATETIME('" + SHORT_DATE_FORMAT.format(value) + "','yyyy-MM-dd')";
    }

    @Override
    public DbType getType() {
      return DbType.H2;
    }

    @Override
    public String getURL(final Properties connectionProperties) {
      final String host = System.getProperty(DATABASE_HOST_PROPERTY);
      if (host == null || host.length() == 0)
        throw new RuntimeException(DATABASE_HOST_PROPERTY + " is required for database type " + getType());
      final String port = System.getProperty(DATABASE_PORT_PROPERTY);
      if (port == null || port.length() == 0)
        throw new RuntimeException(DATABASE_PORT_PROPERTY + " is required for database type " + getType());
      final String sid = System.getProperty(DATABASE_SID_PROPERTY);
      if (sid == null || sid.length() == 0)
        throw new RuntimeException(DATABASE_SID_PROPERTY + " is required for database type " + getType());

      return "jdbc:h2://" + host + ":" + port + "/" + sid + getUserInfoString(connectionProperties);
    }

    @Override
    public String getUserInfoString(final Properties connectionProperties) {
      if (connectionProperties != null) {
        final String username = (String) connectionProperties.get("user");
        final String password = (String) connectionProperties.get("password");
        if (username != null && username.length() > 0 && password != null && password.length() > 0)
          return ";" + "user=" + username + ";" + "password=" + password;
      }

      return "";
    }
  }

  public static class H2EmbeddedDatabase extends Database {

    /**
     * The date format used
     */
    private DateFormat SHORT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * The date format for long dates (timestamps)
     */
    private DateFormat LONG_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public String getAutoIncrementValueSQL(final String idSource) {
      return "CALL IDENTITY()";
    }

    @Override
    public String getDriverName() {
      return "org.h2.Driver";
    }

    @Override
    public String getSequenceSQL(final String sequenceName) {
      return "select next value for " + sequenceName;
    }

    @Override
    public String getSQLDateString(final Date value, final boolean longDate) {
      return longDate ?
              "PARSEDATETIME('" + LONG_DATE_FORMAT.format(value) + "','yyyy-MM-dd HH:mm:ss')" :
              "PARSEDATETIME('" + SHORT_DATE_FORMAT.format(value) + "','yyyy-MM-dd')";
    }

    @Override
    public DbType getType() {
      return DbType.H2_EMBEDDED;
    }

    @Override
    public boolean isEmbedded() {
      return true;
    }

    @Override
    public String getURL(final Properties connectionProperties) {
      final String host = System.getProperty(DATABASE_HOST_PROPERTY);
      if (host == null || host.length() == 0)
        throw new RuntimeException(DATABASE_HOST_PROPERTY + " is required for database type " + getType());

      return "jdbc:h2:" + host + getUserInfoString(connectionProperties);
    }
  }
}