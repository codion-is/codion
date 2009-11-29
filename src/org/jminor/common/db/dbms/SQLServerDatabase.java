/*
 * Copyright (c) 2009, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * Works for SQL Server 2000 and higher
 */
public class SQLServerDatabase extends AbstractDatabase {

  private static final ThreadLocal dateFormat = new ThreadLocal() {
    @Override
    protected synchronized Object initialValue() {
      return new SimpleDateFormat("MM-dd-yyyy");//105
    }
  };
  private static final ThreadLocal timestampFormat = new ThreadLocal() {
    @Override
    protected synchronized Object initialValue() {
      return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//120
    }
  };

  public SQLServerDatabase() {
    super(SQLSERVER);
  }

  public SQLServerDatabase(final String host, final String port, final String databaseName) {
    super(SQLSERVER, host, port, databaseName, false);
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
    throw new RuntimeException("Sequence support is not implemented for database type: " + getDatabaseType());
  }

  /** {@inheritDoc} */
  public String getSQLDateString(final Date value, final boolean isTimestamp) {
    return isTimestamp ?
            "convert(datetime, '" + ((DateFormat) timestampFormat.get()).format(value) + "', 120)" :
            "convert(datetime, '" + ((DateFormat) dateFormat.get()).format(value) + "', 105)";
  }

  /** {@inheritDoc} */
  public String getURL(final Properties connectionProperties) {
    final String host = getHost();
    if (host == null || host.length() == 0)
      throw new RuntimeException(DATABASE_HOST + " is required for database type " + getDatabaseType());
    final String port = getPort();
    if (port == null || port.length() == 0)
      throw new RuntimeException(DATABASE_PORT + " is required for database type " + getDatabaseType());
    final String sid = getSid();
    if (sid == null || sid.length() == 0)
      throw new RuntimeException(DATABASE_SID + " is required for database type " + getDatabaseType());

    return "jdbc:sqlserver://" + host + ":" + port + ";databaseName=" + sid;
  }

  /** {@inheritDoc} */
  public String getAuthenticationInfo(final Properties connectionProperties) {
    return null;
  }

  /** {@inheritDoc} */
  public void shutdownEmbedded(final Properties connectionProperties) {}

  /** {@inheritDoc} */
  public boolean supportsIsValid() {
    return true;
  }

  /** {@inheritDoc} */
  public String getErrorMessage(final SQLException exception) {
    return exception.getMessage();
  }
}
