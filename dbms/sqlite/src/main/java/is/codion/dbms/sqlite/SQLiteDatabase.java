/*
 * Copyright (c) 2018 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.dbms.sqlite;

import is.codion.common.db.database.AbstractDatabase;

import java.sql.SQLException;

/**
 * A SQLite embedded database implementation, quite experimental, based on the xerial/sqlite-jdbc driver.
 */
final class SQLiteDatabase extends AbstractDatabase {

  private static final String AUTO_INCREMENT_QUERY = "select last_insert_rowid()";
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
    return "for update";
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
    return exception.getErrorCode() == FOREIGN_KEY_ERROR;
  }
}
