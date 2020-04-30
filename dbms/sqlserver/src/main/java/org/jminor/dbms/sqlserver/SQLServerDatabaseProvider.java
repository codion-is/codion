/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.sqlserver;

import org.jminor.common.db.database.Database;
import org.jminor.common.db.database.DatabaseProvider;

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
  public String getDatabaseClassName() {
    return SQLServerDatabase.class.getName();
  }

  @Override
  public Database createDatabase() {
    return new SQLServerDatabase(requireNonNull(Database.DATABASE_URL.get(), Database.DATABASE_URL.getProperty()));
  }
}
