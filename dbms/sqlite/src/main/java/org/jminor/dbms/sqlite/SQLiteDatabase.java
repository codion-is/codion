/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.sqlite;

import org.jminor.common.db.AbstractDatabase;
import org.jminor.common.db.Database;

import java.sql.SQLException;
import java.util.Properties;

import static java.util.Objects.requireNonNull;

/**
 * A SQLite embedded database implementation, quite experimental, based on the xerial/sqlite-jdbc driver.
 */
public final class SQLiteDatabase extends AbstractDatabase {

  private static final String DRIVER_CLASS_NAME = "org.sqlite.JDBC";
  private static final String URL_PREFIX_FILE = "jdbc:sqlite:";
  private static final String AUTO_INCREMENT_QUERY = "select last_insert_rowid()";
  private static final int FOREIGN_KEY_ERROR = 787;

  private final String databaseFilePath;

  /**
   * Instantiates a new embedded SQLiteDatabase, using {@link Database#DATABASE_HOST} for database file path.
   */
  public SQLiteDatabase() {
    this(Database.DATABASE_HOST.get());
  }

  /**
   * Instantiates a new embedded SQLiteDatabase
   * @param databaseFilePath the path to the database file
   */
  public SQLiteDatabase(final String databaseFilePath) {
    super(Type.SQLITE, DRIVER_CLASS_NAME);
    this.databaseFilePath = requireNonNull(databaseFilePath, "databaseFilePath");
  }

  /** {@inheritDoc} */
  @Override
  public String getAutoIncrementQuery(final String idSource) {
    return AUTO_INCREMENT_QUERY;
  }

  /** {@inheritDoc} */
  @Override
  public String getURL(final Properties connectionProperties) {
    return URL_PREFIX_FILE + databaseFilePath + getUrlAppend();
  }

  /** {@inheritDoc} */
  @Override
  public boolean supportsNowait() {
    return false;
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
