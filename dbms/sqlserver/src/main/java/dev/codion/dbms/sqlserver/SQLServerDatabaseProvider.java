/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.dbms.sqlserver;

import dev.codion.common.db.database.Database;
import dev.codion.common.db.database.DatabaseProvider;

import static java.util.Objects.requireNonNull;

/**
 * Provides sql server database implementations
 */
public final class SQLServerDatabaseProvider implements DatabaseProvider {

  private static final String DRIVER_PACKAGE = "com.microsoft.sqlserver.jdbc";

  @Override
  public boolean isDriverCompatible(final String driverClassName) {
    return requireNonNull(driverClassName, "driverClassName").startsWith(DRIVER_PACKAGE);
  }

  @Override
  public boolean isDatabaseCompatible(final Database database) {
    return database instanceof SQLServerDatabase;
  }

  @Override
  public Database createDatabase(final String jdbcUrl) {
    return new SQLServerDatabase(jdbcUrl);
  }
}
