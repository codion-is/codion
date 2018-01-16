/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.dbms;

import org.jminor.common.db.AbstractDatabase;
import org.jminor.common.db.Database;

import java.util.Objects;
import java.util.Properties;

/**
 * A SQLite embedded database implementation, quite experimental, based on the xerial/sqlite-jdbc driver.
 */
public final class SQLiteDatabase extends AbstractDatabase {

  private static final String DRIVER_CLASS_NAME = "org.sqlite.JDBC";
  private static final String URL_PREFIX_FILE = "jdbc:sqlite:";
  private static final String AUTO_INCREMENT_QUERY = "select last_insert_rowid()";

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
    this.databaseFilePath = Objects.requireNonNull(databaseFilePath, "databaseFilePath");
  }

  /** {@inheritDoc} */
  @Override
  public String getAutoIncrementQuery(final String idSource) {
    return AUTO_INCREMENT_QUERY;
  }

  /** {@inheritDoc} */
  @Override
  public String getURL(final Properties connectionProperties) {
    return URL_PREFIX_FILE + databaseFilePath;
  }

  /** {@inheritDoc} */
  @Override
  public boolean supportsNowait() {
    return false;
  }
}
