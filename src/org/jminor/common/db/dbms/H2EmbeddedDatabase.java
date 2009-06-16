package org.jminor.common.db.dbms;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class H2EmbeddedDatabase implements IDatabase {

  /**
   * The date format used
   */
  private DateFormat SHORT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

  /**
   * The date format for long dates (timestamps)
   */
  private DateFormat LONG_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  /** {@inheritDoc} */
  public String getDatabaseType() {
    return DATABASE_TYPE_EMBEDDED_H2;
  }

  /** {@inheritDoc} */
  public void loadDriver() throws ClassNotFoundException {
    Class.forName("org.h2.Driver");
  }

  /** {@inheritDoc} */
  public String getAutoIncrementValueSQL(final String idSource) {
    return "CALL IDENTITY()";
  }

  /** {@inheritDoc} */
  public String getSequenceSQL(final String sequenceName) {
    return "select next value for " + sequenceName;
  }

  /** {@inheritDoc} */
  public String getSQLDateString(final Date value, final boolean longDate) {
    return longDate ?
            "PARSEDATETIME('" + LONG_DATE_FORMAT.format(value) + "','yyyy-MM-dd HH:mm:ss')" :
            "PARSEDATETIME('" + SHORT_DATE_FORMAT.format(value) + "','yyyy-MM-dd')";
  }

  /** {@inheritDoc} */
  public String getURL(final Properties connectionProperties) {
    final String host = System.getProperty(DATABASE_HOST_PROPERTY);
    if (host == null || host.length() == 0)
      throw new RuntimeException(DATABASE_HOST_PROPERTY + " is required for database type " + getDatabaseType());

    return "jdbc:h2:" + host + getUserInfoString(connectionProperties);
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
  public void onDisconnect(final Properties connectionProperties) {}

  /** {@inheritDoc} */
  public boolean supportsNoWait() {
    return true;
  }

  /** {@inheritDoc} */
  public boolean supportsIsValid() {
    return true;
  }
}
