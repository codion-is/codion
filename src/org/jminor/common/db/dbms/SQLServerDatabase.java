/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.jminor.common.model.Util;

import java.util.Properties;

/**
 * A Database implementation based on the SQL Server (2000 or higher) database.
 */
public final class SQLServerDatabase extends AbstractDatabase {

  public SQLServerDatabase() {
    super(SQLSERVER);
  }

  public SQLServerDatabase(final String host, final String port, final String databaseName) {
    super(SQLSERVER, host, port, databaseName, false);
  }

  public void loadDriver() throws ClassNotFoundException {
    Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
  }

  public String getAutoIncrementValueSQL(final String idSource) {
    return "SELECT SCOPE_IDENTITY()";
  }

  public String getSequenceSQL(final String sequenceName) {
    throw new RuntimeException("Sequence support is not implemented for database type: " + getDatabaseType());
  }

  public String getURL(final Properties connectionProperties) {
    final String sid = getSid();
    return "jdbc:sqlserver://" + getHost() + ":" + getPort() + (!Util.nullOrEmpty(sid) ? ";databaseName=" + sid : "");
  }

  @Override
  protected void validate(final String databaseType, final String host, final String port, final String sid, final boolean embedded) {
    Util.require(DATABASE_HOST, host);
    Util.require(DATABASE_PORT, port);
  }
}
