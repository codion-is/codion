/*
 * Copyright (c) 2009, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.jminor.common.model.formats.LongDateFormat;
import org.jminor.common.model.formats.ShortDashDateFormat;

import java.sql.SQLException;
import java.util.Date;
import java.util.Properties;

/**
 * Works for SQL Server 2000 and higher
 */
public class SQLServerDatabase implements IDatabase {

  /** {@inheritDoc} */
  public String getDatabaseType() {
    return DATABASE_TYPE_SQLSERVER;
  }

  /** {@inheritDoc} */
  public void loadDriver() throws ClassNotFoundException {
    Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
  }

  /** {@inheritDoc} */
  public String getAutoIncrementValueSQL(final String idSource) {
    return "SELECT SCOPE_IDENTITY()";
  }

  /** {@inheritDoc} */
  public String getSequenceSQL(final String sequenceName) {
    throw new IllegalArgumentException("Sequence support is not implemented for database type: " + getDatabaseType());
  }

  /** {@inheritDoc} */
  public String getSQLDateString(final Date value, final boolean longDate) {
    return longDate ?
            "convert(datetime, '" + LongDateFormat.get().format(value) + "')" :
            "convert(datetime, '" + ShortDashDateFormat.get().format(value) + "')";
  }

  /** {@inheritDoc} */
  public String getURL(final Properties connectionProperties) {
    final String host = System.getProperty(DATABASE_HOST_PROPERTY);
    if (host == null || host.length() == 0)
      throw new RuntimeException(DATABASE_HOST_PROPERTY + " is required for database type " + getDatabaseType());
    final String port = System.getProperty(DATABASE_PORT_PROPERTY);
    if (port == null || port.length() == 0)
      throw new RuntimeException(DATABASE_PORT_PROPERTY + " is required for database type " + getDatabaseType());
    final String sid = System.getProperty(DATABASE_SID_PROPERTY);
    if (sid == null || sid.length() == 0)
      throw new RuntimeException(DATABASE_SID_PROPERTY + " is required for database type " + getDatabaseType());

    return "jdbc:sqlserver://" + host + ":" + port + ";databaseName=" + sid;
  }

  /** {@inheritDoc} */
  public String getUserInfoString(final Properties connectionProperties) {
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
