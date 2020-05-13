/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.postgresql;

import org.jminor.common.db.database.Database;
import org.jminor.common.db.database.DatabaseProvider;

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
