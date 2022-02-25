/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.dbms.hsqldb;

import is.codion.common.db.database.Database;
import is.codion.common.db.database.DatabaseFactory;

import static java.util.Objects.requireNonNull;

/**
 * Provides hsql database implementations
 */
public final class HSQLDatabaseFactory implements DatabaseFactory {

  private static final String DRIVER_PACKAGE = "org.hsqldb";

  @Override
  public boolean isDriverCompatible(String driverClassName) {
    return requireNonNull(driverClassName, "driverClassName").startsWith(DRIVER_PACKAGE);
  }

  @Override
  public Database createDatabase(String jdbcUrl) {
    return new HSQLDatabase(jdbcUrl, Database.SELECT_FOR_UPDATE_NOWAIT.get());
  }
}
