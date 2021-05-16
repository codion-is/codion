/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.dbms.sqlserver;

import is.codion.common.db.database.Database;
import is.codion.common.db.database.DatabaseFactory;

import static java.util.Objects.requireNonNull;

/**
 * Provides sql server database implementations
 */
public final class SQLServerDatabaseFactory implements DatabaseFactory {

  private static final String DRIVER_PACKAGE = "com.microsoft.sqlserver.jdbc";

  @Override
  public boolean isDriverCompatible(final String driverClassName) {
    return requireNonNull(driverClassName, "driverClassName").startsWith(DRIVER_PACKAGE);
  }

  @Override
  public Database createDatabase(final String jdbcUrl) {
    return new SQLServerDatabase(jdbcUrl);
  }
}
