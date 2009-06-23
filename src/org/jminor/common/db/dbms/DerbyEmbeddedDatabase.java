package org.jminor.common.db.dbms;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class DerbyEmbeddedDatabase implements IDatabase {
  /**
   * The date format used for Derby
   */
  private DateFormat DERBY_SHORT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

  /**
   * The date format for long dates (timestamps) used by Derby
   */
  private DateFormat DERBY_LONG_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  /** {@inheritDoc} */
  public String getDatabaseType() {
    return DATABASE_TYPE_EMBEDDED_DERBY;
  }

  /** {@inheritDoc} */
  public void loadDriver() throws ClassNotFoundException {
    Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
  }

  /** {@inheritDoc} */
  public String getAutoIncrementValueSQL(final String idSource) {
    return "select IDENTITY_VAL_LOCAL() from " + idSource;
  }

  /** {@inheritDoc} */
  public String getSequenceSQL(final String sequenceName) {
    throw new IllegalArgumentException("Sequence support is not implemented for database type: " + getDatabaseType());
  }

  /** {@inheritDoc} */
  public String getSQLDateString(final Date value, final boolean longDate) {
    return longDate ?
            "DATE('" + DERBY_LONG_DATE_FORMAT.format(value) + "')" :
            "DATE('" + DERBY_SHORT_DATE_FORMAT.format(value) + "')";
  }

  /** {@inheritDoc} */
  public String getURL(final Properties connectionProperties) {
    final String host = System.getProperty(DATABASE_HOST_PROPERTY);
    if (host == null || host.length() == 0)
      throw new RuntimeException(DATABASE_HOST_PROPERTY + " is required for database type " + getDatabaseType());

    return "jdbc:derby:" + host + getUserInfoString(connectionProperties);
  }

  /** {@inheritDoc} */
  public String getUserInfoString(final Properties connectionProperties) {
    if (connectionProperties != null) {
      final String username = (String) connectionProperties.get("user");
      final String password = (String) connectionProperties.get("password");
      if (username != null && username.length() > 0 && password != null && password.length() > 0)
        return ";" + "user=" + username + ";" + "password=" + password;
    }

    return "";
  }

  /** {@inheritDoc} */
  public boolean isEmbedded() {
    return true;
  }

  /** {@inheritDoc} */
  public void shutdownEmbedded(final Properties connectionProperties) {
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

  /** {@inheritDoc} */
  public boolean supportsNoWait() {
    return false;
  }

  /** {@inheritDoc} */
  public boolean supportsIsValid() {
    return true;
  }
}
