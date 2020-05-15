/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.dbms.postgresql;

import is.codion.common.db.database.Database;
import is.codion.common.db.database.DatabaseProvider;

import static java.util.Objects.requireNonNull;

/**
 * Provides postgresql database implementations
 */
public final class PostgreSQLDatabaseProvider implements DatabaseProvider {

  private static final String DRIVER_PACKAGE = "org.postgresql";

  @Override
  public boolean isDriverCompatible(final String driverClassName) {
    return requireNonNull(driverClassName, "driverClassName").startsWith(DRIVER_PACKAGE);
  }

  @Override
  public boolean isDatabaseCompatible(final Database database) {
    return database instanceof PostgreSQLDatabase;
  }

  @Override
  public Database createDatabase(final String jdbcUrl) {
    return new PostgreSQLDatabase(jdbcUrl);
  }
}
