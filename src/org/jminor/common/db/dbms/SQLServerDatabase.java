/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import java.util.Properties;

/**
 * A Database implementation based on the SQL Server (2000 or higher) database.
 */
public class SQLServerDatabase extends AbstractDatabase {

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
  public String getURL(final Properties connectionProperties) {
    final String sid = getSid();
    return "jdbc:sqlserver://" + getHost() + ":" + getPort() + (sid != null && sid.length() > 0 ? ";databaseName=" + sid : "");
  }

  @Override
  protected void validate(final String databaseType, final String host, final String port, final String sid, final boolean embedded) {
    require(DATABASE_HOST, host);
    require(DATABASE_PORT, port);
  }
}
