/*
 * Copyright (c) 2019 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
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
  private static final String JTDS_DRIVER_PACKAGE = "net.sourceforge.jtds.jdbc";

  @Override
  public boolean driverCompatible(String driverClassName) {
    requireNonNull(driverClassName, "driverClassName");
    return driverClassName.startsWith(DRIVER_PACKAGE) || driverClassName.startsWith(JTDS_DRIVER_PACKAGE);
  }

  @Override
  public Database createDatabase(String url) {
    return new SQLServerDatabase(url);
  }
}
