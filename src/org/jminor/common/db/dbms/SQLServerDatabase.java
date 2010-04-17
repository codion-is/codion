/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.jminor.common.model.DateUtil;

import java.text.DateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * A Database implementation based on the SQL Server (2000 or higher) database.
 */
public class SQLServerDatabase extends AbstractDatabase {

  private static final ThreadLocal<DateFormat> dateFormat = DateUtil.getThreadLocalDateFormat("dd-MM-yyyy");//105
  private static final ThreadLocal<DateFormat> timestampFormat = DateUtil.getThreadLocalDateFormat("yyyy-MM-dd HH:mm:ss");//120

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
            "convert(datetime, '" + timestampFormat.get().format(value) + "', 120)" :
            "convert(datetime, '" + dateFormat.get().format(value) + "', 105)";
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

    return "jdbc:sqlserver://" + host + ":" + port + (sid != null && sid.length() > 0 ? ";databaseName=" + sid : "");
  }
}
