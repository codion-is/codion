/*
 * Copyright (c) 2009 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.dbms.postgresql;

import is.codion.common.db.database.AbstractDatabase;

import java.sql.SQLException;

import static java.util.Objects.requireNonNull;

/**
 * A Database implementation based on the PostgreSQL database.
 */
final class PostgreSQLDatabase extends AbstractDatabase {

  private static final String INVALID_PASS = "28P01";
  private static final String INTEGRITY_CONSTRAINT_VIOLATION = "23000";
  private static final String FOREIGN_KEY_VIOLATION = "23503";
  private static final String UNIQUE_CONSTRAINT_ERROR = "23505";
  private static final String TIMEOUT_ERROR = "57014";//query_canceled

  private static final String JDBC_URL_PREFIX = "jdbc:postgresql://";
  private static final int MAXIMUM_STATEMENT_PARAMETERS = 65_535;

  static final String CHECK_QUERY = "select 1";

  private final boolean nowait;

  PostgreSQLDatabase(String url) {
    this(url, false);
  }

  PostgreSQLDatabase(String url, boolean nowait) {
    super(url);
    this.nowait = nowait;
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
  public String selectForUpdateClause() {
    if (nowait) {
      return FOR_UPDATE_NOWAIT;
    }

    return FOR_UPDATE;
  }

  @Override
  public String limitOffsetClause(Integer limit, Integer offset) {
    return createLimitOffsetClause(limit, offset);
  }

  @Override
  public String autoIncrementQuery(String idSource) {
    return "select currval('" + requireNonNull(idSource, "idSource") + "')";
  }

  @Override
  public String sequenceQuery(String sequenceName) {
    return "select nextval('" + requireNonNull(sequenceName, "sequenceName") + "')";
  }

  @Override
  public boolean isAuthenticationException(SQLException exception) {
    return INVALID_PASS.equals(exception.getSQLState());
  }

  @Override
  public boolean isReferentialIntegrityException(SQLException exception) {
    return INTEGRITY_CONSTRAINT_VIOLATION.equals(exception.getSQLState()) || FOREIGN_KEY_VIOLATION.equals(exception.getSQLState());
  }

  @Override
  public boolean isUniqueConstraintException(SQLException exception) {
    return UNIQUE_CONSTRAINT_ERROR.equals(exception.getSQLState());
  }

  @Override
  public boolean isTimeoutException(SQLException exception) {
    return TIMEOUT_ERROR.equals(exception.getSQLState());
  }

  /**
   * @return false
   */
  @Override
  public boolean supportsIsValid() {
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
  public int maximumNumberOfParameters() {
    return MAXIMUM_STATEMENT_PARAMETERS;
  }

  @Override
  public String checkConnectionQuery() {
    return CHECK_QUERY;
  }
}
