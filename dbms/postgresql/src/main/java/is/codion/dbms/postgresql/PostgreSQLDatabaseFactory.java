/*
 * Copyright (c) 2019 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.dbms.postgresql;

import is.codion.common.db.database.Database;
import is.codion.common.db.database.DatabaseFactory;

import static java.util.Objects.requireNonNull;

/**
 * Provides postgresql database implementations
 */
public final class PostgreSQLDatabaseFactory implements DatabaseFactory {

  private static final String DRIVER_PACKAGE = "org.postgresql";

  @Override
  public boolean isDriverCompatible(String driverClassName) {
    return requireNonNull(driverClassName, "driverClassName").startsWith(DRIVER_PACKAGE);
  }

  @Override
  public Database createDatabase(String jdbcUrl) {
    return new PostgreSQLDatabase(jdbcUrl, Database.SELECT_FOR_UPDATE_NOWAIT.get());
  }
}
