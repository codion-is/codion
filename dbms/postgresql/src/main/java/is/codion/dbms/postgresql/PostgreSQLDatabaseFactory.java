/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
  public boolean driverCompatible(String driverClassName) {
    return requireNonNull(driverClassName, "driverClassName").startsWith(DRIVER_PACKAGE);
  }

  @Override
  public Database createDatabase(String url) {
    return new PostgreSQLDatabase(url, Database.SELECT_FOR_UPDATE_NOWAIT.get());
  }
}
