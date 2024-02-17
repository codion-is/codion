/*
 * Copyright (c) 2018 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.dbms.sqlite;

import is.codion.common.db.database.AbstractDatabase;

import java.sql.SQLException;

import static java.util.Objects.requireNonNull;

/**
 * A SQLite embedded database implementation, quite experimental, based on the xerial/sqlite-jdbc driver.
 */
final class SQLiteDatabase extends AbstractDatabase {

  private static final String AUTO_INCREMENT_QUERY = "SELECT LAST_INSERT_ROWID()";
  private static final int FOREIGN_KEY_ERROR = 787;

  private static final String JDBC_URL_PREFIX = "jdbc:sqlite:";

  SQLiteDatabase(String url) {
    super(url);
  }

  @Override
  public String name() {
    return removeUrlPrefixOptionsAndParameters(url(), JDBC_URL_PREFIX);
  }

  @Override
  public String autoIncrementQuery(String idSource) {
    return AUTO_INCREMENT_QUERY;
  }

  @Override
  public String selectForUpdateClause() {
    return FOR_UPDATE;
  }

  @Override
  public String limitOffsetClause(Integer limit, Integer offset) {
    return createLimitOffsetClause(limit, offset);
  }

  /**
   * @param exception the exception
   * @return true if this exception is a referential integrity error
   */
  @Override
  public boolean isReferentialIntegrityException(SQLException exception) {
    return requireNonNull(exception).getErrorCode() == FOREIGN_KEY_ERROR;
  }
}
