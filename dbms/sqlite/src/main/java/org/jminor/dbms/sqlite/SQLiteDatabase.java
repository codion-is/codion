/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.sqlite;

import org.jminor.common.db.database.AbstractDatabase;

import java.sql.SQLException;

/**
 * A SQLite embedded database implementation, quite experimental, based on the xerial/sqlite-jdbc driver.
 */
final class SQLiteDatabase extends AbstractDatabase {

  private static final String AUTO_INCREMENT_QUERY = "select last_insert_rowid()";
  private static final int FOREIGN_KEY_ERROR = 787;

  private static final String JDBC_URL_PREFIX = "jdbc:sqlite:";

  SQLiteDatabase(final String jdbcUrl) {
    super(jdbcUrl);
  }

  @Override
  public String getName() {
    String name = getURL();
    if (name.toLowerCase().startsWith(JDBC_URL_PREFIX)) {
      name = name.substring(JDBC_URL_PREFIX.length());
    }
    if (name.contains(";")) {
      name = name.substring(0, name.indexOf(";"));
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

  /**
   * @param exception the exception
   * @return true if this exception is a referential integrity error
   */
  @Override
  public boolean isReferentialIntegrityException(final SQLException exception) {
    return exception.getErrorCode() == FOREIGN_KEY_ERROR;
  }
}
