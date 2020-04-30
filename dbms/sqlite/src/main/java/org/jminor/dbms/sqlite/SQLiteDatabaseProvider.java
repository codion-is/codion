/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.sqlite;

import org.jminor.common.db.database.Database;
import org.jminor.common.db.database.DatabaseProvider;

import static java.util.Objects.requireNonNull;

/**
 * Provides sqlite database implementations
 */
public final class SQLiteDatabaseProvider implements DatabaseProvider {

  private static final String DRIVER_PACKAGE = "org.sqlite";

  @Override
  public boolean isDriverCompatible(final String driverClassName) {
    return requireNonNull(driverClassName, "driverClass").startsWith(DRIVER_PACKAGE);
  }

  @Override
  public String getDatabaseClassName() {
    return SQLiteDatabase.class.getName();
  }

  @Override
  public Database createDatabase() {
    return new SQLiteDatabase(requireNonNull(Database.DATABASE_URL.get(), Database.DATABASE_URL.getProperty()));
  }
}
