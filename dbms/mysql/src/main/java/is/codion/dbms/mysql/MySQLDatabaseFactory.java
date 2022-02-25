/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.dbms.mysql;

import is.codion.common.db.database.Database;
import is.codion.common.db.database.DatabaseFactory;

import static java.util.Objects.requireNonNull;

/**
 * Provides mysql database implementations
 */
public final class MySQLDatabaseFactory implements DatabaseFactory {

  private static final String DRIVER_PACKAGE = "com.mysql";

  @Override
  public boolean isDriverCompatible(String driverClassName) {
    return requireNonNull(driverClassName, "driverClassName").startsWith(DRIVER_PACKAGE);
  }

  @Override
  public Database createDatabase(String jdbcUrl) {
    return new MySQLDatabase(jdbcUrl);
  }
}
