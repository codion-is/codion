/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.dbms.mysql;

import is.codion.common.db.database.AbstractDatabase;

import java.sql.SQLException;

/**
 * A Database implementation based on the MySQL database.
 */
final class MySQLDatabase extends AbstractDatabase {

  private static final int REFERENTIAL_CONSTRAINT_ERROR = 1452;
  private static final int UNIQUE_CONSTRAINT_ERROR1 = 1062;
  private static final int UNIQUE_CONSTRAINT_ERROR2 = 1586;

  private static final String JDBC_URL_PREFIX = "jdbc:mysql://";

  static final String AUTO_INCREMENT_QUERY = "select last_insert_id() from dual";

  MySQLDatabase(final String jdbcUrl) {
    super(jdbcUrl);
  }

  @Override
  public String getName() {
    String name = removeUrlPrefixAndOptions(getUrl(), JDBC_URL_PREFIX);
    if (name.contains("/")) {
      name = name.substring(name.lastIndexOf('/') + 1);
    }

    return name;
  }

  @Override
  public String getAutoIncrementQuery(final String idSource) {
    return AUTO_INCREMENT_QUERY;
  }

  @Override
  public SelectForUpdateSupport getSelectForUpdateSupport() {
    return SelectForUpdateSupport.FOR_UPDATE;
  }

  @Override
  public boolean isReferentialIntegrityException(final SQLException exception) {
    return exception.getErrorCode() == REFERENTIAL_CONSTRAINT_ERROR;
  }

  @Override
  public boolean isUniqueConstraintException(final SQLException exception) {
    return exception.getErrorCode() == UNIQUE_CONSTRAINT_ERROR1 || exception.getErrorCode() == UNIQUE_CONSTRAINT_ERROR2;
  }
}
