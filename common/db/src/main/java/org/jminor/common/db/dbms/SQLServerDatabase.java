/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.jminor.common.Util;
import org.jminor.common.db.AbstractDatabase;

import java.util.Objects;
import java.util.Properties;

/**
 * A Database implementation based on the SQL Server (2000 or higher) database.
 */
public final class SQLServerDatabase extends AbstractDatabase {

  static final String DRIVER_CLASS_NAME = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
  static final String AUTO_INCREMENT_QUERY = "SELECT SCOPE_IDENTITY()";
  static final String URL_PREFIX = "jdbc:sqlserver://";

  private static final Integer BOOLEAN_TRUE_VALUE = 1;
  private static final Integer BOOLEAN_FALSE_VALUE = 0;

  /**
   * Instantiates a new SQLServerDatabase.
   */
  public SQLServerDatabase() {
    super(Type.SQLSERVER, DRIVER_CLASS_NAME);
  }

  /**
   * Instantiates a new SQLServerDatabase.
   * @param host the host name
   * @param port the port number
   * @param databaseName the database name
   */
  public SQLServerDatabase(final String host, final Integer port, final String databaseName) {
    super(Type.SQLSERVER, DRIVER_CLASS_NAME, Objects.requireNonNull(host, "host"),
            Objects.requireNonNull(port, "port"), Objects.requireNonNull(databaseName, "databaseName"), false);
  }

  /** {@inheritDoc} */
  @Override
  public String getAutoIncrementQuery(final String idSource) {
    return AUTO_INCREMENT_QUERY;
  }

  /** {@inheritDoc} */
  @Override
  public String getURL(final Properties connectionProperties) {
    final String sid = getSid();
    return URL_PREFIX + getHost() + ":" + getPort() + (!Util.nullOrEmpty(sid) ? ";databaseName=" + sid : "");
  }

  /** {@inheritDoc} */
  @Override
  public Object getBooleanTrueValue() {
    return BOOLEAN_TRUE_VALUE;
  }

  /** {@inheritDoc} */
  @Override
  public Object getBooleanFalseValue() {
    return BOOLEAN_FALSE_VALUE;
  }
}
