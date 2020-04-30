/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.mariadb;

import org.jminor.common.db.database.Database;
import org.jminor.common.db.database.DatabaseProvider;

import static java.util.Objects.requireNonNull;

/**
 * Provides mariadb database implementations
 */
public final class MariaDbDatabaseProvider implements DatabaseProvider {

  private static final String DRIVER_NAME = "org.mariadb.jdbc.Driver";

  @Override
  public boolean isDriverCompatible(final String driverClassName) {
    return requireNonNull(driverClassName, "driverClass").equals(DRIVER_NAME);
  }

  @Override
  public String getDatabaseClassName() {
    return MariaDbDatabase.class.getName();
  }

  @Override
  public Database createDatabase() {
    return new MariaDbDatabase(requireNonNull(Database.DATABASE_URL.get(), Database.DATABASE_URL.getProperty()));
  }
}
