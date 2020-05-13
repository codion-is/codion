/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.derby;

import org.jminor.common.db.database.Database;
import org.jminor.common.db.database.DatabaseProvider;

import static java.util.Objects.requireNonNull;

/**
 * Provides derby database implementations
 */
public final class DerbyDatabaseProvider implements DatabaseProvider {

  private static final String DRIVER_PACKAGE = "org.apache.derby.jdbc";

  @Override
  public boolean isDriverCompatible(final String driverClassName) {
    return requireNonNull(driverClassName, "driverClassName").startsWith(DRIVER_PACKAGE);
  }

  @Override
  public boolean isDatabaseCompatible(final Database database) {
    return database instanceof DerbyDatabase;
  }

  @Override
  public Database createDatabase(final String jdbcUrl) {
    return new DerbyDatabase(jdbcUrl);
  }
}
