/*
 * Copyright (c) 2009, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.jminor.common.model.formats.ShortDashDateFormat;
import org.jminor.common.model.formats.TimestampFormat;

import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Properties;

public class MySQLDatabase implements IDatabase {

  private static final DateFormat TIMESTAMP_FORMAT = new TimestampFormat();
  private static final DateFormat DATE_FORMAT = new ShortDashDateFormat();

  /** {@inheritDoc} */
  public String getDatabaseType() {
    return MYSQL;
  }

  /** {@inheritDoc} */
  public void loadDriver() throws ClassNotFoundException {
    Class.forName("com.mysql.jdbc.Driver");
  }

  /** {@inheritDoc} */
  public String getAutoIncrementValueSQL(final String idSource) {
    return "select last_insert_id() from dual";
  }

  /** {@inheritDoc} */
  public String getSequenceSQL(final String sequenceName) {
    throw new IllegalArgumentException("Sequence support is not implemented for database type: " + getDatabaseType());
  }

  /** {@inheritDoc} */
  public String getSQLDateString(final Date value, final boolean isTimestamp) {
    return isTimestamp ?
            "str_to_date('" + TIMESTAMP_FORMAT.format(value) + "', '%d-%m-%Y %H:%i')" :
            "str_to_date('" + DATE_FORMAT.format(value) + "', '%d-%m-%Y')";
  }

  /** {@inheritDoc} */
  public String getURL(final Properties connectionProperties) {
    final String host = System.getProperty(DATABASE_HOST);
    if (host == null || host.length() == 0)
      throw new RuntimeException(DATABASE_HOST + " is required for database type " + getDatabaseType());
    final String port = System.getProperty(DATABASE_PORT);
    if (port == null || port.length() == 0)
      throw new RuntimeException(DATABASE_PORT + " is required for database type " + getDatabaseType());
    final String sid = System.getProperty(DATABASE_SID);
    if (sid == null || sid.length() == 0)
      throw new RuntimeException(DATABASE_SID + " is required for database type " + getDatabaseType());

    return "jdbc:mysql://" + host + ":" + port + "/" + sid;
  }

  /** {@inheritDoc} */
  public String getAuthenticationInfo(final Properties connectionProperties) {
    return null;
  }

  /** {@inheritDoc} */
  public boolean isEmbedded() {
    return false;
  }

  /** {@inheritDoc} */
  public void shutdownEmbedded(final Properties connectionProperties) {}

  /** {@inheritDoc} */
  public boolean supportsNoWait() {
    return false;
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
