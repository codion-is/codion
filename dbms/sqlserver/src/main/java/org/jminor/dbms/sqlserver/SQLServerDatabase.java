/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.sqlserver;

import org.jminor.common.db.AbstractDatabase;

import java.sql.SQLException;
import java.util.Properties;

import static java.util.Objects.requireNonNull;
import static org.jminor.common.Util.nullOrEmpty;

/**
 * A Database implementation based on the SQL Server (2000 or higher) database.
 */
public final class SQLServerDatabase extends AbstractDatabase {

  static final String DRIVER_CLASS_NAME = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
  static final String AUTO_INCREMENT_QUERY = "SELECT @@IDENTITY";
  static final String URL_PREFIX = "jdbc:sqlserver://";

  private static final int AUTHENTICATION_ERROR = 18456;
  private static final int REFERENTIAL_INTEGRITY_ERROR = 547;
  private static final int UNIQUE_CONSTRAINT_ERROR1 = 2601;
  private static final int UNIQUE_CONSTRAINT_ERROR2 = 2627;

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
    super(Type.SQLSERVER, DRIVER_CLASS_NAME, requireNonNull(host, "host"),
            requireNonNull(port, "port"), requireNonNull(databaseName, "databaseName"), false);
  }

  @Override
  public boolean supportsSelectForUpdate() {
    return false;
  }

  @Override
  public boolean supportsNowait() {
    return false;
  }

  /**
   * @return true
   */
  @Override
  public boolean subqueryRequiresAlias() {
    return true;
  }

  @Override
  public String getAutoIncrementQuery(final String idSource) {
    return AUTO_INCREMENT_QUERY;
  }

  @Override
  public String getURL(final Properties connectionProperties) {
    final String sid = getSid();
    return URL_PREFIX + getHost() + ":" + getPort() + (!nullOrEmpty(sid) ? ";databaseName=" + sid : "") + getUrlAppend();
  }

  @Override
  public boolean isAuthenticationException(final SQLException exception) {
    return exception.getErrorCode() == AUTHENTICATION_ERROR;
  }

  @Override
  public boolean isReferentialIntegrityException(final SQLException exception) {
    return exception.getErrorCode() == REFERENTIAL_INTEGRITY_ERROR;
  }

  @Override
  public boolean isUniqueConstraintException(final SQLException exception) {
    return exception.getErrorCode() == UNIQUE_CONSTRAINT_ERROR1 || exception.getErrorCode() == UNIQUE_CONSTRAINT_ERROR2;
  }
}
