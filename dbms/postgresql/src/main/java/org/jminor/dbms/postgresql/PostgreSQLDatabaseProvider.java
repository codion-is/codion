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

  public static final String DRIVER_PACKAGE = "org.postgresql";

  @Override
  public boolean isDriverCompatible(final String driverClassName) {
    return requireNonNull(driverClassName, "driverClassName").startsWith(DRIVER_PACKAGE);
  }

  @Override
  public String getDatabaseClassName() {
    return PostgreSQLDatabase.class.getName();
  }

  @Override
  public Database createDatabase() {
    return new PostgreSQLDatabase(requireNonNull(Database.DATABASE_URL.get(), Database.DATABASE_URL.getProperty()));
  }
}
