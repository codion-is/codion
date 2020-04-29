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

  @Override
  public boolean isCompatibleWith(final String driverClass) {
    return driverClass.equals("org.postgresql.Driver");
  }

  @Override
  public Database createDatabase() {
    final String jdbcUrl = requireNonNull(Database.DATABASE_URL.get(), Database.DATABASE_URL.getProperty());

    return new PostgreSQLDatabase(jdbcUrl);
  }
}
