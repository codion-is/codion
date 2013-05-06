/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.jminor.common.db.AbstractDatabase;
import org.jminor.common.model.Util;

import java.util.Properties;

/**
 * A Database implementation based on the SQL Server (2000 or higher) database.
 */
public final class SQLServerDatabase extends AbstractDatabase {

  static final String DRIVER_CLASS_NAME = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
  static final String AUTO_INCREMENT_QUERY = "SELECT SCOPE_IDENTITY()";
  static final String URL_PREFIX = "jdbc:sqlserver://";

  /**
   * Instantiates a new SQLServerDatabase.
   */
  public SQLServerDatabase() {
    super(SQLSERVER, DRIVER_CLASS_NAME);
  }

  /**
   * Instantiates a new SQLServerDatabase.
   * @param host the host name
   * @param port the port number
   * @param databaseName the database name
   */
  public SQLServerDatabase(final String host, final String port, final String databaseName) {
    super(SQLSERVER, DRIVER_CLASS_NAME, host, port, databaseName, false);
  }

  /** {@inheritDoc} */
  @Override
  public String getAutoIncrementValueSQL(final String idSource) {
    return AUTO_INCREMENT_QUERY;
  }

  /** {@inheritDoc} */
  @Override
  public String getURL(final Properties connectionProperties) {
    Util.require("host", getHost());
    Util.require("port", getPort());
    final String sid = getSid();
    return URL_PREFIX + getHost() + ":" + getPort() + (!Util.nullOrEmpty(sid) ? ";databaseName=" + sid : "");
  }
}
