/*
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.dbms.db2database;

import is.codion.common.db.database.AbstractDatabase;

import java.sql.SQLException;

import static java.util.Objects.requireNonNull;

/**
 * A Db2 database implementation.
 */
final class Db2Database extends AbstractDatabase {

  private static final int REFERENTIAL_INTEGRITY_INSERT_UPDATE = -530;
  private static final int REFERENTIAL_INTEGRITY_DELETE_1 = -532;
  private static final int REFERENTIAL_INTEGRITY_DELETE_2 = -536;
  private static final int UNIQUE_CONSTRAINT = -803;
  private static final int AUTHENTICATION_ERROR = -4214;
  private static final int TIMEOUT_ERROR_1 = -911;
  private static final int TIMEOUT_ERROR_2 = -913;

  private static final String JDBC_URL_PREFIX = "jdbc:db2:";

  Db2Database(String url) {
    super(url);
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
    return "select previous value for " + requireNonNull(idSource, "idSource");
  }

  @Override
  public String sequenceQuery(String sequenceName) {
    return "select next value for " + requireNonNull(sequenceName, "sequenceName");
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
    int errorCode = exception.getErrorCode();

    return errorCode == REFERENTIAL_INTEGRITY_INSERT_UPDATE ||
            errorCode == REFERENTIAL_INTEGRITY_DELETE_1 ||
            errorCode == REFERENTIAL_INTEGRITY_DELETE_2;
  }

  @Override
  public boolean isUniqueConstraintException(SQLException exception) {
    return exception.getErrorCode() == UNIQUE_CONSTRAINT;
  }

  @Override
  public boolean isAuthenticationException(SQLException exception) {
    return exception.getErrorCode() == AUTHENTICATION_ERROR;
  }

  @Override
  public boolean isTimeoutException(SQLException exception) {
    int errorCode = exception.getErrorCode();

    return errorCode == TIMEOUT_ERROR_1 ||
            errorCode == TIMEOUT_ERROR_2;
  }
}
