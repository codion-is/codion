/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.dbms.sqlite;

import is.codion.common.db.database.Database;
import is.codion.common.db.database.DatabaseFactory;

import static java.util.Objects.requireNonNull;

/**
 * Provides sqlite database implementations
 */
public final class SQLiteDatabaseFactory implements DatabaseFactory {

  private static final String DRIVER_PACKAGE = "org.sqlite";

  @Override
  public boolean isDriverCompatible(final String driverClassName) {
    return requireNonNull(driverClassName, "driverClassName").startsWith(DRIVER_PACKAGE);
  }

  @Override
  public Database createDatabase(final String jdbcUrl) {
    return new SQLiteDatabase(jdbcUrl);
  }
}
