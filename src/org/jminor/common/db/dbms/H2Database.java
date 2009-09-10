/*
 * Copyright (c) 2009, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class H2Database implements Dbms {

  /**
   * The date format used
   */
  private DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

  /**
   * The date format for timestamps
   */
  private DateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  private boolean embedded = System.getProperty(Dbms.DATABASE_EMBEDDED, "false").toUpperCase().equals("TRUE");

  /** {@inheritDoc} */
  public String getDatabaseType() {
    return H2;
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
  public String getSQLDateString(final Date value, final boolean isTimestamp) {
    return isTimestamp ?
            "PARSEDATETIME('" + TIMESTAMP_FORMAT.format(value) + "','yyyy-MM-dd HH:mm:ss')" :
            "PARSEDATETIME('" + DATE_FORMAT.format(value) + "','yyyy-MM-dd')";
  }

  /** {@inheritDoc} */
  public String getURL(final Properties connectionProperties) {
    final String authentication = getAuthenticationInfo(connectionProperties);
    if (isEmbedded()) {
      final String host = System.getProperty(DATABASE_HOST);
      if (host == null || host.length() == 0)
        throw new RuntimeException(DATABASE_HOST + " is required for database type " + getDatabaseType());

      return "jdbc:h2:" + host + (authentication == null ? "" : ";" + authentication);
    }
    else {
      final String host = System.getProperty(DATABASE_HOST);
      if (host == null || host.length() == 0)
        throw new RuntimeException(DATABASE_HOST + " is required for database type " + getDatabaseType());
      final String port = System.getProperty(DATABASE_PORT);
      if (port == null || port.length() == 0)
        throw new RuntimeException(DATABASE_PORT + " is required for database type " + getDatabaseType());
      final String sid = System.getProperty(DATABASE_SID);
      if (sid == null || sid.length() == 0)
        throw new RuntimeException(DATABASE_SID + " is required for database type " + getDatabaseType());

      return "jdbc:h2://" + host + ":" + port + "/" + sid + (authentication == null ? "" : ";" + authentication);
    }
  }

  /** {@inheritDoc} */
  public String getAuthenticationInfo(final Properties connectionProperties) {
    if (connectionProperties != null) {
      final String username = (String) connectionProperties.get("user");
      final String password = (String) connectionProperties.get("password");
      if (username != null && username.length() > 0 && password != null && password.length() > 0)
        return "user=" + username + ";" + "password=" + password;
    }

    return null;
  }

  /** {@inheritDoc} */
  public boolean isEmbedded() {
    return embedded;
  }

  /** {@inheritDoc} */
  public void shutdownEmbedded(final Properties connectionProperties) {}

  /** {@inheritDoc} */
  public boolean supportsNoWait() {
    return true;
  }

  /** {@inheritDoc} */
  public boolean supportsIsValid() {
    return true;
  }

  /** {@inheritDoc} */
  public String getErrorMessage(final SQLException exception) {
    return null;
  }
}
