/*
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.dbms.mariadb;

import is.codion.common.db.database.AbstractDatabase;

import java.sql.SQLException;

/**
 * A Database implementation based on the MariaDB database.
 */
final class MariaDBDatabase extends AbstractDatabase {

  private static final int REFERENTIAL_CONSTRAINT_ERROR = 1452;
  private static final int UNIQUE_CONSTRAINT_ERROR1 = 1062;
  private static final int UNIQUE_CONSTRAINT_ERROR2 = 1586;
  private static final int TIMEOUT_ERROR = 1969;

  private static final String JDBC_URL_PREFIX = "jdbc:mariadb://";

  static final String AUTO_INCREMENT_QUERY = "select last_insert_id() from dual";

  MariaDBDatabase(String jdbUrl) {
    super(jdbUrl);
  }

  @Override
  public String name() {
    String name = removeUrlPrefixOptionsAndParameters(url(), JDBC_URL_PREFIX);
    if (name.contains("/")) {
      name = name.substring(name.lastIndexOf('/') + 1);
    }

    return name;
  }

  @Override
  public String autoIncrementQuery(String idSource) {
    return AUTO_INCREMENT_QUERY;
  }

  @Override
  public String selectForUpdateClause() {
    return "for update";
  }

  @Override
  public String limitOffsetClause(Integer limit, Integer offset) {
    return createLimitOffsetClause(limit, offset);
  }

  @Override
  public boolean isReferentialIntegrityException(SQLException exception) {
    return exception.getErrorCode() == REFERENTIAL_CONSTRAINT_ERROR;
  }

  @Override
  public boolean isUniqueConstraintException(SQLException exception) {
    return exception.getErrorCode() == UNIQUE_CONSTRAINT_ERROR1 || exception.getErrorCode() == UNIQUE_CONSTRAINT_ERROR2;
  }

  @Override
  public boolean isTimeoutException(SQLException exception) {
    return exception.getErrorCode() == TIMEOUT_ERROR;
  }
}
