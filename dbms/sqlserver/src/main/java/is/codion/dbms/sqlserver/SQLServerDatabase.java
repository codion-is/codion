/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.dbms.sqlserver;

import is.codion.common.db.database.AbstractDatabase;

import java.sql.SQLException;

/**
 * A Database implementation based on the SQL Server (2000 or higher) database.
 */
final class SQLServerDatabase extends AbstractDatabase {

  static final String AUTO_INCREMENT_QUERY = "SELECT @@IDENTITY";

  private static final int AUTHENTICATION_ERROR = 18456;
  private static final int REFERENTIAL_INTEGRITY_ERROR = 547;
  private static final int UNIQUE_CONSTRAINT_ERROR1 = 2601;
  private static final int UNIQUE_CONSTRAINT_ERROR2 = 2627;

  private static final String JDBC_URL_PREFIX = "jdbc:sqlserver://";

  SQLServerDatabase(final String jdbcUrl) {
    super(jdbcUrl);
  }

  @Override
  public String getName() {
    String name = removeUrlPrefixAndOptions(getUrl(), JDBC_URL_PREFIX);
    if (name.contains("\\")) {
      name = name.substring(name.lastIndexOf('\\') + 1);
      if (name.contains(":")) {
        name = name.substring(0, name.indexOf(':'));
      }
    }

    return name;
  }

  @Override
  public SelectForUpdateSupport getSelectForUpdateSupport() {
    return SelectForUpdateSupport.NONE;
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
