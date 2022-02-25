/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.dbms.oracle;

import is.codion.common.db.database.Database;
import is.codion.common.db.database.DatabaseFactory;

import static java.util.Objects.requireNonNull;

/**
 * Provides oracle database implementations
 */
public final class OracleDatabaseFactory implements DatabaseFactory {

  private static final String DRIVER_PACKAGE = "oracle.jdbc";

  @Override
  public boolean isDriverCompatible(String driverClassName) {
    return requireNonNull(driverClassName, "driverClassName").startsWith(DRIVER_PACKAGE);
  }

  @Override
  public Database createDatabase(String jdbcUrl) {
    return new OracleDatabase(jdbcUrl, Database.SELECT_FOR_UPDATE_NOWAIT.get());
  }
}
