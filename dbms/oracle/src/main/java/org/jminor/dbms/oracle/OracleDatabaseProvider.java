/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.dbms.oracle;

import org.jminor.common.db.database.Database;
import org.jminor.common.db.database.DatabaseProvider;

import static java.util.Objects.requireNonNull;

/**
 * Provides oracle database implementations
 */
public final class OracleDatabaseProvider implements DatabaseProvider {

  private static final String DRIVER_NAME = "oracle.jdbc.driver.OracleDriver";

  @Override
  public boolean isDriverCompatible(final String driverClassName) {
    return requireNonNull(driverClassName, "driverClass").equals(DRIVER_NAME);
  }

  @Override
  public String getDatabaseClassName() {
    return OracleDatabase.class.getName();
  }

  @Override
  public Database createDatabase() {
    return new OracleDatabase(requireNonNull(Database.DATABASE_URL.get(), Database.DATABASE_URL.getProperty()));
  }
}
